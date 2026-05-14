"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"

export default function MyLikesPage() {
  const router = useRouter()

  useEffect(() => {
    router.replace("/mypage?tab=likes")
  }, [router])

  return null
}