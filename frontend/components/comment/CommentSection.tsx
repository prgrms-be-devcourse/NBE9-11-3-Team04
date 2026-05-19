"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { getAccessToken } from "@/lib/auth-storage"

type CommentSectionProps = {
  postId: number
  onCommentsChanged?: () => void | Promise<void>
}

type LoginRequiredPopupState = {
  open: boolean
  message: string
}

type SuccessResponse<T> = {
  code: string
  message: string
  data: T
}

type CommentAttachmentItem = {
  attachmentId?: number
  id?: number
  fileName: string
  fileUrl: string
  fileType?: string | null
  mimeType?: string | null
}

type CommentItem = {
  commentId: number
  postId: number
  postTitle?: string
  userId: number
  nickname: string | null
  parentCommentId: number | null
  content: string
  createdAt: string
  updatedAt: string
  deleted?: boolean
  isDeleted?: boolean
  attachments?: CommentAttachmentItem[]
  replies: CommentItem[]
}

type CommentListResponse = {
  comments: CommentItem[]
  page?: number
  size?: number
  totalElements?: number
  totalPages?: number
  hasNext?: boolean
}

const COMMENT_LIST_CACHE_TTL_MS = 1000
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

const recentCommentListCache = new Map<string, {
  timestamp: number
  response: CommentListResponse
}>()
const pendingCommentListRequests = new Map<string, Promise<CommentListResponse>>()

function normalizeAttachment(attachment: CommentAttachmentItem): CommentAttachmentItem {
  return {
    ...attachment,
    attachmentId: attachment.attachmentId ?? attachment.id,
  }
}

function normalizeComment(comment: CommentItem): CommentItem {
  return {
    ...comment,
    deleted: comment.deleted ?? comment.isDeleted ?? false,
    attachments: (comment.attachments ?? []).map(normalizeAttachment),
    replies: (comment.replies ?? []).map(normalizeComment),
  }
}

function extractCommentListResponse(responseBody: unknown): CommentListResponse {
  if (!responseBody || typeof responseBody !== "object") {
    return { comments: [] }
  }

  const body = responseBody as
      | CommentListResponse
      | SuccessResponse<CommentListResponse>
      | { data?: CommentListResponse }

  if (Array.isArray((body as CommentListResponse).comments)) {
    const directBody = body as CommentListResponse

    return {
      ...directBody,
      comments: (directBody.comments ?? []).map(normalizeComment),
    }
  }

  const nestedComments = (body as SuccessResponse<CommentListResponse>).data?.comments

  if (Array.isArray(nestedComments)) {
    const nestedBody = (body as SuccessResponse<CommentListResponse>).data

    return {
      ...nestedBody,
      comments: nestedComments.map(normalizeComment),
    }
  }

  return { comments: [] }
}

type CreatedCommentApiResponse = {
  id?: number
  commentId?: number
  data?: {
    id?: number
    commentId?: number
    comment?: {
      id?: number
      commentId?: number
    }
  }
}

/**
 * 댓글/대댓글 생성 응답에서 실제 commentId 를 꺼낸다.
 *
 * 다양한 백엔드 응답 형식에 대응한다.
 */
function extractCreatedCommentId(responseBody: unknown): number | null {
  if (!responseBody || typeof responseBody !== "object") {
    return null
  }

  const candidate = responseBody as CreatedCommentApiResponse

  const possibleIds = [
    candidate.commentId,
    candidate.id,
    candidate.data?.commentId,
    candidate.data?.id,
    candidate.data?.comment?.commentId,
    candidate.data?.comment?.id,
  ]

  const validId = possibleIds.find((value) => typeof value === "number")

  return typeof validId === "number" ? validId : null
}

function isUnauthorizedStatus(status: number): boolean {
  return status === 401 || status === 403
}

function isLoginRequiredMessage(message: string | null | undefined): boolean {
  if (!message) {
    return false
  }

  return message.includes("인증") || message.includes("로그인")
}

function isSelfReportMessage(message: string | null | undefined): boolean {
  if (!message) {
    return false
  }

  return message.includes("자신") || message.includes("본인") || message.includes("신고할 수 없습니다")
}

function isSelfReportPopupMessage(message: string | null | undefined): boolean {
  if (!message) {
    return false
  }

  return message.includes("자신이 작성한") && message.includes("신고할 수 없습니다")
}

async function extractErrorMessage(response: Response, fallbackMessage: string): Promise<string> {
  try {
    const errorData = await response.json()
    return errorData?.message ?? errorData?.resultMessage ?? errorData?.msg ?? fallbackMessage
  } catch {
    return fallbackMessage
  }
}

type ReplyFilesMap = Record<number, File[]>

/**
 * 현재 auth-storage 에는 공통 fetch 헬퍼가 없어서,
 * 댓글 영역에서는 임시로 JWT/OAuth 공통 인증 옵션을 로컬에서 구성한다.
 *
 * - 일반 로그인 사용자는 Bearer 토큰 사용
 * - OAuth 로그인 사용자는 credentials: include 기반 세션 인증 사용
 */
