"use client"

import Link from "next/link"
import {useRouter} from "next/navigation"
import {useEffect, useState} from "react"
import {Bell, Search, User, Menu, X, PenSquare, Code2} from "lucide-react"
import {Button} from "@/components/ui/button"
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
    AUTH_CHANGED_EVENT,
    clearLoginSession,
    getAccessToken,
    getAuthSnapshot,
    persistLoginSession,
} from "@/lib/auth-storage"

const categories = [
    {name: "IT 기술 정보", href: "/category/tech"},
    {name: "취업 시장 정보", href: "/category/job-market"},
    {name: "개발 트렌드", href: "/category/trend"},
    {name: "자유 주제", href: "/category/free"},
]

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

type HeaderNotificationItem = {
    notificationId: number
    isRead?: boolean
    read?: boolean
}

type HeaderNotificationListResponse = {
    notifications: HeaderNotificationItem[]
}

type SuccessResponse<T> = {
    code?: string
    message?: string
    timestamp?: string
    data?: T
}

type MeResponse = {
    userId?: number
    email?: string
    nickname?: string
    role?: string
    status?: string
}

function isNotificationUnread(notification: HeaderNotificationItem) {
    return !(notification.isRead ?? notification.read ?? false)
}

export function Header() {
    const router = useRouter()
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
    const [hasUnreadNotifications, setHasUnreadNotifications] = useState(false)
    const [isLoggedIn, setIsLoggedIn] = useState(false)
    const [nickname, setNickname] = useState("")

    useEffect(() => {
        const syncAuthState = () => {
            const auth = getAuthSnapshot()
            setIsLoggedIn(auth.isLoggedIn)
            setNickname(auth.nickname ?? "")
        }

        const syncAuthFromServer = async () => {
            try {
                const res = await fetch(`${API_BASE_URL}/api/users/me`, {
                    method: "GET",
                    credentials: "include",
                    cache: "no-store",
                })

                if (res.ok) {
                    const body = (await res.json()) as SuccessResponse<MeResponse>
                    const me = body?.data

                    if (me?.email || me?.nickname) {
                        persistLoginSession(
                            undefined,
                            me?.nickname ?? null,
                            me?.email ?? null
                        )
                    }
                } else if (res.status === 401) {
                    clearLoginSession()
                }
            } catch {
                // 네트워크 오류 시에는 기존 local 상태 유지
            } finally {
                syncAuthState()
            }
        }

        syncAuthState()
        void syncAuthFromServer()

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

    const handleLogout = async () => {
        try {
            await fetch(`${API_BASE_URL}/api/auth/logout`, {
                method: "POST",
                credentials: "include",
            })
        } catch {
            // 로그아웃 API 실패여도 로컬 세션은 정리
        } finally {
            clearLoginSession()
            setMobileMenuOpen(false)
            router.push("/")
            router.refresh()
        }
    }

    useEffect(() => {
        let isMounted = true
        const loadNotifications = async () => {
            const auth = getAuthSnapshot()

            if (!auth.isLoggedIn) {
                if (isMounted) {
                    setHasUnreadNotifications(false)
                }
                return
            }
            try {
                const accessToken = getAccessToken()
                const headers = accessToken
                    ? {Authorization: `Bearer ${accessToken}`}
                    : undefined

                const response = await fetch(`${API_BASE_URL}/api/notifications`, {
                    cache: "no-store",
                    credentials: "include",
                    headers,
                })

                if (!response.ok) {
                    if (isMounted) {
                        setHasUnreadNotifications(false)
                    }
                    return
                }

                const body = (await response.json()) as SuccessResponse<HeaderNotificationListResponse>
                const notifications = body?.data?.notifications ?? []

                if (!isMounted) {
                    return
                }

                setHasUnreadNotifications(notifications.some(isNotificationUnread))
            } catch {
                if (isMounted) {
                    setHasUnreadNotifications(false)
                }
            }
        }

        const handleNotificationsUpdated = () => {
            if (isLoggedIn) {
                void loadNotifications()
            } else {
                setHasUnreadNotifications(false)
            }
        }

        if (isLoggedIn) {
            void loadNotifications()
        } else {
            setHasUnreadNotifications(false)
        }
        window.addEventListener("notifications-updated", handleNotificationsUpdated)

        return () => {
            isMounted = false
            window.removeEventListener("notifications-updated", handleNotificationsUpdated)
        }
    }, [isLoggedIn])

    return (
        <header
            className="sticky top-0 z-50 w-full border-b border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
            <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
                <Link href="/" className="flex items-center gap-2">
                    <Code2 className="h-7 w-7 text-primary"/>
                    <span className="text-xl font-bold text-foreground">DevC</span>
                </Link>

                <nav className="hidden items-center gap-6 md:flex">
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button variant="ghost" className="text-muted-foreground hover:text-foreground">
                                카테고리
                            </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="start" className="w-48">
                            {categories.map((category) => (
                                <DropdownMenuItem key={category.name} asChild>
                                    <Link href={category.href}>{category.name}</Link>
                                </DropdownMenuItem>
                            ))}
                        </DropdownMenuContent>
                    </DropdownMenu>

                    <Link href="/popular"
                          className="text-sm text-muted-foreground transition-colors hover:text-foreground">
                        인기글
                    </Link>
                    <Link href="/latest"
                          className="text-sm text-muted-foreground transition-colors hover:text-foreground">
                        최신글
                    </Link>
                    <Link href="/feed"
                          className="text-sm text-muted-foreground transition-colors hover:text-foreground">
                        북마크
                    </Link>
                </nav>

                <div className="flex items-center gap-2">
                    <Link href="/search">
                        <Button variant="ghost" size="icon" className="text-muted-foreground hover:text-foreground">
                            <Search className="h-5 w-5"/>
                            <span className="sr-only">검색</span>
                        </Button>
                    </Link>

                    <Link href={isLoggedIn ? "/notifications" : "/login"}>
                        <Button variant="ghost" size="icon"
                                className="relative text-muted-foreground hover:text-foreground">
                            <Bell className="h-5 w-5"/>
                            {hasUnreadNotifications ? (
                                <span className="absolute right-1 top-1 h-2 w-2 rounded-full bg-primary"/>
                            ) : null}
                            <span className="sr-only">알림</span>
                        </Button>
                    </Link>

                    <Link href={isLoggedIn ? "/write" : "/login"} className="hidden sm:block">
                        <Button className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90">
                            <PenSquare className="h-4 w-4"/>
                            글 쓰기
                        </Button>
                    </Link>

                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button variant="ghost" className="gap-2 text-muted-foreground hover:text-foreground">
                                <User className="h-5 w-5"/>
                                {isLoggedIn ? (
                                    <span className="max-w-24 truncate text-sm">{nickname || "사용자"}</span>
                                ) : null}
                                <span className="sr-only">마이페이지</span>
                            </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" className="w-48">
                            {isLoggedIn ? (
                                <>
                                    <DropdownMenuItem disabled className="font-medium">
                                        {nickname || "사용자"}
                                    </DropdownMenuItem>
                                    <DropdownMenuSeparator/>
                                </>
                            ) : null}

                            <DropdownMenuItem asChild>
                                <Link href={isLoggedIn ? "/mypage" : "/login"}>마이페이지</Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem asChild>
                                <Link href={isLoggedIn ? "/mypage/posts" : "/login"}>내 글 보기</Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem asChild>
                                <Link href={isLoggedIn ? "/mypage/edit" : "/login"}>프로필 수정</Link>
                            </DropdownMenuItem>
                            <DropdownMenuSeparator/>

                            {isLoggedIn ? (
                                <DropdownMenuItem onSelect={() => void handleLogout()}>로그아웃</DropdownMenuItem>
                            ) : (
                                <>
                                    <DropdownMenuItem asChild>
                                        <Link href="/login">로그인</Link>
                                    </DropdownMenuItem>
                                    <DropdownMenuItem asChild>
                                        <Link href="/signup">회원가입</Link>
                                    </DropdownMenuItem>
                                </>
                            )}
                        </DropdownMenuContent>
                    </DropdownMenu>

                    <Button
                        variant="ghost"
                        size="icon"
                        className="text-muted-foreground hover:text-foreground md:hidden"
                        onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                    >
                        {mobileMenuOpen ? <X className="h-5 w-5"/> : <Menu className="h-5 w-5"/>}
                        <span className="sr-only">메뉴</span>
                    </Button>
                </div>
            </div>

            {mobileMenuOpen && (
                <div className="border-t border-border bg-background md:hidden">
                    <div className="mx-auto max-w-7xl px-4 py-4 sm:px-6">
                        <nav className="flex flex-col gap-4">
                            <Link
                                href="/popular"
                                className="text-sm text-muted-foreground transition-colors hover:text-foreground"
                                onClick={() => setMobileMenuOpen(false)}
                            >
                                인기글
                            </Link>
                            <Link
                                href="/latest"
                                className="text-sm text-muted-foreground transition-colors hover:text-foreground"
                                onClick={() => setMobileMenuOpen(false)}
                            >
                                최신글
                            </Link>
                            <Link
                                href="/feed"
                                className="text-sm text-muted-foreground transition-colors hover:text-foreground"
                                onClick={() => setMobileMenuOpen(false)}
                            >
                                피드
                            </Link>

                            <div className="border-t border-border pt-4">
                                <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">카테고리</p>
                                <div className="flex flex-wrap gap-2">
                                    {categories.map((category) => (
                                        <Link
                                            key={category.name}
                                            href={category.href}
                                            className="rounded-md bg-secondary px-3 py-1.5 text-sm text-secondary-foreground transition-colors hover:bg-secondary/80"
                                            onClick={() => setMobileMenuOpen(false)}
                                        >
                                            {category.name}
                                        </Link>
                                    ))}
                                </div>
                            </div>

                            <Link href={isLoggedIn ? "/write" : "/login"} onClick={() => setMobileMenuOpen(false)}>
                                <Button className="w-full gap-2 bg-primary text-primary-foreground hover:bg-primary/90">
                                    <PenSquare className="h-4 w-4"/>
                                    글 쓰기
                                </Button>
                            </Link>

                            <div className="border-t border-border pt-4">
                                {isLoggedIn ? (
                                    <>
                                        <p className="mb-3 text-sm text-foreground">{nickname || "사용자"}님</p>
                                        <Button type="button" variant="outline" className="w-full"
                                                onClick={() => void handleLogout()}>
                                            로그아웃
                                        </Button>
                                    </>
                                ) : (
                                    <div className="flex gap-2">
                                        <Link href="/login" className="flex-1" onClick={() => setMobileMenuOpen(false)}>
                                            <Button type="button" variant="outline" className="w-full">
                                                로그인
                                            </Button>
                                        </Link>
                                        <Link href="/signup" className="flex-1"
                                              onClick={() => setMobileMenuOpen(false)}>
                                            <Button type="button" className="w-full">
                                                회원가입
                                            </Button>
                                        </Link>
                                    </div>
                                )}
                            </div>
                        </nav>
                    </div>
                </div>
            )}
        </header>
    )
}