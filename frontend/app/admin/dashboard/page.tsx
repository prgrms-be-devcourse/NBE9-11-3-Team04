"use client"

import { useEffect, useState } from "react"
import {
  Users,
  FileText,
  AlertTriangle,
  TrendingUp,
  Eye,
  MessageSquare,
} from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

// ==========================
// Type (API 응답 기준)
// ==========================
type DashboardData = {
  summary: {
    totalUsers: number
    totalPosts: number
    pendingReports: number
    todayVisitors: number
  }
  todayReports: {
    post: {
      total: number
      pending: number
      resolved: number
      byReason: { reason: string; count: number }[]
    }
    comment: {
      total: number
      pending: number
      resolved: number
      byReason: { reason: string; count: number }[]
    }
  }
  todayActivity: {
    newPosts: number
    newComments: number
    newUsers: number
  }
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

function getAuthHeaders(): HeadersInit {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  }

  if (typeof window !== "undefined") {
    const token = window.localStorage.getItem("accessToken")
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }
  }

  return headers
}


// ==========================
// Component
// ==========================
export default function AdminDashboardPage() {
  const [dashboard, setDashboard] = useState<DashboardData | null>(null)
  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const res = await fetch(
          `${API_BASE_URL}/api/admin/dashboard`,
          {
            method: "GET",
            headers: getAuthHeaders(),
          }
        )

        if (res.status === 401) {
          throw new Error("UNAUTHORIZED")
        }

        if (res.status === 403) {
          throw new Error("FORBIDDEN")
        }

        const json = await res.json()
        setDashboard(json.data)
      } catch (err) {
        console.error("dashboard fetch error", err)
      }
    }

    fetchDashboard()
  }, [])

  // ==========================
  // stats (API 기반)
  // ==========================
  const stats = dashboard
    ? [
      {
        name: "총 사용자",
        value: dashboard.summary.totalUsers.toLocaleString(),
        icon: Users,
        change: "+", // 필요하면 백엔드에서 추가 가능
        trend: "up",
      },
      {
        name: "총 게시글",
        value: dashboard.summary.totalPosts.toLocaleString(),
        icon: FileText,
        change: "+",
        trend: "up",
      },
      {
        name: "미처리 신고",
        value: dashboard.summary.pendingReports.toLocaleString(),
        icon: AlertTriangle,
        change: "-",
        trend: "down",
      },
      {
        name: "일일 방문자",
        value: dashboard.summary.todayVisitors.toLocaleString(),
        icon: Eye,
        change: "+",
        trend: "up",
      },
    ]
    : []

  const postReport = dashboard?.todayReports.post
  const commentReport = dashboard?.todayReports.comment
  const activity = dashboard?.todayActivity

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-foreground">대시보드</h1>
        <p className="text-muted-foreground">
          DevC 관리자 대시보드에 오신 것을 환영합니다.
        </p>
      </div>

      {/* Stats Grid */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((stat) => (
          <Card key={stat.name}>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                {stat.name}
              </CardTitle>
              <stat.icon className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-foreground">
                {stat.value}
              </div>
              <p className="text-xs text-muted-foreground">
                <span
                  className={
                    stat.trend === "up"
                      ? "text-green-500"
                      : "text-destructive"
                  }
                >
                  {stat.change}%
                </span>{" "}
                지난 달 대비
              </p>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Today Report Stats */}
      <div>
        <h2 className="mb-4 text-lg font-semibold text-foreground flex items-center gap-2">
          <AlertTriangle className="h-5 w-5 text-destructive" />
          오늘 신고 현황
        </h2>

        <div className="grid gap-4 md:grid-cols-2">
          {/* Post Report Card */}
          <Card>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <CardTitle className="flex items-center gap-2 text-base">
                  <FileText className="h-4 w-4 text-primary" />
                  게시글 신고
                </CardTitle>

                <div className="flex items-center gap-4 text-sm">
                  <span className="font-semibold text-foreground">
                    {postReport?.total ?? 0}건
                  </span>
                  <span className="text-amber-500">
                    대기 {postReport?.pending ?? 0}
                  </span>
                  <span className="text-green-500">
                    처리 {postReport?.resolved ?? 0}
                  </span>
                </div>
              </div>
            </CardHeader>

            <CardContent>
              <div className="space-y-2">
                {(postReport?.byReason ?? []).map((item) => (
                  <div
                    key={item.reason}
                    className="flex items-center justify-between rounded-md bg-secondary/50 px-3 py-2"
                  >
                    <span className="text-sm text-muted-foreground">
                      {item.reason}
                    </span>
                    <span className="text-sm font-medium text-foreground">
                      {item.count}
                    </span>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>

          {/* Comment Report Card */}
          <Card>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <CardTitle className="flex items-center gap-2 text-base">
                  <MessageSquare className="h-4 w-4 text-primary" />
                  댓글 신고
                </CardTitle>

                <div className="flex items-center gap-4 text-sm">
                  <span className="font-semibold text-foreground">
                    {commentReport?.total ?? 0}건
                  </span>
                  <span className="text-amber-500">
                    대기 {commentReport?.pending ?? 0}
                  </span>
                  <span className="text-green-500">
                    처리 {commentReport?.resolved ?? 0}
                  </span>
                </div>
              </div>
            </CardHeader>

            <CardContent>
              <div className="space-y-2">
                {(commentReport?.byReason ?? []).map((item) => (
                  <div
                    key={item.reason}
                    className="flex items-center justify-between rounded-md bg-secondary/50 px-3 py-2"
                  >
                    <span className="text-sm text-muted-foreground">
                      {item.reason}
                    </span>
                    <span className="text-sm font-medium text-foreground">
                      {item.count}
                    </span>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Today Activity */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <TrendingUp className="h-5 w-5 text-primary" />
            오늘의 활동
          </CardTitle>
        </CardHeader>

        <CardContent>
          <div className="grid gap-4 sm:grid-cols-3">
            <div className="rounded-lg bg-secondary p-4 text-center">
              <FileText className="mx-auto mb-2 h-6 w-6 text-primary" />
              <p className="text-2xl font-bold text-foreground">
                {activity?.newPosts ?? 0}
              </p>
              <p className="text-sm text-muted-foreground">새 게시글</p>
            </div>

            <div className="rounded-lg bg-secondary p-4 text-center">
              <MessageSquare className="mx-auto mb-2 h-6 w-6 text-primary" />
              <p className="text-2xl font-bold text-foreground">
                {activity?.newComments ?? 0}
              </p>
              <p className="text-sm text-muted-foreground">새 댓글</p>
            </div>

            <div className="rounded-lg bg-secondary p-4 text-center">
              <Users className="mx-auto mb-2 h-6 w-6 text-primary" />
              <p className="text-2xl font-bold text-foreground">
                {activity?.newUsers ?? 0}
              </p>
              <p className="text-sm text-muted-foreground">신규 가입</p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}