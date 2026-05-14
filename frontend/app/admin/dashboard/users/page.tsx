"use client"

import { useEffect, useMemo, useState } from "react"
import { Eye } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "")

/* =========================
   TYPES
========================= */

type UserStatus = "ACTIVE" | "WARNED" | "SUSPENDED" | "BLACKLISTED"

type ActionType = "warn" | "suspend" | "blacklist" | "activate"

interface User {
  userId: number
  nickname: string
  email: string
  createdAt: string
  postCount: number
  commentCount: number
  status: UserStatus
  suspendedUntil?: string
}

/* =========================
   STATUS LABELS
========================= */

const statusLabels: Record<string, { label: string; className: string }> = {
  ACTIVE: {
    label: "활성",
    className: "bg-green-500/10 text-green-500",
  },
  WARNED: {
    label: "경고",
    className: "bg-yellow-500/10 text-yellow-500",
  },
  SUSPENDED: {
    label: "정지",
    className: "bg-orange-500/10 text-orange-500",
  },
  BLACKLISTED: {
    label: "차단",
    className: "bg-red-500/10 text-red-500",
  },
}

/* =========================
   API
========================= */

function getAuthHeaders(): HeadersInit {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  }

  if (typeof window !== "undefined") {
    const token = localStorage.getItem("accessToken")
    if (token) headers.Authorization = `Bearer ${token}`
  }

  return headers
}

async function fetchUsers(): Promise<User[]> {
  const res = await fetch(`${API_BASE}/api/admin/members`, {
    headers: getAuthHeaders(),
  })

  if (!res.ok) throw new Error("사용자 조회 실패")

  const json = await res.json()
  return json.data.content
}

async function updateUserStatus(params: {
  userId: number
  status: UserStatus
  days?: number
}) {
  const res = await fetch(
    `${API_BASE}/api/admin/members/${params.userId}/status`,
    {
      method: "PATCH",
      headers: getAuthHeaders(),
      body: JSON.stringify({
        status: params.status,
        days: params.days ?? null,
      }),
    }
  )

  if (!res.ok) throw new Error("상태 변경 실패")

  return res.json()
}

/* =========================
   PAGE
========================= */

