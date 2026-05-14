"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"

export default function MyBookmarksPage() {
  const router = useRouter()

  useEffect(() => {
    router.replace("/mypage?tab=bookmarks")
  }, [router])

  return null
}