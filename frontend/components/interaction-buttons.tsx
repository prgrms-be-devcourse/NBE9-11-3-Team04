"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import type React from "react"
import { Heart, Bookmark } from "lucide-react"
import { Button } from "@/components/ui/button"
import { getAccessToken, getAuthSnapshot } from "@/lib/auth-storage"

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

type InteractionButtonsProps = {
  postId: number
  initialLiked?: boolean
  initialBookmarked?: boolean
  initialLikeCount?: number
  onLikeToggle?: (nextLiked: boolean, nextLikeCount: number) => void
  onBookmarkToggle?: (nextBookmarked: boolean) => void
}

type LikeResponse = {
  postId: number
  liked: boolean
  likeCount: number
  message?: string
}

type BookmarkResponse = {
  postId: number
  bookmarked: boolean
  message?: string
}

type LikeWrappedResponse = {
  code?: string
  message?: string
  data?: LikeResponse
}

type BookmarkWrappedResponse = {
  code?: string
  message?: string
  data?: BookmarkResponse
}

type LoginRequiredPopupState = {
  open: boolean
  message: string
}

async function parseResponse(res: Response) {
  const contentType = res.headers.get("content-type") || ""
  const rawText = await res.text()

  if (contentType.includes("application/json")) {
    try {
      return JSON.parse(rawText)
    } catch {
      throw new Error("서버 JSON 응답을 읽는 중 오류가 발생했습니다.")
    }
  }

  if (rawText.includes("<!DOCTYPE html>") || rawText.includes("<html")) {
    throw new Error("서버가 JSON 대신 HTML을 반환했습니다.")
  }

  return rawText
}

function getAuthHeaders(): HeadersInit {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  }

  const token = getAccessToken()
  if (token && token !== "oauth-cookie-session") {
    headers.Authorization = `Bearer ${token}`
  }

  return headers
}

function isUnauthorizedStatus(status: number): boolean {
  return status === 401 || status === 403
}

function isLoginRequiredMessage(message: string | null | undefined): boolean {
  if (!message) return false
  return message.includes("인증") || message.includes("로그인")
}