export default function UsersManagementPage() {
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [selectedUser, setSelectedUser] = useState<User | null>(null)

  const [actionDialog, setActionDialog] = useState<{
    open: boolean
    type: ActionType | null
    user: User | null
  }>({ open: false, type: null, user: null })

  const [duration, setDuration] = useState<string>("")
  const [reason, setReason] = useState("")

  // 상태 필터 탭
  const [filterStatus, setFilterStatus] = useState<UserStatus | "ALL">("ALL")

  // 알림 팝업 (alert 대체)
  const [popup, setPopup] = useState<{
    open: boolean
    title: string
    message: string
  }>({
    open: false,
    title: "안내",
    message: "",
  })

  const openPopup = (title: string, message: string) => {
    setPopup({
      open: true,
      title,
      message,
    })
  }

  const closePopup = () => {
    setPopup({
      open: false,
      title: "안내",
      message: "",
    })
  }

  /* =========================
     LOAD
  ========================= */

  const loadUsers = async () => {
    try {
      setLoading(true)
      setError(null)

      const data = await fetchUsers()
      setUsers(data)
    } catch (e: any) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadUsers()
  }, [])

  /* =========================
     FILTERED USERS
  ========================= */

  const filteredUsers = useMemo(() => {
    if (filterStatus === "ALL") return users
    return users.filter((u) => u.status === filterStatus)
  }, [users, filterStatus])

  /* =========================
     ACTION
  ========================= */

  const handleAction = (type: ActionType, user: User) => {
    setActionDialog({ open: true, type, user })
    setDuration("")
    setReason("")
  }

  const confirmAction = async () => {
    if (!actionDialog.user || !actionDialog.type) return

    const userId = actionDialog.user.userId

    const newStatus: UserStatus =
      actionDialog.type === "activate"
        ? "ACTIVE"
        : actionDialog.type === "warn"
          ? "WARNED"
          : actionDialog.type === "suspend"
            ? "SUSPENDED"
            : "BLACKLISTED"

    // 정지인데 기간 없으면 막기
    if (actionDialog.type === "suspend" && !duration) {
      openPopup("입력 오류", "정지 기간을 선택해야 합니다.")
      return
    }

    try {
      await updateUserStatus({
        userId,
        status: newStatus,
        days: actionDialog.type === "suspend" ? Number(duration) : undefined,
      })

      await loadUsers()

      // 다이얼로그 닫기
      setSelectedUser(null)
      setActionDialog({ open: false, type: null, user: null })

      // 성공 메시지
      const statusLabel = statusLabels[newStatus]?.label ?? "상태"
      openPopup("처리 완료", `유저 상태 변경 완료.`)
    } catch (e: any) {
      openPopup("처리 실패", e?.message ?? "처리 중 오류가 발생했습니다.")
    }
  }

  /* =========================
     UI
  ========================= */

  if (loading) return <div className="p-6">로딩 중...</div>
  if (error) return <div className="p-6 text-red-500">{error}</div>

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">사용자 관리</h1>

      {/* 상태별 필터 탭 */}
      <Tabs
        value={filterStatus}
        onValueChange={(v) => setFilterStatus(v as UserStatus | "ALL")}
      >
        <TabsList>
          <TabsTrigger value="ALL">전체</TabsTrigger>
          <TabsTrigger value="ACTIVE">활성</TabsTrigger>
          <TabsTrigger value="WARNED">경고</TabsTrigger>
          <TabsTrigger value="SUSPENDED">정지</TabsTrigger>
          <TabsTrigger value="BLACKLISTED">차단</TabsTrigger>
        </TabsList>

        <TabsContent value={filterStatus}>
          {/* TABLE */}
          <div className="border rounded-lg overflow-hidden">
            <table className="w-full">
              <thead className="bg-secondary">
                <tr>
                  <th className="p-3 text-left">사용자</th>
                  <th className="p-3 text-left">활동</th>
                  <th className="p-3 text-left">상태</th>
                  <th className="p-3 text-right">작업</th>
                </tr>
              </thead>

              <tbody>
                {filteredUsers.map((user) => (
                  <tr key={user.userId} className="border-t">
                    <td className="p-3">
                      <div>
                        <p className="font-medium">{user.nickname}</p>
                        <p className="text-xs text-muted-foreground">
                          {user.email}
                        </p>
                      </div>
                    </td>

                    <td className="p-3 text-sm">
                      게시글 {user.postCount} / 댓글 {user.commentCount}
                    </td>

                    <td className="p-3">
                      <span
                        className={`px-2 py-1 text-xs rounded ${statusLabels[user.status]?.className ?? ""
                          }`}
                      >
                        {statusLabels[user.status]?.label ?? "UNKNOWN"}
                      </span>
                    </td>

                    {/* ACTION BUTTONS */}
                    <td className="p-3 text-right flex gap-2 justify-end">
                      <Button
                        size="sm"
                        variant="outline"
                        className={`${statusLabels.ACTIVE.className} border-green-500/20 hover:bg-green-500/20`}
                        onClick={() => handleAction("activate", user)}
                      >
                        제재 해제
                      </Button>

                      <Button
                        size="sm"
                        variant="outline"
                        className={`${statusLabels.WARNED.className} border-yellow-500/20 hover:bg-yellow-500/20`}
                        onClick={() => handleAction("warn", user)}
                      >
                        경고
                      </Button>

                      <Button
                        size="sm"
                        variant="outline"
                        className={`${statusLabels.SUSPENDED.className} border-orange-500/20 hover:bg-orange-500/20`}
                        onClick={() => handleAction("suspend", user)}
                      >
                        정지
                      </Button>

                      <Button
                        size="sm"
                        variant="destructive"
                        className={`${statusLabels.BLACKLISTED.className} border-red-500/20 hover:bg-red-500/20`}
                        onClick={() => handleAction("blacklist", user)}
                      >
                        차단
                      </Button>

                      <Button
                        size="icon"
                        variant="ghost"
                        onClick={() => setSelectedUser(user)}
                      >
                        <Eye className="w-4 h-4" />
                      </Button>
                    </td>
                  </tr>
                ))}

                {filteredUsers.length === 0 && (
                  <tr>
                    <td
                      colSpan={4}
                      className="p-6 text-center text-muted-foreground text-sm"
                    >
                      해당 상태의 사용자가 없습니다.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </TabsContent>
      </Tabs>

      {/* =========================
          ACTION DIALOG
      ========================= */}

      <Dialog
        open={actionDialog.open}
        onOpenChange={(open) =>
          !open && setActionDialog({ open: false, type: null, user: null })
        }
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>사용자 처리</DialogTitle>
            <DialogDescription>
              사용자 상태 변경 및 제재 처리를 수행합니다.
            </DialogDescription>
          </DialogHeader>

          {actionDialog.type === "suspend" && (
            <div className="space-y-2">
              <Select value={duration} onValueChange={setDuration}>
                <SelectTrigger>
                  <SelectValue placeholder="정지 기간 선택" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="1">1일</SelectItem>
                  <SelectItem value="3">3일</SelectItem>
                  <SelectItem value="5">5일</SelectItem>
                  <SelectItem value="7">7일</SelectItem>
                </SelectContent>
              </Select>
            </div>
          )}

          <Textarea
            placeholder="사유 입력"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
          />

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() =>
                setActionDialog({
                  open: false,
                  type: null,
                  user: null,
                })
              }
            >
              취소
            </Button>

            <Button onClick={confirmAction}>확인</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* =========================
          DETAIL DIALOG
      ========================= */}

      <Dialog open={!!selectedUser} onOpenChange={() => setSelectedUser(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="border-b pb-3">
              사용자 상세 정보
            </DialogTitle>
          </DialogHeader>

          {selectedUser && (
            <div className="space-y-4 pt-4">
              <div className="grid grid-cols-3 gap-2 text-sm">
                <span className="text-muted-foreground font-medium">
                  닉네임:
                </span>
                <span className="col-span-2">{selectedUser.nickname}</span>

                <span className="text-muted-foreground font-medium">
                  이메일:
                </span>
                <span className="col-span-2">{selectedUser.email}</span>

                <span className="text-muted-foreground font-medium">
                  활동량:
                </span>
                <span className="col-span-2">
                  게시글 {selectedUser.postCount}개 / 댓글{" "}
                  {selectedUser.commentCount}개
                </span>

                <span className="text-muted-foreground font-medium">상태:</span>
                <div className="col-span-2">
                  <span
                    className={`px-2 py-0.5 rounded text-xs ${statusLabels[selectedUser.status]?.className
                      }`}
                  >
                    {statusLabels[selectedUser.status]?.label}
                  </span>
                </div>

                <span className="text-muted-foreground font-medium">
                  가입일:
                </span>
                <span className="col-span-2">
                  {new Date(selectedUser.createdAt).toLocaleDateString()}
                </span>

                {selectedUser.status === "SUSPENDED" &&
                  selectedUser.suspendedUntil && (
                    <>
                      <span className="text-red-500 font-medium">
                        제재 종료일:
                      </span>
                      <span className="col-span-2 text-red-500">
                        {new Date(selectedUser.suspendedUntil).toLocaleString()}
                      </span>
                    </>
                  )}
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* =========================
          POPUP DIALOG (alert 대체)
      ========================= */}

      <Dialog open={popup.open} onOpenChange={closePopup}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{popup.title}</DialogTitle>
          </DialogHeader>

          <p className="text-sm text-muted-foreground">{popup.message}</p>

          <DialogFooter>
            <Button onClick={closePopup}>확인</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}