"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"

export default function MyCommentsPage() {
  const router = useRouter()

  useEffect(() => {
    router.replace("/mypage?tab=comments")
  }, [router])

  return null
}