const OAUTH_SESSION_PLACEHOLDER_TOKEN = "oauth-cookie-session"

function hasLocalJwtToken(token: string | null | undefined): boolean {
  return Boolean(token) && token !== OAUTH_SESSION_PLACEHOLDER_TOKEN
}

function getAuthFetchOptions(): Pick<RequestInit, "credentials" | "headers"> {
  const token = getAccessToken()

  return {
    credentials: "include",
    headers: hasLocalJwtToken(token)
        ? {
          Authorization: `Bearer ${token}`,
        }
        : undefined,
  }
}

function getCurrentUserIdFromToken(): number | null {
  if (typeof window === "undefined") {
    return null
  }

  const token = getAccessToken()
  if (!hasLocalJwtToken(token)) {
    return null
  }

  try {
    const payload = token!.split(".")[1]
    const decoded = JSON.parse(atob(payload)) as {
      userId?: number | string
      user_id?: number | string
      id?: number | string
      sub?: number | string
    }

    const rawUserId = decoded.userId ?? decoded.user_id ?? decoded.id ?? decoded.sub

    if (typeof rawUserId === "number") {
      return rawUserId
    }

    if (typeof rawUserId === "string") {
      const parsedUserId = Number(rawUserId)
      return Number.isNaN(parsedUserId) ? null : parsedUserId
    }

    return null
  } catch {
    return null
  }
}

/**
 * OAuth 로그인 사용자는 localStorage 기반 JWT가 없을 수 있어서,
 * 쿠키 인증으로 현재 사용자 정보를 다시 조회해 currentUserId 를 동기화한다.
 */
