"use client"

import { useEffect, useState } from "react"
import { PostCard, type Post } from "@/components/post-card"
import { TrendingUp } from "lucide-react"
import { categoryLabelMap, categorySlugMap } from "@/constants/category"

type PostPageResponse = {
  data : {
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
    createdAt: string
    liked?: boolean
    bookmarked?: boolean
  }[]
}}

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

export default function PopularPage() {
  const [posts, setPosts] = useState<Post[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchPosts = async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/api/posts?sort=LIKES`, {
          credentials: "include",
        })

        if (!response.ok) {
          throw new Error("게시글을 불러오지 못했습니다.")
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
          liked: post.liked ?? false,
          bookmarked: post.bookmarked ?? false,
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
          <TrendingUp className="h-5 w-5 text-primary" />
        </div>
        <div>
          <h1 className="text-3xl font-bold">인기글</h1>
          <p className="text-muted-foreground">
            가장 많은 관심을 받은 글들입니다
          </p>
        </div>
      </div>

      {loading ? (
        <div className="py-10 text-center text-muted-foreground">로딩중...</div>
      ) : (
        <div className="grid gap-6">
          {posts.map((post, index) => (
            <div key={post.id} className="relative">
              <div className="absolute -left-2 -top-2 z-10 flex h-8 w-8 items-center justify-center rounded-full bg-primary text-sm font-bold text-primary-foreground">
                {index + 1}
              </div>
              <PostCard post={post} />
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
