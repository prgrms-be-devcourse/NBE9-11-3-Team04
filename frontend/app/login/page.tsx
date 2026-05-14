"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useRouter, useSearchParams } from "next/navigation"
import { Code2, Eye, EyeOff, Github } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Checkbox } from "@/components/ui/checkbox"
import { login } from "@/lib/auth"
import { persistLoginSession } from "@/lib/auth-storage"

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

function getOauthErrorMessage(errorCode: string | null) {
  switch (errorCode) {
    case "OAUTH2_CANCELLED":
      return "로그인이 취소되었습니다."
    case "OAUTH2_INVALID_STATE":
      return "보안 검증에 실패했습니다. 다시 시도해주세요."
    case "OAUTH2_MEMBER_BLACKLISTED":
      return "이용이 제한된 계정입니다."
    case "OAUTH2_INVALID_PRINCIPAL":
      return "OAuth 사용자 정보 처리에 실패했습니다."
    case "OAUTH2_TOKEN_ISSUE":
      return "토큰 발급에 실패했습니다."
    case "OAUTH2_LOGIN_FAILED":
      return "OAuth 로그인에 실패했습니다."
    default:
      return "OAuth 로그인 중 오류가 발생했습니다."
  }
}

export default function LoginPage() {
  const router = useRouter()
  const searchParams = useSearchParams()

  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState("")
  const [formData, setFormData] = useState({
    email: "",
    password: "",
    rememberMe: false,
  })

  useEffect(() => {
    const oauth = searchParams.get("oauth")
    if (!oauth) return

    if (oauth === "pending_signup") {
      router.replace("/oauth/signup")
      return
    }

    if (oauth === "error") {
      const errorCode = searchParams.get("errorCode")
      setError(getOauthErrorMessage(errorCode))
    }
  }, [searchParams, router])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError("")
    setIsLoading(true)

    try {
      const data = await login({
        email: formData.email,
        password: formData.password,
      })

      if (!data.accessToken) {
        throw new Error("토큰 응답이 없습니다.")
      }

      persistLoginSession(data.accessToken, data.nickname, data.email)
      router.push("/")
    } catch (err) {
      setError(err instanceof Error ? err.message : "로그인에 실패했습니다.")
    } finally {
      setIsLoading(false)
    }
  }

  const handleGithubLogin = () => {
    setError("")
    window.location.href = `${API_BASE_URL}/oauth2/authorization/github`
  }

  const handleKakaoLogin = () => {
    setError("")
    window.location.href = `${API_BASE_URL}/oauth2/authorization/kakao`
  }

  const handleGoogleLogin = () => {
    setError("")
    window.location.href = `${API_BASE_URL}/oauth2/authorization/google`
  }

  return (
    <div className="flex min-h-[calc(100vh-8rem)] items-center justify-center px-4 py-12">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <Link href="/" className="inline-flex items-center gap-2">
            <Code2 className="h-8 w-8 text-primary" />
            <span className="text-2xl font-bold text-foreground">DevC</span>
          </Link>
          <p className="mt-2 text-muted-foreground">계정으로 로그인하세요</p>
        </div>

        <div className="rounded-lg border border-border bg-card p-6">
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="email">이메일</Label>
              <Input
                id="email"
                type="email"
                placeholder="name@example.com"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                required
                className="bg-secondary"
              />
            </div>

            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label htmlFor="password">비밀번호</Label>
                <Link href="/forgot-password" className="text-sm text-primary hover:underline">
                  비밀번호 찾기
                </Link>
              </div>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  placeholder="비밀번호를 입력하세요"
                  value={formData.password}
                  onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                  required
                  className="bg-secondary pr-10"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <Checkbox
                id="remember"
                checked={formData.rememberMe}
                onCheckedChange={(checked) =>
                  setFormData({ ...formData, rememberMe: checked as boolean })
                }
              />
              <Label htmlFor="remember" className="text-sm font-normal">
                로그인 상태 유지
              </Label>
            </div>

            <Button
              type="submit"
              className="w-full bg-primary text-primary-foreground hover:bg-primary/90"
              disabled={isLoading}
            >
              {isLoading ? "로그인 중..." : "로그인"}
            </Button>

            {error && <p className="text-sm text-destructive">{error}</p>}
          </form>

          <div className="relative my-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-border" />
            </div>
            <div className="relative flex justify-center text-xs uppercase">
              <span className="bg-card px-2 text-muted-foreground">또는</span>
            </div>
          </div>

          <div className="space-y-3">
            <Button
              variant="outline"
              className="w-full gap-2"
              type="button"
              onClick={handleGithubLogin}
            >
              <Github className="h-4 w-4" />
              GitHub으로 로그인
            </Button>

            <Button
              variant="outline"
              className="w-full gap-2"
              type="button"
              onClick={handleGoogleLogin}
            >
              <svg className="h-4 w-4" viewBox="0 0 24 24">
                <path
                  fill="currentColor"
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                />
                <path
                  fill="currentColor"
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                />
                <path
                  fill="currentColor"
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                />
                <path
                  fill="currentColor"
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                />
              </svg>
              Google로 로그인
            </Button>

            <Button
              variant="outline"
              className="w-full gap-2 border-[#FEE500] bg-[#FEE500] text-[#000000] hover:bg-[#FEE500]/90"
              type="button"
              onClick={handleKakaoLogin}
            >
              <svg className="h-4 w-4" viewBox="0 0 24 24">
                <path
                  fill="currentColor"
                  d="M12 3C6.477 3 2 6.463 2 10.691c0 2.651 1.719 4.984 4.32 6.355-.144.521-.925 3.356-.959 3.578 0 0-.019.161.084.223.104.062.228.009.228.009.299-.043 3.467-2.265 4.009-2.648.759.106 1.542.162 2.318.162 5.523 0 10-3.463 10-7.691S17.523 3 12 3z"
                />
              </svg>
              카카오로 로그인
            </Button>
          </div>
        </div>

        <p className="mt-6 text-center text-sm text-muted-foreground">
          계정이 없으신가요?{" "}
          <Link href="/signup" className="text-primary hover:underline">
            회원가입
          </Link>
        </p>
      </div>
    </div>
  )
}