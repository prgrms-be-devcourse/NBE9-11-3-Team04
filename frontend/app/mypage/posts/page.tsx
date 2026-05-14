"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"

export default function MyPostsPage() {
  const router = useRouter()

  useEffect(() => {
    router.replace("/mypage?tab=posts")
  }, [router])

  return null
}