"use client"

import { useEffect, useState } from "react"
import { Eye, MoreHorizontal } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  Dialog,
  DialogContent,
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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Label } from "@/components/ui/label"

/* =========================
   API CONFIG
========================= */

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "")

/* =========================
   TYPES
========================= */

type ReportStatus = "PENDING" | "RESOLVED" | "REJECTED"
type SanctionType = "WARNED" | "SUSPENDED" | "BLACKLISTED"

interface ReportGroup {
  targetType: "POST" | "COMMENT"
  targetId: number
  targetNickname: string
  targetTitle?: string
  targetContent?: string
  reportCount: number
  reasonTypes: string[]
  status: ReportStatus
  latestCreatedAt: string
}

/* =========================
   LABELS
========================= */

const typeLabels: Record<string, string> = {
  POST: "게시글",
  COMMENT: "댓글",
}

const statusLabels: Record<string, { label: string; className: string }> = {
  PENDING: {
    label: "대기",
    className: "bg-gray-500/10 text-gray-500 px-2 py-1 rounded-md text-xs",
  },
  RESOLVED: {
    label: "완료",
    className: "bg-green-500/10 text-green-500 px-2 py-1 rounded-md text-xs",
  },
  REJECTED: {
    label: "반려",
    className: "bg-red-500/10 text-red-500 px-2 py-1 rounded-md text-xs",
  },
}

/* =========================
   API
========================= */

async function handleApiError(res: Response) {
  let errorMessage = "서버 오류가 발생했습니다."

  try {
    const json = await res.json()

    if (json?.message) errorMessage = json.message
    else if (json?.error?.message) errorMessage = json.error.message
    else if (json?.data?.message) errorMessage = json.data.message
  } catch {
    const text = await res.text()
    if (text) errorMessage = text
  }

  throw new Error(errorMessage)
}

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

async function fetchGroupedReports(status: ReportStatus): Promise<ReportGroup[]> {
  const res = await fetch(
    `${API_BASE}/api/admin/reports/groups?status=${status}&page=0&size=10`,
    {
      method: "GET",
      headers: getAuthHeaders(),
    }
  )

  if (!res.ok) {
    await handleApiError(res)
  }

  const json = await res.json()
  return json.data.content
}

async function processReportGroup(params: {
  targetId: number
  targetType: "POST" | "COMMENT"
  action: "RESOLVE" | "REJECT"
  adminNote: string
  sanctionType?: SanctionType
  suspensionDays?: number
}) {
  const endpoint =
    params.action === "RESOLVE"
      ? `${API_BASE}/api/admin/reports/groups/approve`
      : `${API_BASE}/api/admin/reports/groups/reject`

  const res = await fetch(endpoint, {
    method: "POST",
    headers: getAuthHeaders(),
    body: JSON.stringify({
      reportId: params.targetId,
      targetType: params.targetType,
      adminNote: params.adminNote,
      sanctionType: params.sanctionType ?? null,
      suspensionDays: params.suspensionDays ?? null,
    }),
  })

  if (!res.ok) {
    await handleApiError(res)
  }

  const text = await res.text()
  return text ? JSON.parse(text) : null
}

/* =========================
   PAGE
========================= */

