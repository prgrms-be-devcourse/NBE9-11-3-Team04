"use client"

import { useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { Code2, Eye, EyeOff, Check, X, Github } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Checkbox } from "@/components/ui/checkbox"
import { signup } from "@/lib/auth"

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

export default function SignUpPage() {
  const router = useRouter()
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState("")
  const [formData, setFormData] = useState({
    nickname: "",
    email: "",
    password: "",
    confirmPassword: "",
    agreeTerms: false,
  })

  const passwordRequirements = [
    { label: "최소 8자 이상", met: formData.password.length >= 8 },
    { label: "영문 포함", met: /[a-zA-Z]/.test(formData.password) },
    { label: "숫자 포함", met: /\d/.test(formData.password) },
    { label: "특수문자 포함", met: /[!@#$%^&*]/.test(formData.password) },
  ]

  const passwordsMatch =
    formData.password.length > 0 &&
    formData.confirmPassword.length > 0 &&
    formData.password === formData.confirmPassword

  const handleGithubSignup = () => {
    setError("")
    window.location.href = `${API_BASE_URL}/oauth2/authorization/github`
  }

  const handleGoogleSignup = () => {
    setError("")
    window.location.href = `${API_BASE_URL}/oauth2/authorization/google`
  }

  const handleKakaoSignup = () => {
    setError("")
    window.location.href = `${API_BASE_URL}/oauth2/authorization/kakao`
  }

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setError("")

    const nickname = formData.nickname.trim()
    const email = formData.email.trim()

    if (nickname.length < 2 || nickname.length > 50) {
      setError("닉네임은 2자 이상 50자 이하로 입력해주세요.")
      return
    }

    if (!passwordRequirements.every((item) => item.met)) {
      setError("비밀번호 조건을 모두 만족해주세요.")
      return
    }

    if (!passwordsMatch) {
      setError("비밀번호 확인이 일치하지 않습니다.")
      return
    }

    if (!formData.agreeTerms) {
      setError("이용약관 동의가 필요합니다.")
      return
    }

    setIsLoading(true)
    try {
      await signup({
        nickname,
        email,
        password: formData.password,
      })
      router.push("/login")
    } catch (err) {
      setError(err instanceof Error ? err.message : "회원가입에 실패했습니다.")
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="flex min-h-[calc(100vh-8rem)] items-center justify-center px-4 py-12">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <Link href="/" className="inline-flex items-center gap-2">
            <Code2 className="h-8 w-8 text-primary" />
            <span className="text-2xl font-bold text-foreground">DevC</span>
          </Link>
          <p className="mt-2 text-muted-foreground">새 계정을 만들어 시작하세요</p>
        </div>

        <div className="rounded-lg border border-border bg-card p-6">
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="nickname">닉네임</Label>
              <Input
                id="nickname"
                type="text"
                value={formData.nickname}
                onChange={(e) => setFormData({ ...formData, nickname: e.target.value })}
                required
                minLength={2}
                maxLength={50}
                className="bg-secondary"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="email">이메일</Label>
              <Input
                id="email"
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                required
                className="bg-secondary"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">비밀번호</Label>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
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

              <div className="grid grid-cols-1 gap-1 pt-2">
                {passwordRequirements.map((req) => (
                  <div key={req.label} className="flex items-center gap-2 text-xs">
                    {req.met ? (
                      <Check className="h-3.5 w-3.5 text-green-600" />
                    ) : (
                      <X className="h-3.5 w-3.5 text-muted-foreground" />
                    )}
                    <span className={req.met ? "text-green-600" : "text-muted-foreground"}>
                      {req.label}
                    </span>
                  </div>
                ))}
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="confirmPassword">비밀번호 확인</Label>
              <Input
                id="confirmPassword"
                type={showPassword ? "text" : "password"}
                value={formData.confirmPassword}
                onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
                required
                className="bg-secondary"
              />
              {formData.confirmPassword.length > 0 ? (
                <p className={`text-xs ${passwordsMatch ? "text-green-600" : "text-destructive"}`}>
                  {passwordsMatch ? "비밀번호가 일치합니다." : "비밀번호가 일치하지 않습니다."}
                </p>
              ) : null}
            </div>

            <div className="flex items-center gap-2">
              <Checkbox
                id="agreeTerms"
                checked={formData.agreeTerms}
                onCheckedChange={(checked) =>
                  setFormData({ ...formData, agreeTerms: checked as boolean })
                }
              />
              <Label htmlFor="agreeTerms" className="text-sm font-normal">
                이용약관 및 개인정보 처리방침에 동의합니다.
              </Label>
            </div>

            <Button
              type="submit"
              className="w-full bg-primary text-primary-foreground hover:bg-primary/90"
              disabled={isLoading}
            >
              {isLoading ? "가입 중..." : "회원가입"}
            </Button>

            {error ? <p className="text-sm text-destructive">{error}</p> : null}
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
            <Button variant="outline" className="w-full gap-2" type="button" onClick={handleGithubSignup}>
              <Github className="h-4 w-4" />
              GitHub로 가입
            </Button>

            <Button variant="outline" className="w-full gap-2" type="button" onClick={handleGoogleSignup}>
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
              Google로 가입
            </Button>

            <Button
              variant="outline"
              className="w-full gap-2 border-[#FEE500] bg-[#FEE500] text-[#000000] hover:bg-[#FEE500]/90"
              type="button"
              onClick={handleKakaoSignup}
            >
              카카오로 가입
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}
