"use client"

import { useEffect, useState } from "react"
import { PostCard, type Post } from "@/components/post-card"
import Link from "next/link"
import { useParams } from "next/navigation"
import { categoryLabelMap, categorySlugMap } from "@/constants/category"

type PostPageResponse = {
  data: {
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
    }[]
    number: number
    totalPages: number
  }
}

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

const PAGE_SIZE = 10

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

export default function CategoryPage() {
  const params = useParams()
  const slug = params.slug as string

  const [posts, setPosts] = useState<Post[]>([])
  const [loading, setLoading] = useState(true)
  const [currentPage, setCurrentPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const categoryMap: Record<string, number> = Object.fromEntries(
    Object.entries(categorySlugMap).map(([id, slug]) => [slug, Number(id)])
  )

  const categoryId = categoryMap[slug]
  const categoryName = categoryLabelMap[categoryId] ?? slug

  const allCategories = Object.entries(categorySlugMap).map(([id, slug]) => ({
    slug,
    name: categoryLabelMap[Number(id)],
  }))

  const fetchPosts = async (page = 0) => {
    try {
      setLoading(true)

      if (!categoryId) return

      const response = await fetch(
        `${API_BASE_URL}/api/posts?categoryId=${categoryId}&page=${page}&size=${PAGE_SIZE}`,
        {
          cache: "no-store",
        }
      )

      if (!response.ok) {
        throw new Error("데이터 못불러옴")
      }

      const res = await response.json()
      const data: PostPageResponse["data"] = res.data

      const mapped: Post[] = data.content.map((post) => ({
        id: String(post.postId),
        title: post.title,
        excerpt: post.content,
        author: { name: post.nickName },
        category: categoryLabelMap[post.categoryId],
        categorySlug: categorySlugMap[post.categoryId],
        categoryId: post.categoryId,
        createdAt: formatTimeAgo(post.createdAt),
        likes: post.likeCount,
        comments: post.commentCount,
        views: post.viewCount,
        tags: [],
      }))

      setPosts(mapped)
      setCurrentPage(data.number)
      setTotalPages(data.totalPages)
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchPosts(0)
  }, [slug, categoryId])

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="mb-2 text-3xl font-bold">{categoryName}</h1>
      </div>

      {/* Navigation */}
      <div className="mb-8 flex flex-wrap gap-2">
        {allCategories.map((cat) => (
          <Link
            key={cat.slug}
            href={`/category/${cat.slug}`}
            className={`rounded-full px-4 py-2 text-sm ${
              cat.slug === slug
                ? "bg-[#38B8A0] text-black font-bold"
                : "bg-black text-white"
            }`}
          >
            {cat.name}
          </Link>
        ))}
      </div>

      {/* Posts */}
      {loading ? (
        <div className="py-10 text-center text-muted-foreground">
          로딩중...
        </div>
      ) : posts.length > 0 ? (
        <>
          <div className="grid gap-6">
            {posts.map((post) => (
              <PostCard key={post.id} post={post} />
            ))}
          </div>

          {/* 페이지네이션 */}
          <div className="mt-8 flex justify-center gap-2">
            {[...Array(totalPages)].map((_, index) => (
              <button
                key={index}
                onClick={() => fetchPosts(index)}
                className={`rounded border px-4 py-2 ${
                  currentPage === index
                    ? "bg-[#38B8A0] text-black font-bold"
                    : "bg-black text-white"
                }`}
              >
                {index + 1}
              </button>
            ))}
          </div>
        </>
      ) : (
        <div className="rounded-lg border p-12 text-center">
          <p className="text-lg font-semibold">
            아직 작성된 글이 없습니다
          </p>
          <Link href={`/write?category=${slug}`}>
            <button className="mt-4 rounded bg-[#38B8A0] px-4 py-2 font-bold text-black">
              글 쓰기
            </button>
          </Link>
        </div>
      )}
    </div>
  )
}