"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import { Code2, Github, Twitter } from "lucide-react"
import { AUTH_CHANGED_EVENT, clearLoginSession, getAuthSnapshot } from "@/lib/auth-storage"

export function Footer() {
  const [isLoggedIn, setIsLoggedIn] = useState(false)

  useEffect(() => {
    const syncAuthState = () => {
      setIsLoggedIn(getAuthSnapshot().isLoggedIn)
    }

    syncAuthState()

    const handleStorage = () => {
      syncAuthState()
    }

    window.addEventListener("storage", handleStorage)
    window.addEventListener(AUTH_CHANGED_EVENT, handleStorage as EventListener)

    return () => {
      window.removeEventListener("storage", handleStorage)
      window.removeEventListener(AUTH_CHANGED_EVENT, handleStorage as EventListener)
    }
  }, [])

  const handleLogout = () => {
    clearLoginSession()
  }

  return (
    <footer className="border-t border-border bg-background">
      <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="grid gap-8 md:grid-cols-4">
          <div className="md:col-span-2">
            <Link href="/" className="flex items-center gap-2">
              <Code2 className="h-6 w-6 text-primary" />
              <span className="text-lg font-bold text-foreground">DevC</span>
            </Link>
            <p className="mt-4 max-w-md text-sm leading-relaxed text-muted-foreground">
              개발자들을 위한 지식 공유 커뮤니티입니다. 최신 기술 트렌드, 실무 경험, 다양한 개발 이야기를 함께 나눠보세요.
            </p>
            <div className="mt-4 flex gap-4">
              <a
                href="https://github.com"
                target="_blank"
                rel="noopener noreferrer"
                className="text-muted-foreground transition-colors hover:text-foreground"
              >
                <Github className="h-5 w-5" />
                <span className="sr-only">GitHub</span>
              </a>
              <a
                href="https://twitter.com"
                target="_blank"
                rel="noopener noreferrer"
                className="text-muted-foreground transition-colors hover:text-foreground"
              >
                <Twitter className="h-5 w-5" />
                <span className="sr-only">Twitter</span>
              </a>
            </div>
          </div>

          <div>
            <h3 className="mb-4 text-sm font-semibold text-foreground">커뮤니티</h3>
            <ul className="flex flex-col gap-3">
              <li>
                <Link href="/popular" className="text-sm text-muted-foreground transition-colors hover:text-foreground">
                  인기글
                </Link>
              </li>
              <li>
                <Link href="/latest" className="text-sm text-muted-foreground transition-colors hover:text-foreground">
                  최신글
                </Link>
              </li>
              <li>
                <Link href="/feed" className="text-sm text-muted-foreground transition-colors hover:text-foreground">
                  피드
                </Link>
              </li>
              <li>
                <Link href="/category/javascript" className="text-sm text-muted-foreground transition-colors hover:text-foreground">
                  카테고리
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h3 className="mb-4 text-sm font-semibold text-foreground">계정</h3>
            <ul className="flex flex-col gap-3">
              {isLoggedIn ? (
                <li>
                  <button
                    type="button"
                    onClick={handleLogout}
                    className="text-sm text-muted-foreground transition-colors hover:text-foreground"
                  >
                    로그아웃
                  </button>
                </li>
              ) : (
                <>
                  <li>
                    <Link href="/login" className="text-sm text-muted-foreground transition-colors hover:text-foreground">
                      로그인
                    </Link>
                  </li>
                  <li>
                    <Link href="/signup" className="text-sm text-muted-foreground transition-colors hover:text-foreground">
                      회원가입
                    </Link>
                  </li>
                </>
              )}
              <li>
                <Link href="/mypage" className="text-sm text-muted-foreground transition-colors hover:text-foreground">
                  마이페이지
                </Link>
              </li>
            </ul>
          </div>
        </div>

        <div className="mt-12 border-t border-border pt-8">
          <p className="text-center text-sm text-muted-foreground">
            &copy; {new Date().getFullYear()} DevC. All rights reserved.
          </p>
        </div>
      </div>
    </footer>
  )
}
