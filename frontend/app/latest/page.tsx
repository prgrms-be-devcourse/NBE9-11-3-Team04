"use client"

import { useEffect, useState } from "react"
import { PostCard, type Post } from "@/components/post-card"
import { Clock } from "lucide-react"
import { getAccessToken } from "@/lib/auth-storage"
import { categoryLabelMap, categorySlugMap } from "@/constants/category"

type PostPageResponse = {
  data:{
  content: {
    postId: number
    userId?: number
    title: string
    content: string
    nickName: string
    categoryId: number
    viewCount: number
    likeCount: number
    commentCount: number
    liked: boolean
    bookmarked: boolean
    createdAt: string
  }[]
}
}
const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

const formatTimeAgo = (dateString: string) => {
  const date = new Date(dateString)
  const diff = Date.now() - date.getTime()

  const minutes = Math.floor(diff / 1000 / 60)
  if (minutes < 1) return "방금 전"
  if (minutes < 60) return `${minutes}분 전`

  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}시간 전`

  const days = Math.floor(hours / 24)
  return `${days}일 전`
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

export default function LatestPage() {
  const [posts, setPosts] = useState<Post[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchPosts = async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/api/posts?sort=LATEST`, {
          headers: getAuthHeaders(),
          credentials: "include",
          cache: "no-store",
        })

        if (!response.ok) {
          throw new Error("최신글을 불러오지 못했습니다.")
        }

        const res = await response.json()
        const data: PostPageResponse["data"] = res.data

        const mapped: Post[] = data.content.map((post) => ({
          id: String(post.postId),
          title: post.title,
          excerpt: post.content,
          author: {
            name: post.nickName,
            userId: post.userId,
          },
          category: categoryLabelMap[post.categoryId],
          categorySlug: categorySlugMap[post.categoryId],
          categoryId: post.categoryId,
          createdAt: formatTimeAgo(post.createdAt),
          likes: post.likeCount,
          comments: post.commentCount,
          views: post.viewCount,
          tags: [],
          liked: post.liked,
          bookmarked: post.bookmarked,
        }))

        setPosts(mapped)
      } catch (err) {
        console.error(err)
      } finally {
        setLoading(false)
      }
    }

    fetchPosts()
  }, [])

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-8 flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10">
          <Clock className="h-5 w-5 text-primary" />
        </div>
        <div>
          <h1 className="text-3xl font-bold">최신글</h1>
          <p className="text-muted-foreground">방금 올라온 따끈따끈한 글들입니다</p>
        </div>
      </div>

      {loading ? (
        <div className="py-10 text-center text-muted-foreground">Loading...</div>
      ) : (
        <div className="grid gap-6">
          {posts.map((post) => (
            <PostCard
              key={post.id}
              post={post}
              onBookmarkToggle={(postId, nextBookmarked) => {
                setPosts(prev =>
                  prev.map(p =>
                    Number(p.id) === postId
                      ? { ...p, bookmarked: nextBookmarked }
                      : p
                  )
                )
              }}
            />
          ))}
        </div>
      )}
    </div>
  )
}