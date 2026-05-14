"use client"

import Link from "next/link"
import { useParams } from "next/navigation"
import { useEffect, useMemo, useState } from "react"
import { apiFetch } from "@/lib/api"

type PublicProfilePost = {
  postId: number
  title: string
  likeCount: number
  commentCount: number
  createdAt: string
}

type PublicProfile = {
  userId: number
  nickname: string
  posts: PublicProfilePost[]
}

type SuccessResponse<T> = {
  code: string
  message: string
  timestamp: string
  data: T
}

function formatDate(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString("ko-KR")
}

export default function UserProfilePage() {
  const params = useParams()
  const [profile, setProfile] = useState<PublicProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")

  const userId = useMemo(() => {
    const raw = params?.userId
    if (Array.isArray(raw)) return Number(raw[0])
    return Number(raw)
  }, [params])

  useEffect(() => {
    const fetchProfile = async () => {
      if (!userId || Number.isNaN(userId)) {
        setError("잘못된 사용자 경로입니다.")
        setLoading(false)
        return
      }

      try {
        setLoading(true)
        setError("")

        const response = await apiFetch<SuccessResponse<PublicProfile>>(
          `/api/users/${userId}/profile`,
          { method: "GET" }
        )

        setProfile(response.data)
      } catch (err) {
        const message =
          err instanceof Error ? err.message : "사용자 정보를 불러오지 못했습니다."
        setError(message)
      } finally {
        setLoading(false)
      }
    }

    void fetchProfile()
  }, [userId])

  if (loading) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="rounded-lg border border-border bg-card p-8 text-center text-muted-foreground">
          로딩 중...
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="rounded-lg border border-border bg-card p-8 text-center text-destructive">
          {error}
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      <section className="mb-6 rounded-lg border border-border bg-card p-6">
      <h1 className="text-2xl font-bold text-foreground">{profile?.nickname?.startsWith("withdrawn_") ? "탈퇴한 계정" : profile?.nickname}</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          공개 게시글 {profile?.posts.length ?? 0}개
        </p>
      </section>

      <section className="space-y-3">
        {profile?.posts.length ? (
          profile.posts.map((post) => (
            <Link
              key={post.postId}
              href={`/posts/${post.postId}`}
              className="block rounded-lg border border-border bg-card p-4 transition-colors hover:border-primary/50"
            >
              <p className="font-semibold text-foreground">{post.title}</p>
              <p className="mt-2 text-xs text-muted-foreground">
                {formatDate(post.createdAt)} · 좋아요 {post.likeCount} · 댓글 {post.commentCount}
              </p>
            </Link>
          ))
        ) : (
          <div className="rounded-lg border border-border bg-card p-6 text-center text-sm text-muted-foreground">
            공개된 게시글이 없습니다.
          </div>
        )}
      </section>
    </div>
  )
}
