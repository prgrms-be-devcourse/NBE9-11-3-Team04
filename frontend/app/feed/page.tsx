"use client"

import { useEffect, useMemo, useState } from "react"
import { useRouter } from "next/navigation"
import { PostCard, type Post } from "@/components/post-card"
import { Bookmark, Search } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { apiFetch } from "@/lib/api"
import { getAuthSnapshot } from "@/lib/auth-storage"
import Link from "next/link"
import { categoryLabelMap, categorySlugMap } from "@/constants/category"

type SuccessResponse<T> = {
  code: string
  message: string
  timestamp: string
  data: T
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

export default function FeedPage() {
  const router = useRouter()
  const [searchQuery, setSearchQuery] = useState("")
  const [bookmarkedPosts, setBookmarkedPosts] = useState<BookmarkedPostResponse[]>([])
  const [likedPostIds, setLikedPostIds] = useState<Set<number>>(new Set())
  const [bookmarkedPostIds, setBookmarkedPostIds] = useState<Set<number>>(new Set())
  const [loading, setLoading] = useState(true)
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
        setError("")

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

        setBookmarkedPosts(bookmarks)
        setBookmarkedPostIds(new Set(bookmarks.map((post) => post.postId)))
        setLikedPostIds(new Set(likes.map((post) => post.postId)))
      } catch (err) {
        if (err instanceof Error && err.message === "UNAUTHORIZED") {
          router.replace("/login")
          return
        }

        console.error(err)
        setError("북마크한 글을 불러오지 못했습니다.")
      } finally {
        setLoading(false)
      }
    }

    void fetchData()
  }, [router])

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

    if (!nextBookmarked) {
      setBookmarkedPosts((prev) => prev.filter((post) => post.postId !== postId))
    }
  }

  const filteredPosts = useMemo(() => {
    const keyword = searchQuery.trim().toLowerCase()

    if (!keyword) return bookmarkedPosts

    return bookmarkedPosts.filter(
      (post) =>
        post.title.toLowerCase().includes(keyword) ||
        post.authorNickname.toLowerCase().includes(keyword)
    )
  }, [bookmarkedPosts, searchQuery])

  const postCards = useMemo(
    () => mapBookmarkedPostsToPostCard(filteredPosts, likedPostIds, bookmarkedPostIds),
    [filteredPosts, likedPostIds, bookmarkedPostIds]
  )

  if (loading) return null

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="grid gap-8 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <div className="mb-8 flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10">
              <Bookmark className="h-5 w-5 text-primary" />
            </div>
            <div>
              <h1 className="text-3xl font-bold text-foreground">북마크</h1>
              <p className="text-muted-foreground">북마크한 게시글을 모아봅니다</p>
            </div>
          </div>

          {error ? (
            <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
              {error}
            </div>
          ) : postCards.length > 0 ? (
            <div className="grid gap-6">
              {postCards.map((post) => (
                <PostCard
                  key={post.id}
                  post={post}
                  onBookmarkToggle={handleBookmarkToggle}
                />
              ))}
            </div>
          ) : (
            <div className="rounded-lg border border-border bg-card p-12 text-center">
              <Bookmark className="mx-auto mb-4 h-12 w-12 text-muted-foreground" />
              <h3 className="mb-2 text-lg font-semibold text-foreground">
                북마크한 글이 없습니다
              </h3>
              <p className="mb-6 text-sm text-muted-foreground">
                관심있는 게시글을 북마크하면 이곳에서 모아볼 수 있습니다.
              </p>

              <Link href="/">
                <Button className="bg-primary text-primary-foreground hover:bg-primary/90">
                  게시글 둘러보기
                </Button>
              </Link>
            </div>
          )}
        </div>

        <div className="space-y-6">
          <div className="rounded-lg border border-border bg-card p-6">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="font-semibold text-foreground">북마크한 글</h2>
              <span className="text-sm text-muted-foreground">
                {bookmarkedPosts.length}개
              </span>
            </div>
            <p className="text-sm text-muted-foreground">
              저장해둔 게시글을 한곳에서 빠르게 확인할 수 있습니다.
            </p>
          </div>

          <div className="rounded-lg border border-border bg-card p-6">
            <h2 className="mb-4 font-semibold text-foreground">북마크 검색</h2>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                type="text"
                placeholder="제목 또는 작성자 검색..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="bg-secondary pl-9"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}