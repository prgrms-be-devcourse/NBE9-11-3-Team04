"use client"

import { useEffect, useRef } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { persistLoginSession } from "@/lib/auth-storage"
import { exchangeOAuthCode } from "@/lib/auth"

export default function OAuthCallbackPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const isProcessingRef = useRef(false)

  useEffect(() => {
    if (isProcessingRef.current) return
    isProcessingRef.current = true

    const errorCode = searchParams.get("errorCode")
    const code = searchParams.get("code")

    const run = async () => {
      // OAuth 실패 케이스
      if (errorCode) {
        router.replace(`/login?oauth=error&errorCode=${errorCode}`)
        return
      }

      // code가 있으면 토큰 교환 실행
      if (code) {
        const usedCodeKey = `oauth_code_used:${code}`

        // 중복 실행 방지
        if (sessionStorage.getItem(usedCodeKey) === "1") {
          return
        }

        try {
          const data = await exchangeOAuthCode({ code })

          sessionStorage.setItem(usedCodeKey, "1")

          persistLoginSession(
            data.accessToken,
            data.nickname,
            data.email
          )

          router.replace("/mypage")
          router.refresh()
          return
        } catch (error) {
          console.error("OAuth exchange error:", error)
          router.replace("/login?oauth=error&errorCode=OAUTH2_TOKEN_ISSUE")
          return
        }
      }

      // code도 없으면 로그인 페이지로 이동
      router.replace("/login")
    }

    void run()
  }, [router, searchParams])

  return (
    <div className="mx-auto flex min-h-[60vh] max-w-4xl items-center justify-center px-4 py-8 sm:px-6 lg:px-8">
      <div className="w-full max-w-md rounded-lg border border-border bg-card p-8 text-center shadow-sm">
        <h1 className="text-xl font-semibold text-foreground">
          OAuth 로그인 처리 중
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">
          로그인 정보를 확인하고 있습니다. 잠시만 기다려주세요.
        </p>
      </div>
    </div>
  )
}