export default function ReportsManagementPage() {
  const [groups, setGroups] = useState<ReportGroup[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [status, setStatus] = useState<ReportStatus>("PENDING")
  const [selectedGroup, setSelectedGroup] = useState<ReportGroup | null>(null)

  const [dialog, setDialog] = useState<{
    open: boolean
    group: ReportGroup | null
    action: "RESOLVE" | "REJECT" | null
  }>({ open: false, group: null, action: null })

  const [adminNote, setAdminNote] = useState("")
  const [sanctionType, setSanctionType] = useState<SanctionType | "">("")
  const [suspensionDays, setSuspensionDays] = useState("")

  // ✅ alert 대신 사용할 에러 팝업 상태
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

  const loadGroups = async (s: ReportStatus) => {
    try {
      setLoading(true)
      setError(null)

      const data = await fetchGroupedReports(s)
      setGroups(data)
    } catch (e: any) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadGroups(status)
  }, [status])

  const openDialog = (group: ReportGroup, action: "RESOLVE" | "REJECT") => {
    setDialog({ open: true, group, action })
    setAdminNote("")
    setSanctionType("")
    setSuspensionDays("")
  }

  const closeDialog = () => {
    setDialog({ open: false, group: null, action: null })
  }

  const handleProcess = async () => {
    if (!dialog.group || !dialog.action) return

    try {
      await processReportGroup({
        targetId: dialog.group.targetId,
        targetType: dialog.group.targetType,
        action: dialog.action,
        adminNote,
        sanctionType: sanctionType || undefined,
        suspensionDays: suspensionDays ? Number(suspensionDays) : undefined,
      })

      closeDialog()
      await loadGroups(status)

      openPopup("처리 완료", "신고 처리가 완료되었습니다.")
    } catch (e: any) {
      openPopup("처리 실패", e.message ?? "처리 중 오류가 발생했습니다.")
    }
  }

  const GroupTable = ({ data }: { data: ReportGroup[] }) => (
    <div className="border rounded-lg overflow-hidden">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>유형</TableHead>
            <TableHead>대상</TableHead>
            <TableHead>신고 수</TableHead>
            <TableHead>사유</TableHead>
            <TableHead>피신고자</TableHead>
            <TableHead>상태</TableHead>
            <TableHead>작업</TableHead>
          </TableRow>
        </TableHeader>

        <TableBody>
          {data.map((g) => {
            const label = statusLabels[g.status]

            return (
              <TableRow key={g.targetType + g.targetId}>
                <TableCell>{typeLabels[g.targetType] ?? "UNKNOWN"}</TableCell>

                <TableCell className="max-w-[300px] truncate">
                  {g.targetTitle || g.targetContent}
                </TableCell>

                <TableCell>{g.reportCount}</TableCell>

                <TableCell className="truncate">
                  {g.reasonTypes.join(", ")}
                </TableCell>

                <TableCell>{g.targetNickname}</TableCell>

                <TableCell>
                  <span className={label?.className ?? ""}>
                    {label?.label ?? "UNKNOWN"}
                  </span>
                </TableCell>

                <TableCell className="flex gap-1">
                  <Button
                    size="icon"
                    variant="ghost"
                    onClick={() => setSelectedGroup(g)}
                  >
                    <Eye />
                  </Button>

                  {g.status === "PENDING" && (
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button size="icon" variant="ghost">
                          <MoreHorizontal />
                        </Button>
                      </DropdownMenuTrigger>

                      <DropdownMenuContent>
                        <DropdownMenuItem onClick={() => openDialog(g, "RESOLVE")}>
                          승인 처리
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => openDialog(g, "REJECT")}>
                          반려
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  )}
                </TableCell>
              </TableRow>
            )
          })}
        </TableBody>
      </Table>
    </div>
  )

  if (loading) return <div className="p-6">로딩중...</div>
  if (error) return <div className="p-6 text-red-500">{error}</div>

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">신고 관리</h1>

      <Tabs value={status} onValueChange={(v) => setStatus(v as ReportStatus)}>
        <TabsList>
          <TabsTrigger value="PENDING">대기</TabsTrigger>
          <TabsTrigger value="RESOLVED">완료</TabsTrigger>
          <TabsTrigger value="REJECTED">반려</TabsTrigger>
        </TabsList>

        <TabsContent value={status}>
          <GroupTable data={groups} />
        </TabsContent>
      </Tabs>

      {/* 처리 모달 */}
      <Dialog open={dialog.open} onOpenChange={closeDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {dialog.action === "RESOLVE" ? "승인 처리" : "반려 처리"}
            </DialogTitle>
          </DialogHeader>

          <div className="space-y-3">
            {dialog.action === "RESOLVE" && (
              <>
                <div>
                  <Label>제재 유형</Label>
                  <Select
                    value={sanctionType}
                    onValueChange={(v) => setSanctionType(v as SanctionType)}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="선택" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="WARNED">경고</SelectItem>
                      <SelectItem value="SUSPENDED">정지</SelectItem>
                      <SelectItem value="BLACKLISTED">차단</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                {sanctionType === "SUSPENDED" && (
                  <div>
                    <Label>기간</Label>
                    <Select
                      value={suspensionDays}
                      onValueChange={setSuspensionDays}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="선택" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="1">1일</SelectItem>
                        <SelectItem value="7">7일</SelectItem>
                        <SelectItem value="30">30일</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                )}
              </>
            )}

            <Textarea
              value={adminNote}
              onChange={(e) => setAdminNote(e.target.value)}
              placeholder="메모"
            />
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={closeDialog}>
              취소
            </Button>
            <Button onClick={handleProcess}>확인</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 상세 */}
      <Dialog open={!!selectedGroup} onOpenChange={() => setSelectedGroup(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>상세</DialogTitle>
          </DialogHeader>

          <div className="space-y-2 text-sm">
            <div>
              <span className="font-semibold">피신고자:</span>{" "}
              {selectedGroup?.targetNickname}
            </div>
            <div>
              <span className="font-semibold">신고 수:</span>{" "}
              {selectedGroup?.reportCount}
            </div>
            <div>
              <span className="font-semibold">사유:</span>{" "}
              {selectedGroup?.reasonTypes?.join(", ")}
            </div>
            <div>
              <span className="font-semibold">상태:</span>{" "}
              {selectedGroup
                ? statusLabels[selectedGroup.status]?.label ?? "UNKNOWN"
                : ""}
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* ✅ 경고/에러 알림 모달 */}
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