async function fetchCurrentUserIdFromServer(): Promise<number | null> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/users/me`, {
      ...getAuthFetchOptions(),
      method: "GET",
    })

    if (!response.ok) {
      return null
    }

    const data = (await response.json()) as {
      userId?: number | string
      id?: number | string
      data?: {
        userId?: number | string
        id?: number | string
      }
    }

    const rawUserId = data.userId ?? data.id ?? data.data?.userId ?? data.data?.id

    if (typeof rawUserId === "number") {
      return rawUserId
    }

    if (typeof rawUserId === "string") {
      const parsedUserId = Number(rawUserId)
      return Number.isNaN(parsedUserId) ? null : parsedUserId
    }

    return null
  } catch {
    return null
  }
}

function getJsonAuthHeaders(): Headers {
  const headers = new Headers()
  headers.set("Content-Type", "application/json")

  const authHeaders = getAuthFetchOptions().headers
  if (authHeaders) {
    const normalized = new Headers(authHeaders)
    normalized.forEach((value, key) => {
      headers.set(key, value)
    })
  }

  return headers
}

function isImageAttachment(attachment: CommentAttachmentItem): boolean {
  const normalizedFileType = attachment.fileType?.toUpperCase()
  const normalizedMimeType = attachment.mimeType?.toLowerCase()

  return normalizedFileType === "IMAGE" || normalizedMimeType?.startsWith("image/") === true
}

function renderAttachments(
    attachments: CommentAttachmentItem[] | undefined,
    options?: {
      canDelete?: boolean
      deletingAttachmentId?: number | null
      onDeleteAttachment?: (attachmentId: number) => void
    },
) {
  if (!attachments || attachments.length === 0) {
    return null
  }

  return (
      <div className="mt-3 space-y-3">
        <p className="text-xs font-medium text-muted-foreground">첨부파일</p>
        <div className="flex flex-col gap-3">
          {attachments.map((attachment) => {
            const resolvedAttachmentId = attachment.attachmentId ?? attachment.id
            const fileUrl = `${API_BASE_URL}${attachment.fileUrl}`
            const isImage = isImageAttachment(attachment)
            const isDeleting = resolvedAttachmentId !== undefined && options?.deletingAttachmentId === resolvedAttachmentId

            return (
                <div
                    key={resolvedAttachmentId ?? attachment.fileName}
                    className="flex flex-col gap-2 rounded-md border border-border/70 p-3"
                >
                  {isImage && (
                      <img
                          src={fileUrl}
                          alt={attachment.fileName}
                          className="max-h-80 w-fit max-w-full rounded-md border border-border object-contain"
                      />
                  )}
                  <div className="flex items-center justify-between gap-3">
                    <a
                        href={fileUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="w-fit text-sm text-primary underline-offset-2 hover:underline"
                    >
                      {attachment.fileName}
                    </a>
                    {options?.canDelete && resolvedAttachmentId !== undefined && options.onDeleteAttachment && (
                        <button
                            type="button"
                            onClick={() => options.onDeleteAttachment?.(resolvedAttachmentId)}
                            disabled={isDeleting}
                            className="text-xs font-medium text-destructive hover:underline disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          {isDeleting ? "삭제 중..." : "첨부 삭제"}
                        </button>
                    )}
                  </div>
                </div>
            )
          })}
        </div>
      </div>
  )
}

function sortCommentsByNewest(comments: CommentItem[]): CommentItem[] {
  return [...comments]
      .map((comment) => ({
        ...comment,
        replies: sortCommentsByNewest(comment.replies ?? []),
      }))
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
}

function resolveCommentTotalPages(response: CommentListResponse, currentPage: number): number {
  const serverTotalPages = response.totalPages ?? 0
  const size = response.size && response.size > 0 ? response.size : 20
  const totalElementsPages =
      response.totalElements !== undefined ? Math.ceil(response.totalElements / size) : 0
  const hasNextPages = response.hasNext ? currentPage + 2 : currentPage + 1

  return Math.max(serverTotalPages, totalElementsPages, hasNextPages)
}

export default function CommentSection({ postId, onCommentsChanged }: CommentSectionProps) {
  const [comments, setComments] = useState<CommentItem[]>([])
  const [commentPage, setCommentPage] = useState(0)
  const [commentTotalPages, setCommentTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [newComment, setNewComment] = useState("")
  const [submitting, setSubmitting] = useState(false)
  const [replyInputs, setReplyInputs] = useState<Record<number, string>>({})
  const [replySubmittingId, setReplySubmittingId] = useState<number | null>(null)
  const [openedReplyId, setOpenedReplyId] = useState<number | null>(null)
  const [deletingCommentId, setDeletingCommentId] = useState<number | null>(null)
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null)
  const [editInputs, setEditInputs] = useState<Record<number, string>>({})
  const [editingSubmittingId, setEditingSubmittingId] = useState<number | null>(null)
  const [reportSubmittingId, setReportSubmittingId] = useState<number | null>(null)
  const [currentUserId, setCurrentUserId] = useState<number | null>(null)
  const [newCommentFiles, setNewCommentFiles] = useState<File[]>([])
  const [replyFiles, setReplyFiles] = useState<ReplyFilesMap>({})
  const [deletingAttachmentId, setDeletingAttachmentId] = useState<number | null>(null)

  const handleRemoveNewCommentFile = (indexToRemove: number) => {
    setNewCommentFiles((prev) => prev.filter((_, index) => index !== indexToRemove))
  }

  const handleRemoveReplyFile = (commentId: number, indexToRemove: number) => {
    setReplyFiles((prev) => ({
      ...prev,
      [commentId]: (prev[commentId] ?? []).filter((_, index) => index !== indexToRemove),
    }))
  }
  const handleDeleteAttachment = async (commentId: number, attachmentId: number) => {
    try {
      setDeletingAttachmentId(attachmentId)
      setError(null)

      const response = await fetch(`${API_BASE_URL}/api/comments/${commentId}/attachments/${attachmentId}`, {
        ...getAuthFetchOptions(),
        method: "DELETE",
        headers: getJsonAuthHeaders(),
      })

      if (!response.ok) {
        if (isUnauthorizedStatus(response.status)) {
          openLoginRequiredPopup("로그인이 필요한 기능입니다.")
          return
        }

        const message = await extractErrorMessage(response, "첨부파일 삭제에 실패했습니다.")
        throw new Error(message)
      }

      await loadComments({ force: true, page: commentPage })
    } catch (err) {
      setError(err instanceof Error ? err.message : "알 수 없는 오류가 발생했습니다.")
    } finally {
      setDeletingAttachmentId(null)
    }
  }

  const [loginRequiredPopup, setLoginRequiredPopup] = useState<LoginRequiredPopupState>({
    open: false,
    message: "로그인이 필요한 기능입니다.",
  })

  const loginPath = useMemo(() => "/login", [])

  const openLoginRequiredPopup = useCallback((message = "로그인이 필요한 기능입니다.") => {
    setLoginRequiredPopup({
      open: true,
      message,
    })
  }, [])

  const closeLoginRequiredPopup = useCallback(() => {
    setLoginRequiredPopup((prev) => ({
      ...prev,
      open: false,
    }))
  }, [])

  const moveToLoginPage = useCallback(() => {
    if (typeof window !== "undefined") {
      window.location.href = loginPath
    }
  }, [loginPath])

  const loadComments = useCallback(async (options?: { force?: boolean; page?: number }) => {
    const page = options?.page ?? 0
    const cacheKey = `${postId}:${page}`

    try {
      setLoading(true)
      setError(null)

      if (!options?.force) {
        const cached = recentCommentListCache.get(cacheKey)
        const now = Date.now()

        if (cached && now - cached.timestamp < COMMENT_LIST_CACHE_TTL_MS) {
          setComments(sortCommentsByNewest(cached.response.comments ?? []))
          setCommentPage(cached.response.page ?? page)
          setCommentTotalPages(resolveCommentTotalPages(cached.response, cached.response.page ?? page))
          return
        }

        const pendingRequest = pendingCommentListRequests.get(cacheKey)

        if (pendingRequest) {
          const pendingResponse = await pendingRequest
          setComments(sortCommentsByNewest(pendingResponse.comments ?? []))
          setCommentPage(pendingResponse.page ?? page)
          setCommentTotalPages(resolveCommentTotalPages(pendingResponse, pendingResponse.page ?? page))
          return
        }
      }

      const requestPromise = (async () => {
        const response = await fetch(
            `${API_BASE_URL}/api/posts/${postId}/comments?page=${page}&size=20`
        )

        if (!response.ok) {
          throw new Error("댓글을 불러오지 못했습니다.")
        }

        const responseBody = await response.json()
        const data = extractCommentListResponse(responseBody)

        const normalizedResponse: CommentListResponse = {
          ...data,
          comments: data.comments ?? [],
        }

        recentCommentListCache.set(cacheKey, {
          timestamp: Date.now(),
          response: normalizedResponse,
        })

        return normalizedResponse
      })()

      if (!options?.force) {
        pendingCommentListRequests.set(cacheKey, requestPromise)
      }

      const loadedResponse = await requestPromise

      setComments(sortCommentsByNewest(loadedResponse.comments ?? []))
      setCommentPage(loadedResponse.page ?? page)
      setCommentTotalPages(resolveCommentTotalPages(loadedResponse, loadedResponse.page ?? page))
    } catch (err) {
      setError(err instanceof Error ? err.message : "알 수 없는 오류가 발생했습니다.")
    } finally {
      pendingCommentListRequests.delete(cacheKey)
      setLoading(false)
    }
  }, [postId])
  const commentPageNumbers = Array.from({ length: commentTotalPages }, (_, index) => index)

  const changeCommentPage = (page: number) => {
    if (page === commentPage) {
      return
    }

    void loadComments({ force: true, page })
  }

  useEffect(() => {
    void loadComments({ page: 0 })
  }, [loadComments])

  useEffect(() => {
    let cancelled = false

    const syncCurrentUserId = () => {
      void (async () => {
        const tokenUserId = getCurrentUserIdFromToken()

        if (tokenUserId !== null) {
          if (!cancelled) {
            setCurrentUserId(tokenUserId)
          }
          return
        }

        const serverUserId = await fetchCurrentUserIdFromServer()
        if (!cancelled) {
          setCurrentUserId(serverUserId)
        }
      })()
    }

    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        syncCurrentUserId()
      }
    }

    syncCurrentUserId()

    window.addEventListener("storage", syncCurrentUserId)
    window.addEventListener("focus", syncCurrentUserId)
    document.addEventListener("visibilitychange", handleVisibilityChange)
    window.addEventListener("auth-changed", syncCurrentUserId as EventListener)

    return () => {
      cancelled = true
      window.removeEventListener("storage", syncCurrentUserId)
      window.removeEventListener("focus", syncCurrentUserId)
      document.removeEventListener("visibilitychange", handleVisibilityChange)
      window.removeEventListener("auth-changed", syncCurrentUserId as EventListener)
    }
  }, [])

  /**
   * 댓글/대댓글 작성 직후 첨부파일을 업로드한다.
   *
   * - 일반 로그인 사용자는 Authorization 헤더 기반으로 인증
   * - OAuth 로그인 사용자는 credentials: include 기반 세션 인증
   *
   * FormData 요청이라 Content-Type 은 직접 지정하지 않고 브라우저에 맡긴다.
   */
  const uploadCommentAttachments = async (commentId: number, files: File[]) => {
    if (files.length === 0) {
      return
    }

    const formData = new FormData()

    files.forEach((file, index) => {
      formData.append("files", file)
      formData.append("fileOrder", String(index + 1))
    })

    const authOptions = getAuthFetchOptions()

    const response = await fetch(`${API_BASE_URL}/api/comments/${commentId}/attachments`, {
      ...authOptions,
      method: "POST",
      body: formData,
    })

    if (!response.ok) {
      throw new Error("댓글 첨부파일 업로드에 실패했습니다.")
    }
  }

  const handleCreateComment = async () => {
    const trimmedComment = newComment.trim()

    if (!trimmedComment) {
      return
    }

    try {
      setSubmitting(true)
      setError(null)

      const response = await fetch(`${API_BASE_URL}/api/posts/${postId}/comments`, {
        ...getAuthFetchOptions(),
        method: "POST",
        headers: getJsonAuthHeaders(),
        body: JSON.stringify({
          content: trimmedComment,
        }),
      })

      if (!response.ok) {
        if (isUnauthorizedStatus(response.status)) {
          openLoginRequiredPopup("로그인이 필요한 기능입니다.")
          return
        }

        const message = await extractErrorMessage(response, "댓글 작성에 실패했습니다.")
        throw new Error(message)
      }

      const createdComment = await response.json()
      const createdCommentId = extractCreatedCommentId(createdComment)

      if (newCommentFiles.length > 0) {
        if (createdCommentId === null) {
          throw new Error("댓글은 생성됐지만 생성된 commentId를 응답에서 찾지 못해 첨부 업로드를 진행하지 못했습니다.")
        }

        await uploadCommentAttachments(createdCommentId, newCommentFiles)
      }

      setNewComment("")
      setNewCommentFiles([])
      await loadComments({ force: true, page: 0 })
      await onCommentsChanged?.()
    } catch (err) {
      setError(err instanceof Error ? err.message : "알 수 없는 오류가 발생했습니다.")
    } finally {
      setSubmitting(false)
    }
  }

  const handleCreateReply = async (commentId: number) => {
    const content = (replyInputs[commentId] ?? "").trim()

    if (!content) {
      return
    }

    try {
      setReplySubmittingId(commentId)
      setError(null)

      const response = await fetch(`${API_BASE_URL}/api/comments/${commentId}/replies`, {
        ...getAuthFetchOptions(),
        method: "POST",
        headers: getJsonAuthHeaders(),
        body: JSON.stringify({
          content,
        }),
      })

      if (!response.ok) {
        if (isUnauthorizedStatus(response.status)) {
          openLoginRequiredPopup("로그인이 필요한 기능입니다.")
          return
        }

        const message = await extractErrorMessage(response, "대댓글 작성에 실패했습니다.")
        throw new Error(message)
      }

      const createdReply = await response.json()
      const createdReplyId = extractCreatedCommentId(createdReply)

      if ((replyFiles[commentId]?.length ?? 0) > 0) {
        if (createdReplyId === null) {
          throw new Error("답글은 생성됐지만 생성된 commentId를 응답에서 찾지 못해 첨부 업로드를 진행하지 못했습니다.")
        }

        await uploadCommentAttachments(createdReplyId, replyFiles[commentId] ?? [])
      }

      setReplyInputs((prev) => ({
        ...prev,
        [commentId]: "",
      }))
      setReplyFiles((prev) => ({
        ...prev,
        [commentId]: [],
      }))
      setOpenedReplyId(null)
      await loadComments({ force: true, page: commentPage })
      await onCommentsChanged?.()
    } catch (err) {
      setError(err instanceof Error ? err.message : "알 수 없는 오류가 발생했습니다.")
    } finally {
      setReplySubmittingId(null)
    }
  }

  const handleReportComment = async (commentId: number) => {
    try {
      setReportSubmittingId(commentId)
      setError(null)

      const response = await fetch(`${API_BASE_URL}/api/report/comment`, {
        ...getAuthFetchOptions(),
        method: "POST",
        headers: getJsonAuthHeaders(),
        body: JSON.stringify({
          targetId: commentId,
          reasonType: "ETC",
          reasonDetail: "댓글 영역에서 접수한 신고입니다.",
        }),
      })

      if (!response.ok) {
        let message = await extractErrorMessage(response, "댓글 신고에 실패했습니다.")

        if (isUnauthorizedStatus(response.status) || isLoginRequiredMessage(message)) {
          setError(null)
          openLoginRequiredPopup("로그인이 필요한 기능입니다.")
          return
        }

        if (isSelfReportMessage(message)) {
          setError(null)
          openLoginRequiredPopup("자신이 작성한 댓글은 신고할 수 없습니다.")
          return
        }

        if (typeof message === "string" && message.includes("이미 신고")) {
          message = "이미 신고한 댓글입니다."
        }

        throw new Error(message)
      }

      alert("댓글 신고가 접수되었습니다.")
      window.dispatchEvent(new CustomEvent("notifications-updated"))
    } catch (err) {
      const message = err instanceof Error ? err.message : "알 수 없는 오류가 발생했습니다."

      if (isLoginRequiredMessage(message)) {
        setError(null)
        openLoginRequiredPopup("로그인이 필요한 기능입니다.")
        return
      }

      if (isSelfReportMessage(message)) {
        setError(null)
        openLoginRequiredPopup("자신이 작성한 댓글은 신고할 수 없습니다.")
        return
      }

      setError(message)
    } finally {
      setReportSubmittingId(null)
    }
  }

  const handleDeleteComment = async (commentId: number) => {
    try {
      setDeletingCommentId(commentId)
      setError(null)

      const response = await fetch(`${API_BASE_URL}/api/comments/${commentId}`, {
        ...getAuthFetchOptions(),
        method: "DELETE",
        headers: getJsonAuthHeaders(),
      })

      if (!response.ok) {
        if (isUnauthorizedStatus(response.status)) {
          openLoginRequiredPopup("로그인이 필요한 기능입니다.")
          return
        }

        const message = await extractErrorMessage(response, "댓글 삭제에 실패했습니다.")
        throw new Error(message)
      }

      if (openedReplyId === commentId) {
        setOpenedReplyId(null)
      }
      if (editingCommentId === commentId) {
        setEditingCommentId(null)
      }

      await loadComments({ force: true, page: commentPage })
      await onCommentsChanged?.()
    } catch (err) {
      setError(err instanceof Error ? err.message : "알 수 없는 오류가 발생했습니다.")
    } finally {
      setDeletingCommentId(null)
    }
  }

  const handleStartEdit = (commentId: number, currentContent: string) => {
    setEditingCommentId(commentId)
    setEditInputs((prev) => ({
      ...prev,
      [commentId]: currentContent,
    }))
  }

  const handleUpdateComment = async (commentId: number) => {
    const content = (editInputs[commentId] ?? "").trim()

    if (!content) {
      return
    }

    try {
      setEditingSubmittingId(commentId)
      setError(null)

      const response = await fetch(`${API_BASE_URL}/api/comments/${commentId}`, {
        ...getAuthFetchOptions(),
        method: "PATCH",
        headers: getJsonAuthHeaders(),
        body: JSON.stringify({
          content,
        }),
      })

      if (!response.ok) {
        if (isUnauthorizedStatus(response.status)) {
          openLoginRequiredPopup("로그인이 필요한 기능입니다.")
          return
        }

        const message = await extractErrorMessage(response, "댓글 수정에 실패했습니다.")
        throw new Error(message)
      }

      setEditingCommentId(null)
      await loadComments({ force: true, page: commentPage })
    } catch (err) {
      setError(err instanceof Error ? err.message : "알 수 없는 오류가 발생했습니다.")
    } finally {
      setEditingSubmittingId(null)
    }
  }

  return (
      <section className="rounded-xl border border-border bg-card p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-foreground">댓글</h2>

        <div className="mt-4 space-y-3 rounded-lg border border-border p-4">
        <textarea
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
            placeholder="댓글을 입력하세요."
            className="min-h-[96px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground outline-none ring-offset-background placeholder:text-muted-foreground focus-visible:ring-2 focus-visible:ring-ring"
        />

          <label className="mt-3 inline-flex w-fit cursor-pointer items-center rounded-md border border-border px-3 py-2 text-sm font-medium text-foreground hover:bg-muted/50">
            파일 첨부
            <input
                type="file"
                multiple
                onChange={(event) => {
                  const selectedFiles = Array.from(event.target.files ?? [])
                  setNewCommentFiles(selectedFiles)
                  event.target.value = ""
                }}
                className="hidden"
            />
          </label>

          <div className="flex items-center justify-between gap-3">
            {newCommentFiles.length > 0 ? (
                <div className="space-y-2">
                  <p className="text-xs text-muted-foreground">
                    첨부파일 {newCommentFiles.length}개 선택됨
                  </p>
                  <div className="space-y-2">
                    {newCommentFiles.map((file, index) => (
                        <div
                            key={`${file.name}-${file.size}-${index}`}
                            className="flex items-center justify-between gap-3 rounded-md border border-border/70 px-3 py-2"
                        >
                          <p className="truncate text-xs text-foreground">{file.name}</p>
                          <button
                              type="button"
                              onClick={() => handleRemoveNewCommentFile(index)}
                              className="shrink-0 text-xs font-medium text-destructive hover:underline"
                          >
                            삭제
                          </button>
                        </div>
                    ))}
                  </div>
                </div>
            ) : (
                <p className="text-xs text-muted-foreground">선택된 첨부파일 없음</p>
            )}
            <button
                type="button"
                onClick={handleCreateComment}
                disabled={submitting || !newComment.trim()}
                className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:cursor-not-allowed disabled:opacity-50"
            >
              {submitting ? "작성 중..." : "댓글 작성"}
            </button>
          </div>
        </div>

        {loading && (
            <p className="mt-4 text-sm text-muted-foreground">댓글을 불러오는 중입니다...</p>
        )}

        {error && <p className="mt-4 text-sm text-destructive">{error}</p>}

        {loginRequiredPopup.open && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
              <div className="w-full max-w-sm rounded-xl border border-border bg-card p-6 shadow-lg">
                <h3 className="text-lg font-semibold text-foreground">안내</h3>
                <p className="mt-3 text-sm text-muted-foreground">{loginRequiredPopup.message}</p>
                <div className="mt-6 flex justify-end gap-2">
                  {!isSelfReportPopupMessage(loginRequiredPopup.message) && (
                      <button
                          type="button"
                          onClick={closeLoginRequiredPopup}
                          className="rounded-md border border-border px-4 py-2 text-sm font-medium text-foreground"
                      >
                        취소
                      </button>
                  )}
                  <button
                      type="button"
                      onClick={loginRequiredPopup.message.includes("로그인") ? moveToLoginPage : closeLoginRequiredPopup}
                      className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
                  >
                    {loginRequiredPopup.message.includes("로그인") ? "로그인 하러가기" : "확인"}
                  </button>
                </div>
              </div>
            </div>
        )}

        {!loading && !error && comments.length === 0 && (
            <p className="mt-4 text-sm text-muted-foreground">아직 댓글이 없습니다.</p>
        )}

        <div className="mt-4 space-y-4">
          {comments.map((comment) => (
              <div key={comment.commentId} className="rounded-lg border border-border p-4">
                {editingCommentId === comment.commentId ? (
                    <div className="space-y-2">
                                <textarea
                                    value={editInputs[comment.commentId] ?? ""}
                                    onChange={(e) =>
                                        setEditInputs((prev) => ({
                                          ...prev,
                                          [comment.commentId]: e.target.value,
                                        }))
                                    }
                                    className="min-h-[96px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground outline-none ring-offset-background placeholder:text-muted-foreground focus-visible:ring-2 focus-visible:ring-ring"
                                />
                      <div className="flex justify-end gap-2">
                        <button
                            type="button"
                            onClick={() => setEditingCommentId(null)}
                            className="rounded-md border border-border px-3 py-2 text-sm font-medium text-foreground"
                        >
                          취소
                        </button>
                        <button
                            type="button"
                            onClick={() => handleUpdateComment(comment.commentId)}
                            disabled={
                                editingSubmittingId === comment.commentId ||
                                !(editInputs[comment.commentId] ?? "").trim()
                            }
                            className="rounded-md bg-primary px-3 py-2 text-sm font-medium text-primary-foreground disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          {editingSubmittingId === comment.commentId ? "수정 중..." : "수정 완료"}
                        </button>
                      </div>
                    </div>
                ) : (
                    <>
                      <p className="text-sm text-foreground">
                        {comment.deleted ? "삭제된 댓글입니다." : comment.content}
                      </p>
                      {renderAttachments(comment.attachments, {
                        canDelete: comment.userId === currentUserId,
                        deletingAttachmentId,
                        onDeleteAttachment: (attachmentId) => handleDeleteAttachment(comment.commentId, attachmentId),
                      })}
                    </>
                )}

                <div className="mt-2 flex items-center justify-between gap-3">
                  <p className="text-xs text-muted-foreground">
                    작성자: {comment.nickname ?? `user-${comment.userId}`}
                  </p>
                  <div className="flex items-center gap-3">
                    {comment.userId === currentUserId && (
                        <>
                          <button
                              type="button"
                              onClick={() => handleStartEdit(comment.commentId, comment.content)}
                              className="text-xs font-medium text-primary hover:underline"
                          >
                            수정
                          </button>
                          <button
                              type="button"
                              onClick={() => handleDeleteComment(comment.commentId)}
                              disabled={deletingCommentId === comment.commentId}
                              className="text-xs font-medium text-destructive hover:underline disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            {deletingCommentId === comment.commentId ? "삭제 중..." : "삭제"}
                          </button>
                        </>
                    )}
                    <button
                        type="button"
                        onClick={() => handleReportComment(comment.commentId)}
                        disabled={reportSubmittingId === comment.commentId}
                        className="text-xs font-medium text-destructive hover:underline disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {reportSubmittingId === comment.commentId ? "신고 중..." : "신고"}
                    </button>
                  </div>
                </div>

                <div className="mt-3 flex justify-end">
                  <button
                      type="button"
                      onClick={() =>
                          setOpenedReplyId((prev) => (prev === comment.commentId ? null : comment.commentId))
                      }
                      className="text-sm font-medium text-primary hover:underline"
                  >
                    {openedReplyId === comment.commentId ? "답글 입력 닫기" : "답글 달기"}
                  </button>
                </div>

                {openedReplyId === comment.commentId && (
                    <div className="mt-4 ml-4 border-l-2 border-border/70 pl-4">
                      <p className="mb-2 text-xs font-medium text-muted-foreground">이 댓글에 답글 달기</p>
                      <div className="space-y-2 rounded-md bg-muted/30 p-3">
                  <textarea
                      value={replyInputs[comment.commentId] ?? ""}
                      onChange={(e) =>
                          setReplyInputs((prev) => ({
                            ...prev,
                            [comment.commentId]: e.target.value,
                          }))
                      }
                      placeholder="답글을 입력하세요."
                      className="min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground outline-none ring-offset-background placeholder:text-muted-foreground focus-visible:ring-2 focus-visible:ring-ring"
                  />

                        <label className="mt-3 inline-flex w-fit cursor-pointer items-center rounded-md border border-border px-3 py-2 text-sm font-medium text-foreground hover:bg-muted/50">
                          파일 첨부
                          <input
                              type="file"
                              multiple
                              onChange={(event) => {
                                const selectedFiles = Array.from(event.target.files ?? [])
                                setReplyFiles((prev) => ({
                                  ...prev,
                                  [comment.commentId]: selectedFiles,
                                }))
                                event.target.value = ""
                              }}
                              className="hidden"
                          />
                        </label>

                        {(replyFiles[comment.commentId]?.length ?? 0) > 0 ? (
                            <div className="mt-2 space-y-2">
                              <p className="text-xs text-muted-foreground">
                                첨부파일 {replyFiles[comment.commentId]?.length ?? 0}개 선택됨
                              </p>
                              <div className="space-y-2">
                                {(replyFiles[comment.commentId] ?? []).map((file, index) => (
                                    <div
                                        key={`${file.name}-${file.size}-${index}`}
                                        className="flex items-center justify-between gap-3 rounded-md border border-border/70 px-3 py-2"
                                    >
                                      <p className="truncate text-xs text-foreground">{file.name}</p>
                                      <button
                                          type="button"
                                          onClick={() => handleRemoveReplyFile(comment.commentId, index)}
                                          className="shrink-0 text-xs font-medium text-destructive hover:underline"
                                      >
                                        삭제
                                      </button>
                                    </div>
                                ))}
                              </div>
                            </div>
                        ) : (
                            <p className="mt-2 text-xs text-muted-foreground">선택된 첨부파일 없음</p>
                        )}

                        <div className="flex justify-end">
                          <button
                              type="button"
                              onClick={() => handleCreateReply(comment.commentId)}
                              disabled={
                                  replySubmittingId === comment.commentId ||
                                  !(replyInputs[comment.commentId] ?? "").trim()
                              }
                              className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            {replySubmittingId === comment.commentId ? "답글 작성 중..." : "답글 작성"}
                          </button>
                        </div>
                      </div>
                    </div>
                )}

                {comment.replies?.length > 0 && (
                    <div className="mt-4 ml-4 space-y-2 border-l-2 border-border/70 pl-4">
                      {comment.replies.map((reply) => (
                          <div key={reply.commentId} className="rounded-md bg-muted/50 p-3 shadow-sm">
                            {editingCommentId === reply.commentId ? (
                                <div className="space-y-2">
                                                <textarea
                                                    value={editInputs[reply.commentId] ?? ""}
                                                    onChange={(e) =>
                                                        setEditInputs((prev) => ({
                                                          ...prev,
                                                          [reply.commentId]: e.target.value,
                                                        }))
                                                    }
                                                    className="min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground outline-none ring-offset-background placeholder:text-muted-foreground focus-visible:ring-2 focus-visible:ring-ring"
                                                />
                                  <div className="flex justify-end gap-2">
                                    <button
                                        type="button"
                                        onClick={() => setEditingCommentId(null)}
                                        className="rounded-md border border-border px-3 py-2 text-sm font-medium text-foreground"
                                    >
                                      취소
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => handleUpdateComment(reply.commentId)}
                                        disabled={
                                            editingSubmittingId === reply.commentId ||
                                            !(editInputs[reply.commentId] ?? "").trim()
                                        }
                                        className="rounded-md bg-primary px-3 py-2 text-sm font-medium text-primary-foreground disabled:cursor-not-allowed disabled:opacity-50"
                                    >
                                      {editingSubmittingId === reply.commentId ? "수정 중..." : "수정 완료"}
                                    </button>
                                  </div>
                                </div>
                            ) : (
                                <>
                                  <p className="text-sm text-foreground">
                                    {reply.deleted ? "삭제된 댓글입니다." : reply.content}
                                  </p>
                                  {renderAttachments(reply.attachments, {
                                    canDelete: reply.userId === currentUserId,
                                    deletingAttachmentId,
                                    onDeleteAttachment: (attachmentId) => handleDeleteAttachment(reply.commentId, attachmentId),
                                  })}
                                </>
                            )}

                            <div className="mt-2 flex items-center justify-between gap-3">
                              <p className="text-xs text-muted-foreground">
                                작성자: {reply.nickname ?? `user-${reply.userId}`}
                              </p>
                              <div className="flex items-center gap-3">
                                {reply.userId === currentUserId && (
                                    <>
                                      <button
                                          type="button"
                                          onClick={() => handleStartEdit(reply.commentId, reply.content)}
                                          className="text-xs font-medium text-primary hover:underline"
                                      >
                                        수정
                                      </button>
                                      <button
                                          type="button"
                                          onClick={() => handleDeleteComment(reply.commentId)}
                                          disabled={deletingCommentId === reply.commentId}
                                          className="text-xs font-medium text-destructive hover:underline disabled:cursor-not-allowed disabled:opacity-50"
                                      >
                                        {deletingCommentId === reply.commentId ? "삭제 중..." : "삭제"}
                                      </button>
                                    </>
                                )}
                                <button
                                    type="button"
                                    onClick={() => handleReportComment(reply.commentId)}
                                    disabled={reportSubmittingId === reply.commentId}
                                    className="text-xs font-medium text-destructive hover:underline disabled:cursor-not-allowed disabled:opacity-50"
                                >
                                  {reportSubmittingId === reply.commentId ? "신고 중..." : "신고"}
                                </button>
                              </div>
                            </div>
                          </div>
                      ))}
                    </div>
                )}
              </div>
          ))}
        </div>

        {commentTotalPages > 1 && (
            <div className="mt-6 flex flex-wrap justify-center gap-2">
              {commentPageNumbers.map((page) => (
                  <button
                      key={page}
                      type="button"
                      onClick={() => changeCommentPage(page)}
                      className={`rounded-md border px-3 py-1.5 text-sm font-medium ${
                          page === commentPage
                              ? "border-primary bg-primary text-primary-foreground"
                              : "border-border text-foreground hover:bg-muted/50"
                      }`}
                  >
                    {page + 1}
                  </button>
              ))}
            </div>
        )}
      </section>
  )
}
