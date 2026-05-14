"use client"

import { ChangeEvent, FormEvent, useEffect, useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import {
  Settings,
  Mail,
  User,
  ArrowLeft,
  MapPin,
  Link as LinkIcon,
  Github,
  Twitter,
  FileText,
} from "lucide-react"
import { apiFetch, isApiError } from "@/lib/api"
import {
  clearLoginSession,
  getCurrentUserProfile,
  persistLoginSession,
  saveCurrentUserProfile,
} from "@/lib/auth-storage"

type SuccessResponse<T> = {
  code: string
  message: string
  timestamp: string
  data: T
}

type MyProfileResponse = {
  userId: number
  email: string
  nickname: string
  role?: string
  status?: string
}

type UpdateMyProfileRequest = {
  nickname: string
}

type ProfileForm = {
  email: string
  nickname: string
}

type LocalProfileForm = {
  bio: string
  location: string
  website: string
  github: string
  twitter: string
}

export default function MyPageEditPage() {
  const router = useRouter()

  const [form, setForm] = useState<ProfileForm>({
    email: "",
    nickname: "",
  })

  const [localProfile, setLocalProfile] = useState<LocalProfileForm>({
    bio: "",
    location: "",
    website: "",
    github: "",
    twitter: "",
  })

  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState("")
  const [nicknameError, setNicknameError] = useState("")

  useEffect(() => {
    const fetchMyProfile = async () => {
      try {
        setLoading(true)
        setError("")

        const response = await apiFetch<SuccessResponse<MyProfileResponse>>("/api/mypage", {
          method: "GET",
          auth: true,
        })

        const profile = response?.data

        setForm({
          email: profile?.email ?? "",
          nickname: profile?.nickname ?? "",
        })

        persistLoginSession(
          undefined,
          profile?.nickname ?? "",
          profile?.email ?? ""
        )

        const savedProfile = getCurrentUserProfile()

        setLocalProfile({
          bio: savedProfile?.bio ?? "",
          location: savedProfile?.location ?? "",
          website: savedProfile?.website ?? "",
          github: savedProfile?.github ?? "",
          twitter: savedProfile?.twitter ?? "",
        })
      } catch (err) {
        if (isApiError(err) && err.isUnauthorized) {
          router.replace("/")
          return
        }

        console.error(err)
        setError(err instanceof Error ? err.message : "프로필 정보를 불러오지 못했습니다.")
      } finally {
        setLoading(false)
      }
    }

    fetchMyProfile()
  }, [router])

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target

    setForm((prev) => ({
      ...prev,
      [name]: value,
    }))

    if (name === "nickname") {
      setNicknameError("")
    }
  }

  const handleLocalProfileChange = (
    e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = e.target

    setLocalProfile((prev) => ({
      ...prev,
      [name]: value,
    }))
  }

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setNicknameError("")

    if (!form.nickname.trim()) {
      setNicknameError("닉네임을 입력해주세요.")
      return
    }

    try {
      setSubmitting(true)

      const requestBody: UpdateMyProfileRequest = {
        nickname: form.nickname.trim(),
      }

      const response = await apiFetch<SuccessResponse<MyProfileResponse>>("/api/mypage", {
        method: "PATCH",
        auth: true,
        body: JSON.stringify(requestBody),
      })

      const updatedProfile = response?.data

      if (!updatedProfile) {
        throw new Error("프로필 수정 응답이 올바르지 않습니다.")
      }

      persistLoginSession(
        undefined,
        updatedProfile.nickname,
        updatedProfile.email
      )

      saveCurrentUserProfile({
        email: updatedProfile.email,
        nickname: updatedProfile.nickname,
        username: updatedProfile.email.split("@")[0] || updatedProfile.nickname,
        bio: localProfile.bio,
        location: localProfile.location,
        website: localProfile.website,
        github: localProfile.github,
        twitter: localProfile.twitter,
      })

      setForm({
        email: updatedProfile.email,
        nickname: updatedProfile.nickname,
      })

      alert("프로필 수정이 완료되었습니다.")
      router.push("/mypage")
      router.refresh()
    } catch (err) {
      if (isApiError(err) && err.isUnauthorized) {
        router.replace("/")
        return
      }

      if (isApiError(err)) {
        if (err.code === "MYPAGE_409_NICKNAME_ALREADY_EXISTS" || err.message === "중복된 이름입니다.") {
          setNicknameError("중복된 이름입니다.")
          return
        }

        setNicknameError(err.message)
        return
      }

      console.error(err)
      alert(err instanceof Error ? err.message : "프로필 수정에 실패했습니다.")
    } finally {
      setSubmitting(false)
    }
  }

  const handleWithdraw = async () => {
    if (confirm("정말로 회원 탈퇴를 하시겠습니까?")) {
      try {
        await apiFetch("/api/users/me", {
          method: "DELETE",
          auth: true,
        })

        alert("회원 탈퇴가 완료되었습니다.")

        clearLoginSession()
        localStorage.removeItem("current_user_profile")
        window.location.replace("/")
      } catch (e) {
        if (isApiError(e) && e.isUnauthorized) {
          router.replace("/")
          return
        }

        console.error(e)
        alert(e instanceof Error ? e.message : "회원 탈퇴에 실패했습니다.")
      }
    }
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="rounded-lg border border-border bg-card p-12 text-center">
          <p className="text-sm text-muted-foreground">로딩 중...</p>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="rounded-lg border border-border bg-card p-12 text-center">
          <p className="text-sm text-red-500">{error}</p>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">프로필 수정</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            내 정보를 수정할 수 있습니다.
          </p>
        </div>

        <Link href="/mypage">
          <Button variant="outline" className="gap-2">
            <ArrowLeft className="h-4 w-4" />
            마이페이지로
          </Button>
        </Link>
      </div>

      <div className="rounded-lg border border-border bg-card p-6">
        <div className="flex flex-col items-start gap-6 sm:flex-row">
          <Avatar className="h-24 w-24 border-4 border-primary/20">
            <AvatarImage src="" alt={form.nickname || "사용자"} />
            <AvatarFallback className="bg-primary text-2xl text-primary-foreground">
              {(form.nickname || "사용자").slice(0, 2)}
            </AvatarFallback>
          </Avatar>

          <div className="flex-1">
            <div className="mb-4 flex flex-wrap items-center gap-4">
              <div>
                <h2 className="text-2xl font-bold text-foreground">
                  {form.nickname || "사용자"}
                </h2>
              </div>
              <Button variant="outline" className="gap-2" disabled>
                <Settings className="h-4 w-4" />
                프로필 편집 중
              </Button>
            </div>

            <p className="leading-relaxed text-muted-foreground">
              내 정보를 입력하세요
            </p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="mt-8 space-y-6 border-t border-border pt-6">
          <div className="grid gap-6 md:grid-cols-2">
            <div>
              <label
                htmlFor="email"
                className="mb-2 flex items-center gap-2 text-sm font-medium text-foreground"
              >
                <Mail className="h-4 w-4" />
                이메일
              </label>
              <input
                id="email"
                name="email"
                type="email"
                value={form.email}
                readOnly
                className="w-full cursor-not-allowed rounded-lg border border-border bg-muted px-3 py-2 text-sm text-muted-foreground outline-none"
              />
            </div>

            <div>
              <label
                htmlFor="nickname"
                className="mb-2 flex items-center gap-2 text-sm font-medium text-foreground"
              >
                <User className="h-4 w-4" />
                닉네임
              </label>
              <input
                id="nickname"
                name="nickname"
                type="text"
                value={form.nickname}
                onChange={handleChange}
                placeholder="닉네임을 입력하세요"
                className={`w-full rounded-lg border bg-background px-3 py-2 text-sm outline-none transition ${
                  nicknameError
                    ? "border-red-500 focus:border-red-500"
                    : "border-border focus:border-primary"
                }`}
              />
              {nicknameError ? (
                <p className="mt-2 text-sm text-red-500">{nicknameError}</p>
              ) : null}
            </div>
          </div>

          <div>
            <label
              htmlFor="bio"
              className="mb-2 flex items-center gap-2 text-sm font-medium text-foreground"
            >
              <FileText className="h-4 w-4" />
              소개
            </label>
            <textarea
              id="bio"
              name="bio"
              value={localProfile.bio}
              onChange={handleLocalProfileChange}
              placeholder="자기소개를 입력하세요"
              rows={4}
              className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none transition focus:border-primary"
            />
          </div>

          <div className="grid gap-6 md:grid-cols-2">
            <div>
              <label
                htmlFor="location"
                className="mb-2 flex items-center gap-2 text-sm font-medium text-foreground"
              >
                <MapPin className="h-4 w-4" />
                위치
              </label>
              <input
                id="location"
                name="location"
                type="text"
                value={localProfile.location}
                onChange={handleLocalProfileChange}
                placeholder="위치를 입력하세요"
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none transition focus:border-primary"
              />
            </div>

            <div>
              <label
                htmlFor="website"
                className="mb-2 flex items-center gap-2 text-sm font-medium text-foreground"
              >
                <LinkIcon className="h-4 w-4" />
                웹사이트
              </label>
              <input
                id="website"
                name="website"
                type="text"
                value={localProfile.website}
                onChange={handleLocalProfileChange}
                placeholder="웹사이트 주소를 입력하세요"
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none transition focus:border-primary"
              />
            </div>
          </div>

          <div className="grid gap-6 md:grid-cols-2">
            <div>
              <label
                htmlFor="github"
                className="mb-2 flex items-center gap-2 text-sm font-medium text-foreground"
              >
                <Github className="h-4 w-4" />
                GitHub
              </label>
              <input
                id="github"
                name="github"
                type="text"
                value={localProfile.github}
                onChange={handleLocalProfileChange}
                placeholder="GitHub 아이디 또는 링크를 입력하세요"
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none transition focus:border-primary"
              />
            </div>

            <div>
              <label
                htmlFor="twitter"
                className="mb-2 flex items-center gap-2 text-sm font-medium text-foreground"
              >
                <Twitter className="h-4 w-4" />
                Twitter / X
              </label>
              <input
                id="twitter"
                name="twitter"
                type="text"
                value={localProfile.twitter}
                onChange={handleLocalProfileChange}
                placeholder="Twitter/X 아이디 또는 링크를 입력하세요"
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none transition focus:border-primary"
              />
            </div>
          </div>

          <div className="flex gap-3 pt-2">
            <Button
              type="submit"
              disabled={submitting}
              className="bg-primary text-primary-foreground hover:bg-primary/90"
            >
              {submitting ? "수정 중..." : "저장"}
            </Button>

            <Button type="button" variant="outline" onClick={() => router.push("/mypage")}>
              취소
            </Button>

            <Button type="button" variant="destructive" onClick={handleWithdraw}>
              회원 탈퇴
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}