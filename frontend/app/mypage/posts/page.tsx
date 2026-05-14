"use client"

import { useEffect, useMemo, useState } from "react"
import { useRouter } from "next/navigation"
import { PostCard, type Post } from "@/components/post-card"
import { apiFetch } from "@/lib/api"
import { getAuthSnapshot } from "@/lib/auth-storage"

type SuccessResponse<T> = {
  code: string
  message: string
  timestamp: string
  data: T
}

type MyPostResponse = {
  postId: number
  title: string
  likeCount: number
  commentCount: number
  viewCount: number
  createdAt: string
}

type MyProfileResponse = {
  nickname: string
}

type LikedPostResponse = {
  postId: number
}

type BookmarkedPostResponse = {
  postId: number
}

function formatRelativeDate(value: string) {
  if (!value) return ""

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value

  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMinutes = Math.floor(diffMs / 1000 / 60)
  const diffHours = Math.floor(diffMinutes / 60)
  const diffDays = Math.floor(diffHours / 24)

  if (diffMinutes < 1) return "방금 전"
  if (diffMinutes < 60) return `${diffMinutes}분 전`
  if (diffHours < 24) return `${diffHours}시간 전`
  if (diffDays < 30) return `${diffDays}일 전`

  return date.toLocaleDateString("ko-KR")
}

function mapMyPostsToPostCard(
  posts: MyPostResponse[],
  nickname: string,
  likedPostIds: Set<number>,
  bookmarkedPostIds: Set<number>
): Post[] {
  return posts.map((post) => ({
    id: String(post.postId),
    title: post.title,
    excerpt: "",
    author: { name: nickname },
    category: "내 글",
    categorySlug: "my-posts",
    categoryId: 0,
    createdAt: formatRelativeDate(post.createdAt),
    likes: post.likeCount,
    comments: post.commentCount,
    views: post.viewCount,
    tags: [],
    liked: likedPostIds.has(post.postId),
    bookmarked: bookmarkedPostIds.has(post.postId),
  }))
}

export default function MyPostsPage() {
  const router = useRouter()
  const [posts, setPosts] = useState<MyPostResponse[]>([])
  const [nickname, setNickname] = useState("")
  const [likedPostIds, setLikedPostIds] = useState<Set<number>>(new Set())
  const [bookmarkedPostIds, setBookmarkedPostIds] = useState<Set<number>>(new Set())
  const [loading, setLoading] = useState(true)
  const [interactionReady, setInteractionReady] = useState(false)
  const [error, setError] = useState("")

  useEffect(() => {
    const auth = getAuthSnapshot()

    if (!auth.isLoggedIn) {
      router.replace("/login")
      return
    }

    const fetchData = async () => {
      try {
        setLoading(true)
        setInteractionReady(false)
        setError("")

        const [postsRes, profileRes, likesRes, bookmarksRes] = await Promise.all([
          apiFetch<SuccessResponse<MyPostResponse[]>>("/api/mypage/posts", {
            method: "GET",
            auth: true,
          }),
          apiFetch<SuccessResponse<MyProfileResponse>>("/api/mypage", {
            method: "GET",
            auth: true,
          }),
          apiFetch<SuccessResponse<LikedPostResponse[]>>("/api/mypage/likes", {
            method: "GET",
            auth: true,
          }),
          apiFetch<SuccessResponse<BookmarkedPostResponse[]>>("/api/mypage/bookmarks", {
            method: "GET",
            auth: true,
          }),
        ])

        const nextPosts = postsRes?.data ?? []
        const nextNickname = profileRes?.data?.nickname ?? ""
        const nextLikedPostIds = new Set((likesRes?.data ?? []).map((post) => post.postId))
        const nextBookmarkedPostIds = new Set(
          (bookmarksRes?.data ?? []).map((post) => post.postId)
        )

        setPosts(nextPosts)
        setNickname(nextNickname)
        setLikedPostIds(nextLikedPostIds)
        setBookmarkedPostIds(nextBookmarkedPostIds)
        setInteractionReady(true)
      } catch (err) {
        if (err instanceof Error && err.message === "UNAUTHORIZED") {
          router.replace("/login")
          return
        }

        console.error(err)
        setError("내가 쓴 글을 불러오지 못했습니다.")
      } finally {
        setLoading(false)
      }
    }

    void fetchData()
  }, [router])

  const handleLikeToggle = (
    postId: number,
    nextLiked: boolean,
    nextLikeCount: number
  ) => {
    setLikedPostIds((prev) => {
      const next = new Set(prev)

      if (nextLiked) {
        next.add(postId)
      } else {
        next.delete(postId)
      }

      return next
    })

    setPosts((prev) =>
      prev.map((post) =>
        post.postId === postId
          ? { ...post, likeCount: nextLikeCount }
          : post
      )
    )
  }

  const handleBookmarkToggle = (postId: number, nextBookmarked: boolean) => {
    setBookmarkedPostIds((prev) => {
      const next = new Set(prev)

      if (nextBookmarked) {
        next.add(postId)
      } else {
        next.delete(postId)
      }

      return next
    })
  }

  const postCards = useMemo(
    () => mapMyPostsToPostCard(posts, nickname, likedPostIds, bookmarkedPostIds),
    [posts, nickname, likedPostIds, bookmarkedPostIds]
  )

  if (loading || !interactionReady) return null

  return (
    <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      {error ? (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
          {error}
        </div>
      ) : null}

      {!error && postCards.length === 0 ? (
        <div className="rounded-lg border border-border bg-card p-12 text-center">
          <h3 className="mb-2 text-lg font-semibold text-foreground">작성한 글이 없습니다</h3>
          <p className="text-sm text-muted-foreground">첫 번째 글을 작성해보세요</p>
        </div>
      ) : null}

      {!error && postCards.length > 0 ? (
        <div className="grid gap-6">
          {postCards.map((post) => (
            <PostCard
              key={post.id}
              post={post}
              onLikeToggle={(postId, nextLiked, nextLikeCount) =>
                handleLikeToggle(postId, nextLiked, nextLikeCount)
              }
              onBookmarkToggle={(postId, nextBookmarked) =>
                handleBookmarkToggle(postId, nextBookmarked)
              }
            />
          ))}
        </div>
      ) : null}
    </main>
  )
}