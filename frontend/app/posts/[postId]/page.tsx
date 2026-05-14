"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import Link from "next/link"
import { useParams } from "next/navigation"
import CommentSection from "@/components/comment/CommentSection"
import InteractionButtons from "@/components/interaction-buttons"
import { getAccessToken } from "@/lib/auth-storage"

type PostDetailResponse = {
  code: string
  message: string
  timestamp: string
  data: {
    postId: number
    title: string
    content: string
    userId: number
    writerName: string
    categoryId: number
    viewCount: number
    likeCount: number
    commentCount: number
    bookmarkCount?: number
    bookmarked?: boolean
    createdAt: string
    updatedAt: string
    liked?: boolean
  }
}

type LoginRequiredPopupState = {
  open: boolean
  message: string
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

const ALLOWED_TAGS = new Set([
  "p",
  "br",
  "strong",
  "b",
  "em",
  "i",
  "a",
  "blockquote",
  "ul",
  "ol",
  "li",
  "pre",
  "code",
  "img",
])

function sanitizeRichTextHtml(rawHtml: string) {
  // SSR-safe fallback (DOMParser 없는 환경)
  if (typeof window === "undefined" || typeof DOMParser === "undefined") {
    return rawHtml
      .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, "")
      .replace(/<style[\s\S]*?>[\s\S]*?<\/style>/gi, "")
      .replace(/<(iframe|object|embed|link|meta|base)[^>]*>/gi, "")
      .replace(/\son\w+=(?:"[^"]*"|'[^']*'|[^\s>]+)/gi, "")
      .replace(/\sstyle=(?:"[^"]*"|'[^']*'|[^\s>]+)/gi, "")
      .replace(/\shref=(?:"\s*javascript:[^"]*"|'\s*javascript:[^']*'|javascript:[^\s>]+)/gi, "")
      .replace(/\ssrc=(?:"\s*javascript:[^"]*"|'\s*javascript:[^']*'|javascript:[^\s>]+)/gi, "")
  }

  // Client-side strict sanitize
  const parser = new DOMParser()
  const doc = parser.parseFromString(`<div>${rawHtml}</div>`, "text/html")
  const root = doc.body.firstElementChild as HTMLElement | null
  if (!root) return ""

  const elements = Array.from(root.querySelectorAll("*"))

  for (const el of elements) {
    const tag = el.tagName.toLowerCase()

    if (!ALLOWED_TAGS.has(tag)) {
      const parent = el.parentNode
      if (!parent) continue
      while (el.firstChild) {
        parent.insertBefore(el.firstChild, el)
      }
      parent.removeChild(el)
      continue
    }

    const href = el.getAttribute("href")?.trim() ?? ""
    const src = el.getAttribute("src")?.trim() ?? ""
    const alt = el.getAttribute("alt")?.trim() ?? ""

    for (const attr of Array.from(el.attributes)) {
      el.removeAttribute(attr.name)
    }

    if (tag === "a") {
      const isSafeHref =
        href.startsWith("http://") ||
        href.startsWith("https://") ||
        href.startsWith("mailto:")
      if (isSafeHref) {
        el.setAttribute("href", href)
        el.setAttribute("target", "_blank")
        el.setAttribute("rel", "noreferrer noopener")
      } else {
        const parent = el.parentNode
        if (!parent) continue
        while (el.firstChild) {
          parent.insertBefore(el.firstChild, el)
        }
        parent.removeChild(el)
      }
    }

    if (tag === "img") {
      const isSafeSrc =
        src.startsWith("http://") ||
        src.startsWith("https://") ||
        src.startsWith("data:image/")
      if (!isSafeSrc) {
        el.remove()
        continue
      }
      el.setAttribute("src", src)
      if (alt) el.setAttribute("alt", alt)
    }
  }

  return root.innerHTML
}

function getAuthHeaders(): HeadersInit {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  }

  const token = getAccessToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  return headers
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

export default function PostDetailPage() {
  const params = useParams()

  const [post, setPost] = useState<PostDetailResponse["data"] | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reportLoading, setReportLoading] = useState(false)
  const [currentUserId, setCurrentUserId] = useState<number | null>(null)
  const [loginRequiredPopup, setLoginRequiredPopup] = useState<LoginRequiredPopupState>({
    open: false,
    message: "로그인이 필요한 기능입니다.",
  })

  const postId = useMemo(() => {
    const rawPostId = params?.postId
    if (Array.isArray(rawPostId)) return Number(rawPostId[0])
    return Number(rawPostId)
  }, [params])

  const renderedContent = useMemo(
    () => sanitizeRichTextHtml(post?.content ?? ""),
    [post?.content]
  )

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

  const loadPost = useCallback(async () => {
    if (!postId || Number.isNaN(postId)) {
      setLoading(false)
      return
    }

    try {
      setLoading(true)
      setError(null)

      const response = await fetch(`${API_BASE_URL}/api/posts/${postId}`, {
        credentials: "include",
        headers: getAuthHeaders(),
        cache: "no-store",
      })

      if (!response.ok) {
        throw new Error("게시글을 불러오지 못했습니다.")
      }

      const res = await response.json()
      setPost(res.data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "알 수 없는 오류가 발생했습니다.")
    } finally {
      setLoading(false)
    }
  }, [postId])

  useEffect(() => {
    void loadPost()
  }, [loadPost])

  useEffect(() => {
    const loadMe = async () => {
      try {
        const res = await fetch(`${API_BASE_URL}/api/users/me`, {
          credentials: "include",
          headers: getAuthHeaders(),
        })

        if (!res.ok) return

        const data = await res.json()
        setCurrentUserId(data.data.userId)
      } catch {
        // ignore
      }
    }

    void loadMe()
  }, [])

  const isAuthor = post?.userId && currentUserId === post.userId

  const handleReportPost = async () => {
    if (!postId || Number.isNaN(postId)) return

    try {
      setReportLoading(true)
      setError(null)

      const response = await fetch(`${API_BASE_URL}/api/report/post`, {
        credentials: "include",
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify({
          targetId: postId,
          reasonType: "ETC",
          reasonDetail: "게시글 상세 페이지에서 접수한 신고입니다.",
        }),
      })
      if (!response.ok) {
        let message = await extractErrorMessage(response, "게시글 신고에 실패했습니다.")

        if (isUnauthorizedStatus(response.status) || isLoginRequiredMessage(message)) {
          setError(null)
          openLoginRequiredPopup("로그인이 필요한 기능입니다.")
          return
        }

        if (isSelfReportMessage(message)) {
          setError(null)
          openLoginRequiredPopup("자신이 작성한 게시글은 신고할 수 없습니다.")
          return
        }

        if (typeof message === "string" && message.includes("이미 신고")) {
          message = "이미 신고한 게시글입니다."
        }

        throw new Error(message)
      }

      alert("게시글 신고가 접수되었습니다.")
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
        openLoginRequiredPopup("자신이 작성한 게시글은 신고할 수 없습니다.")
        return
      }

      setError(message)
    } finally {
      setReportLoading(false)
    }
  }

  const handleDeletePost = async () => {
    if (!postId) return
    if (!confirm("정말 삭제하시겠습니까?")) return

    try {
      const res = await fetch(`${API_BASE_URL}/api/posts/${postId}`, {
        method: "DELETE",
        credentials: "include",
        headers: getAuthHeaders(),
      })

      if (!res.ok) throw new Error("삭제 실패")

      alert("삭제되었습니다.")
      window.location.href = "/"
    } catch (err) {
      alert(err instanceof Error ? err.message : "오류 발생")
    }
  }

  if (!postId || Number.isNaN(postId)) {
    return (
      <main className="mx-auto max-w-4xl px-4 py-10">
        <div className="rounded-xl border border-border bg-card p-6 text-center">
          <h1 className="text-xl font-semibold text-foreground">잘못된 게시글 경로입니다.</h1>
        </div>
      </main>
    )
  }

  return (
    <main className="mx-auto max-w-4xl px-4 py-10">
      <section className="mb-8 rounded-xl border border-border bg-card p-6 shadow-sm">
        {loading ? (
          <div>로딩 중...</div>
        ) : error ? (
          <div className="text-destructive">{error}</div>
        ) : (
          <>
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
            <div>
            <h1 className="text-2xl font-bold text-foreground">{post?.title}</h1>
            {post?.userId ? (
              <div className="mt-2 text-sm text-muted-foreground">
                작성자:{" "}
                <Link href={`/users/${post.userId}`} className="font-medium text-foreground hover:underline">
                  {post.writerName}
                </Link>
              </div>
            ) : null}
            {/* 조회수, 댓글수 추가 */}
            <div className="mt-1 text-xs text-muted-foreground">
              조회수 {post?.viewCount ?? 0} · 댓글 {post?.commentCount ?? 0}
            </div>

            <div
              className="prose prose-invert mt-6 max-w-none overflow-hidden break-words [overflow-wrap:anywhere] rounded-lg bg-muted/30 p-4 text-sm [&_p]:break-all [&_li]:break-all [&_blockquote]:break-all [&_a]:break-all [&_img]:block [&_img]:h-auto [&_img]:max-w-full [&_img]:rounded-md [&_blockquote]:border-l-4 [&_blockquote]:pl-3 [&_ul]:list-disc [&_ul]:pl-6 [&_ol]:list-decimal [&_ol]:pl-6 [&_pre]:my-3 [&_pre]:max-w-full [&_pre]:overflow-x-auto [&_pre]:rounded-md [&_pre]:border [&_pre]:border-zinc-300 [&_pre]:bg-zinc-200 [&_pre]:p-3 [&_pre]:text-zinc-900 [&_pre]:font-mono [&_pre]:text-sm"
              dangerouslySetInnerHTML={{ __html: renderedContent }}
            />

            <div className="mt-6">
              <InteractionButtons
                postId={postId}
                initialLiked={Boolean(post?.liked ?? false)}
                initialBookmarked={Boolean(post?.bookmarked ?? false)}
                initialLikeCount={post?.likeCount ?? 0}
              />
            </div>

            {isAuthor && (
              <div className="mt-4 flex items-center gap-2">
                <Link
                  href={`/write?postId=${postId}`}
                  className="rounded-md border px-4 py-2 text-sm hover:bg-muted"
                >
                  수정
                </Link>

                <button
                  onClick={handleDeletePost}
                  className="rounded-md border border-destructive/40 px-4 py-2 text-sm text-destructive hover:bg-destructive/10"
                >
                  삭제
                </button>
              </div>
            )}

            <button
              type="button"
              onClick={handleReportPost}
              disabled={reportLoading}
              className="mt-4 rounded-md border border-destructive/40 px-4 py-2 text-sm font-medium text-destructive disabled:cursor-not-allowed disabled:opacity-50"
            >
              {reportLoading ? "신고 중..." : "신고"}
            </button>
            </div>
          </>
        )}
      </section>

      <CommentSection postId={postId} onCommentsChanged={loadPost} />
    </main>
  )
}