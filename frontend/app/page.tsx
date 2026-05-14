"use client"

import { useCallback, useEffect, useState } from "react"
import { PostCard, type Post } from "@/components/post-card"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { TrendingUp, Clock, Users } from "lucide-react"
import { categoryLabelMap, categorySlugMap } from "@/constants/category"

import {
  AUTH_CHANGED_EVENT,
  getAccessToken,
  getAuthSnapshot,
} from "@/lib/auth-storage"
import { apiFetch } from "@/lib/api"

type SuccessResponse<T> = {
  code: string
  message: string
  timestamp: string
  data: T
}

type PostPageResponse = {
  content: {
    postId: number
    title: string
    content: string
    nickName: string
    categoryId: number
    viewCount: number
    likeCount: number
    commentCount: number
    createdAt: string
    liked?: boolean
    bookmarked?: boolean
  }[]
}

type BookmarkedPostResponse = {
  postId: number
  title: string
  authorNickname: string
  categoryId: number
  likeCount: number
  commentCount: number
  viewCount: number
  createdAt: string
}

type LikedPostResponse = {
  postId: number
}

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

function formatTimeAgo(dateString: string) {
  const now = new Date()
  const created = new Date(dateString)
  const diffMs = now.getTime() - created.getTime()

  const diffMinutes = Math.floor(diffMs / (1000 * 60))
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

  if (diffMinutes < 1) return "방금 전"
  if (diffMinutes < 60) return `${diffMinutes}분 전`
  if (diffHours < 24) return `${diffHours}시간 전`
  if (diffDays < 7) return `${diffDays}일 전`

  return created.toLocaleDateString("ko-KR")
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

function mapBookmarkedPostsToPostCard(
  posts: BookmarkedPostResponse[],
  likedPostIds: Set<number>,
  bookmarkedPostIds: Set<number>
): Post[] {
  return posts.map((post) => ({
    id: String(post.postId),
    title: post.title,
    excerpt: "",
    author: { name: post.authorNickname },
    category: categoryLabelMap[post.categoryId],
    categorySlug: categorySlugMap[post.categoryId],
    categoryId: post.categoryId,
    createdAt: formatRelativeDate(post.createdAt),
    likes: post.likeCount,
    comments: post.commentCount,
    views: post.viewCount,
    tags: [],
    liked: likedPostIds.has(post.postId),
    bookmarked: bookmarkedPostIds.has(post.postId),
  }))
}

export default function HomePage() {
  const [activeTab, setActiveTab] = useState("popular")
  const [posts, setPosts] = useState<Post[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadPosts = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)

      if (activeTab === "popular" || activeTab === "latest") {
        let url = `${API_BASE_URL}/api/posts`

        if (activeTab === "popular") {
          url = `${API_BASE_URL}/api/posts?sort=LIKES`
        } else if (activeTab === "latest") {
          url = `${API_BASE_URL}/api/posts?sort=LATEST`
        }

        const response = await fetch(url, {
          method: "GET",
          credentials: "include",
          headers: getAuthHeaders(),
          cache: "no-store",
        })

        if (!response.ok) {
          throw new Error("게시글 목록을 불러오지 못했습니다.")
        }

        const res = await response.json()
        const data: PostPageResponse = res.data

        const mapped: Post[] = data.content.map((post) => ({
          id: String(post.postId),
          title: post.title,
          excerpt: post.content,
          author: {
            name: post.nickName,
          },
          category: categoryLabelMap[post.categoryId],
          categorySlug: categorySlugMap[post.categoryId],
          categoryId: post.categoryId,
          createdAt: formatTimeAgo(post.createdAt),
          likes: post.likeCount,
          comments: post.commentCount,
          views: post.viewCount,
          tags: [],
          liked: Boolean(post.liked),
          bookmarked: Boolean(post.bookmarked),
        }))

        setPosts(mapped)
        return
      }

      if (activeTab === "feed") {
        const auth = getAuthSnapshot()

        if (!auth.isLoggedIn) {
          setPosts([])
          setError("북마크는 로그인 후 확인할 수 있습니다.")
          return
        }

        const [bookmarksRes, likesRes] = await Promise.all([
          apiFetch<SuccessResponse<BookmarkedPostResponse[]>>("/api/mypage/bookmarks", {
            method: "GET",
            auth: true,
          }),
          apiFetch<SuccessResponse<LikedPostResponse[]>>("/api/mypage/likes", {
            method: "GET",
            auth: true,
          }),
        ])

        const bookmarks = bookmarksRes?.data ?? []
        const likes = likesRes?.data ?? []

        const bookmarkedPostIds = new Set(bookmarks.map((post) => post.postId))
        const likedPostIds = new Set(likes.map((post) => post.postId))

        const mapped = mapBookmarkedPostsToPostCard(
          bookmarks,
          likedPostIds,
          bookmarkedPostIds
        )

        setPosts(mapped)
        return
      }
    } catch (err) {
      if (err instanceof Error && err.message === "UNAUTHORIZED") {
        setError("북마크는 로그인 후 확인할 수 있습니다.")
      } else {
        setError(
          err instanceof Error
            ? err.message
            : "알 수 없는 오류가 발생했습니다."
        )
      }
    } finally {
      setLoading(false)
    }
  }, [activeTab])

  useEffect(() => {
    void loadPosts()
  }, [loadPosts])

  useEffect(() => {
    const handleAuthChanged = () => {
      void loadPosts()
    }

    const handleWindowFocus = () => {
      void loadPosts()
    }

    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        void loadPosts()
      }
    }

    window.addEventListener(AUTH_CHANGED_EVENT, handleAuthChanged)
    window.addEventListener("focus", handleWindowFocus)
    document.addEventListener("visibilitychange", handleVisibilityChange)

    return () => {
      window.removeEventListener(AUTH_CHANGED_EVENT, handleAuthChanged)
      window.removeEventListener("focus", handleWindowFocus)
      document.removeEventListener("visibilitychange", handleVisibilityChange)
    }
  }, [loadPosts])

  const renderPostList = () => {
    if (loading) {
      return <div className="py-10 text-center">로딩 중...</div>
    }

    if (error) {
      return <div className="py-10 text-center text-destructive">{error}</div>
    }

    if (posts.length === 0) {
      return (
        <div className="rounded-lg border border-border bg-card p-6 text-center text-muted-foreground">
          {activeTab === "feed"
            ? "북마크한 게시글이 없습니다."
            : "게시글이 없습니다."}
        </div>
      )
    }

    return (
      <div className="grid gap-6">
        {posts.map((post) => (
          <PostCard
            key={post.id}
            post={post}
            onLikeToggle={(postId, nextLiked, nextLikeCount) => {
              setPosts((prev) =>
                prev.map((p) =>
                  Number(p.id) === postId
                    ? { ...p, liked: nextLiked, likes: nextLikeCount }
                    : p
                )
              )
            }}
            onBookmarkToggle={(postId, nextBookmarked) => {
              setPosts((prev) =>
                prev.map((p) =>
                  Number(p.id) === postId
                    ? { ...p, bookmarked: nextBookmarked }
                    : p
                )
              )
            }}
          />
        ))}
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <section className="mb-12 text-center">
        <h1 className="mb-4 text-4xl font-bold sm:text-5xl">
          개발자들의 <span className="text-primary">지식 허브</span>
        </h1>
        <p className="mx-auto max-w-2xl text-lg text-muted-foreground">
          최신 기술 트렌드, 실무 경험을 나눠보세요.
        </p>
      </section>

      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="mx-auto mb-8 grid w-full max-w-md grid-cols-3">
          <TabsTrigger value="popular">
            <TrendingUp className="h-4 w-4" />
            인기글
          </TabsTrigger>
          <TabsTrigger value="latest">
            <Clock className="h-4 w-4" />
            최신글
          </TabsTrigger>
          <TabsTrigger value="feed">
            <Users className="h-4 w-4" />
            북마크
          </TabsTrigger>
        </TabsList>

        <TabsContent value="popular">{renderPostList()}</TabsContent>
        <TabsContent value="latest">{renderPostList()}</TabsContent>
        <TabsContent value="feed">{renderPostList()}</TabsContent>
      </Tabs>
    </div>
  )
}