export default function InteractionButtons({
  postId,
  initialLiked = false,
  initialBookmarked = false,
  initialLikeCount = 0,
  onLikeToggle,
  onBookmarkToggle,
}: InteractionButtonsProps) {
  const [liked, setLiked] = useState(initialLiked)
  const [bookmarked, setBookmarked] = useState(initialBookmarked)
  const [likeCount, setLikeCount] = useState(initialLikeCount)
  const [likeLoading, setLikeLoading] = useState(false)
  const [bookmarkLoading, setBookmarkLoading] = useState(false)

  const [loginRequiredPopup, setLoginRequiredPopup] = useState<LoginRequiredPopupState>({
    open: false,
    message: "로그인이 필요한 기능입니다.",
  })

  const loginPath = useMemo(() => "/login", [])

  const openLoginRequiredPopup = useCallback((message = "로그인이 필요한 기능입니다.") => {
    setLoginRequiredPopup({ open: true, message })
  }, [])

  const closeLoginRequiredPopup = useCallback(() => {
    setLoginRequiredPopup((prev) => ({ ...prev, open: false }))
  }, [])

  const moveToLoginPage = useCallback(() => {
    if (typeof window !== "undefined") {
      window.location.href = loginPath
    }
  }, [loginPath])

  useEffect(() => {
    setLiked(initialLiked)
  }, [initialLiked])

  useEffect(() => {
    setBookmarked(initialBookmarked)
  }, [initialBookmarked])

  useEffect(() => {
    setLikeCount(initialLikeCount)
  }, [initialLikeCount])

  const requireAuth = () => {
    const auth = getAuthSnapshot()
    if (!auth.isLoggedIn) {
      openLoginRequiredPopup()
      return false
    }
    return true
  }

  const handleLike = async (e: React.MouseEvent<HTMLButtonElement>) => {
    e.preventDefault()
    e.stopPropagation()

    if (!requireAuth() || likeLoading) return

    const prevLiked = liked
    const prevLikeCount = likeCount
    const optimisticLiked = !prevLiked
    const optimisticLikeCount = optimisticLiked
      ? prevLikeCount + 1
      : Math.max(0, prevLikeCount - 1)

    setLiked(optimisticLiked)
    setLikeCount(optimisticLikeCount)
    onLikeToggle?.(optimisticLiked, optimisticLikeCount)

    setLikeLoading(true)

    try {
      const method = prevLiked ? "DELETE" : "POST"

      const res = await fetch(`${API_BASE_URL}/api/posts/${postId}/likes`, {
        method,
        credentials: "include",
        headers: getAuthHeaders(),
      })

      const parsed = await parseResponse(res)

      if (!res.ok) {
        const errorMessage =
          parsed?.message || parsed?.data?.message || "좋아요 실패"

        if (isUnauthorizedStatus(res.status) || isLoginRequiredMessage(errorMessage)) {
          openLoginRequiredPopup(errorMessage || "로그인이 필요한 기능입니다.")
          setLiked(prevLiked)
          setLikeCount(prevLikeCount)
          onLikeToggle?.(prevLiked, prevLikeCount)
          return
        }

        throw new Error(errorMessage)
      }

      const wrapped = parsed as LikeWrappedResponse
      const nextLiked = wrapped.data?.liked ?? optimisticLiked
      const nextLikeCount = wrapped.data?.likeCount ?? optimisticLikeCount

      setLiked(nextLiked)
      setLikeCount(nextLikeCount)
      onLikeToggle?.(nextLiked, nextLikeCount)
    } catch (err) {
      console.error(err)
      setLiked(prevLiked)
      setLikeCount(prevLikeCount)
      onLikeToggle?.(prevLiked, prevLikeCount)
    } finally {
      setLikeLoading(false)
    }
  }

  const handleBookmark = async (e: React.MouseEvent<HTMLButtonElement>) => {
    e.preventDefault()
    e.stopPropagation()

    if (!requireAuth() || bookmarkLoading) return

    const prev = bookmarked
    const optimistic = !prev

    setBookmarked(optimistic)
    onBookmarkToggle?.(optimistic)

    setBookmarkLoading(true)

    try {
      const method = prev ? "DELETE" : "POST"

      const res = await fetch(`${API_BASE_URL}/api/posts/${postId}/bookmarks`, {
        method,
        credentials: "include",
        headers: getAuthHeaders(),
      })

      const parsed = await parseResponse(res)

      if (!res.ok) {
        const errorMessage =
          parsed?.message || parsed?.data?.message || "북마크 실패"

        if (isUnauthorizedStatus(res.status) || isLoginRequiredMessage(errorMessage)) {
          openLoginRequiredPopup(errorMessage || "로그인이 필요한 기능입니다.")
          setBookmarked(prev)
          onBookmarkToggle?.(prev)
          return
        }

        throw new Error(errorMessage)
      }

      const wrapped = parsed as BookmarkWrappedResponse
      const nextBookmarked = wrapped.data?.bookmarked ?? optimistic

      setBookmarked(nextBookmarked)
      onBookmarkToggle?.(nextBookmarked)
    } catch (err) {
      console.error(err)
      setBookmarked(prev)
      onBookmarkToggle?.(prev)
    } finally {
      setBookmarkLoading(false)
    }
  }

  return (
    <div className="flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
      {loginRequiredPopup.open && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
          <div className="w-full max-w-sm rounded-xl border bg-card p-6">
            <h3 className="text-lg font-semibold">로그인 안내</h3>
            <p className="mt-3 text-sm">{loginRequiredPopup.message}</p>
            <div className="mt-6 flex justify-end gap-2">
              <button onClick={closeLoginRequiredPopup}>취소</button>
              <button onClick={moveToLoginPage}>로그인</button>
            </div>
          </div>
        </div>
      )}

      <Button
        type="button"
        variant={liked ? "default" : "outline"}
        size="sm"
        onClick={handleLike}
        disabled={likeLoading}
      >
        <Heart className={`h-4 w-4 ${liked ? "fill-current" : ""}`} />
        {likeCount}
      </Button>

      <Button
        type="button"
        variant={bookmarked ? "default" : "outline"}
        size="icon"
        onClick={handleBookmark}
        disabled={bookmarkLoading}
      >
        <Bookmark className={`h-4 w-4 ${bookmarked ? "fill-current" : ""}`} />
      </Button>
    </div>
  )
}