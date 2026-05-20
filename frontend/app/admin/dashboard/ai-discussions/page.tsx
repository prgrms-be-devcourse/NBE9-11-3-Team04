"use client"

import { useEffect, useMemo, useState } from "react"
import { Bot, CheckCircle, Eye, Loader2, RefreshCw, XCircle } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Textarea } from "@/components/ui/textarea"

type AiDiscussionStatus = "PENDING" | "APPROVED" | "REJECTED"

interface AiDiscussionPostResponse {
  id: number
  title: string
  content: string
  status: AiDiscussionStatus
  rejectionReason: string | null
  approvedPostId: number | null
  createdAt: string | null
}

interface PageResponse<T> {
  content: T[]
  totalElements?: number
  totalPages?: number
  number?: number
  size?: number
}

const statusLabels: Record<AiDiscussionStatus, string> = {
  PENDING: "승인 대기",
  APPROVED: "승인 완료",
  REJECTED: "거절",
}

const statusStyles: Record<AiDiscussionStatus, string> = {
  PENDING: "bg-yellow-100 text-yellow-800 hover:bg-yellow-100",
  APPROVED: "bg-green-100 text-green-800 hover:bg-green-100",
  REJECTED: "bg-red-100 text-red-800 hover:bg-red-100",
}

const API_BASE = (
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://43.200.4.180.nip.io:8080"
).replace(/\/$/, "")

function getAuthHeaders(): HeadersInit {
  const headers: HeadersInit = {
    "Content-Type": "application/json",
  }

  const token = localStorage.getItem("accessToken")
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  return headers
}

export default function AiDiscussionAdminPage() {
  const [status, setStatus] = useState<AiDiscussionStatus>("PENDING")
  const [posts, setPosts] = useState<AiDiscussionPostResponse[]>([])
  const [selectedPost, setSelectedPost] = useState<AiDiscussionPostResponse | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [isDetailOpen, setIsDetailOpen] = useState(false)
  const [isRejectOpen, setIsRejectOpen] = useState(false)
  const [rejectReason, setRejectReason] = useState("")
  const [errorMessage, setErrorMessage] = useState("")
  const [successMessage, setSuccessMessage] = useState("")
  const [keyword, setKeyword] = useState("")
  const [isSubmitting, setIsSubmitting] = useState(false)

  const filteredPosts = useMemo(() => {
    const trimmedKeyword = keyword.trim().toLowerCase()

    if (!trimmedKeyword) {
      return posts
    }

    return posts.filter((post) => {
      return (
        post.title.toLowerCase().includes(trimmedKeyword) ||
        post.content.toLowerCase().includes(trimmedKeyword)
      )
    })
  }, [keyword, posts])

  const fetchDiscussions = async (nextStatus: AiDiscussionStatus = status) => {
    setIsLoading(true)
    setErrorMessage("")

    try {
      const response = await fetch(
        `${API_BASE}/api/admin/ai-discussions?status=${nextStatus}&page=0&size=20`,
        {
          headers: getAuthHeaders(),
          credentials: "include",
        },
      )

      if (!response.ok) {
        throw new Error("AI 토론 주제 목록을 불러오지 못했습니다.")
      }

      const data = await response.json()
      const discussions = extractDiscussionList(data)
      setPosts(discussions)
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "알 수 없는 오류가 발생했습니다.")
    } finally {
      setIsLoading(false)
    }
  }

  const handleStatusChange = (nextStatus: AiDiscussionStatus) => {
    setStatus(nextStatus)
    setSelectedPost(null)
    setKeyword("")
    void fetchDiscussions(nextStatus)
  }

  const openDetail = (post: AiDiscussionPostResponse) => {
    setSelectedPost(post)
    setIsDetailOpen(true)
  }

  const approveDiscussion = async (post: AiDiscussionPostResponse) => {
    setIsSubmitting(true)
    setErrorMessage("")
    setSuccessMessage("")

    try {
      const response = await fetch(`${API_BASE}/api/admin/ai-discussions/${post.id}/approve`, {
        method: "PATCH",
        headers: getAuthHeaders(),
        credentials: "include",
      })

      if (!response.ok) {
        throw new Error("AI 토론 주제 승인에 실패했습니다.")
      }

      setSuccessMessage("AI 토론 주제를 승인했습니다.")
      setIsDetailOpen(false)
      setSelectedPost(null)
      await fetchDiscussions(status)
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "알 수 없는 오류가 발생했습니다.")
    } finally {
      setIsSubmitting(false)
    }
  }

  const rejectDiscussion = async () => {
    if (!selectedPost) {
      return
    }

    setIsSubmitting(true)
    setErrorMessage("")
    setSuccessMessage("")

    try {
      const params = new URLSearchParams()
      if (rejectReason.trim()) {
        params.set("reason", rejectReason.trim())
      }

      const response = await fetch(
        `${API_BASE}/api/admin/ai-discussions/${selectedPost.id}/reject${params.toString() ? `?${params.toString()}` : ""}`,
        {
          method: "PATCH",
          headers: getAuthHeaders(),
          credentials: "include",
        },
      )

      if (!response.ok) {
        throw new Error("AI 토론 주제 거절에 실패했습니다.")
      }

      setSuccessMessage("AI 토론 주제를 거절했습니다.")
      setRejectReason("")
      setIsRejectOpen(false)
      setIsDetailOpen(false)
      setSelectedPost(null)
      await fetchDiscussions(status)
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "알 수 없는 오류가 발생했습니다.")
    } finally {
      setIsSubmitting(false)
    }
  }

  useEffect(() => {
    void fetchDiscussions(status)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div className="space-y-6 text-white">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <Bot className="h-6 w-6 text-teal-400" />
            <h1 className="text-2xl font-bold text-white">AI 토론 관리</h1>
          </div>
          <p className="mt-1 text-sm text-zinc-400">
            AI가 생성한 개발 토론 주제를 검토하고 승인 또는 거절합니다.
          </p>
        </div>

        <Button
          variant="outline"
          className="border-zinc-800 bg-transparent text-zinc-200 hover:bg-zinc-900"
          onClick={() => void fetchDiscussions(status)}
          disabled={isLoading}
        >
          {isLoading ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <RefreshCw className="mr-2 h-4 w-4" />}
          새로고침
        </Button>
      </div>

      {errorMessage && (
        <div className="rounded-md border border-red-900/60 bg-red-950/40 px-4 py-3 text-sm text-red-300">
          {errorMessage}
        </div>
      )}

      {successMessage && (
        <div className="rounded-md border border-green-900/60 bg-green-950/40 px-4 py-3 text-sm text-green-300">
          {successMessage}
        </div>
      )}

      <Card className="border-zinc-800 bg-transparent text-white">
        <CardHeader>
          <CardTitle>AI 토론 주제 목록</CardTitle>
          <CardDescription>
            상태별로 AI 토론 주제를 조회하고 상세 내용을 확인할 수 있습니다.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="mb-4 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
              <Select value={status} onValueChange={(value) => handleStatusChange(value as AiDiscussionStatus)}>
                <SelectTrigger className="w-full sm:w-[180px]">
                  <SelectValue placeholder="상태 선택" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="PENDING">승인 대기</SelectItem>
                  <SelectItem value="APPROVED">승인 완료</SelectItem>
                  <SelectItem value="REJECTED">거절</SelectItem>
                </SelectContent>
              </Select>

              <Input
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="제목 또는 본문 검색"
                className="w-full sm:w-[260px]"
              />
            </div>
            <div className="text-sm text-zinc-400">총 {filteredPosts.length}개</div>
          </div>
          <div className="overflow-hidden rounded-md border border-zinc-800">
            <Table className="table-fixed">
              <TableHeader>
                <TableRow className="border-zinc-800 hover:bg-transparent">
                  <TableHead className="w-[100px] text-zinc-300">상태</TableHead>
                  <TableHead className="text-zinc-300">제목</TableHead>
                  <TableHead className="hidden w-[160px] text-zinc-300 md:table-cell">생성일</TableHead>
                  <TableHead className="hidden w-[120px] text-zinc-300 md:table-cell">게시글 ID</TableHead>
                  <TableHead className="w-[100px] text-right text-zinc-300">관리</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {isLoading ? (
                  <TableRow>
                    <TableCell colSpan={5} className="h-32 text-center text-zinc-400">
                      <Loader2 className="mx-auto mb-2 h-5 w-5 animate-spin" />
                      목록을 불러오는 중입니다.
                    </TableCell>
                  </TableRow>
                ) : filteredPosts.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} className="h-32 text-center text-zinc-400">
                      조회된 AI 토론 주제가 없습니다.
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredPosts.map((post) => (
                    <TableRow key={post.id} className="border-zinc-800 hover:bg-zinc-900/40">
                      <TableCell>
                        <Badge className={statusStyles[post.status]}>{statusLabels[post.status]}</Badge>
                      </TableCell>
                      <TableCell className="min-w-0">
                        <div className="truncate font-medium text-zinc-100">{post.title}</div>
                        <div className="mt-1 truncate text-sm text-zinc-400">{post.content}</div>
                      </TableCell>
                      <TableCell className="hidden text-sm text-zinc-400 md:table-cell">
                        {formatDateTime(post.createdAt)}
                      </TableCell>
                      <TableCell className="hidden text-sm text-zinc-400 md:table-cell">
                        {post.approvedPostId ?? "-"}
                      </TableCell>
                      <TableCell className="text-right">
                        <Button variant="ghost" size="sm" onClick={() => openDetail(post)}>
                          <Eye className="mr-1 h-4 w-4" />
                          상세
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>

      <Dialog open={isDetailOpen} onOpenChange={setIsDetailOpen}>
        <DialogContent className="max-w-3xl border-zinc-800 bg-zinc-950 text-white">
          <DialogHeader>
            <DialogTitle>AI 토론 주제 상세</DialogTitle>
            <DialogDescription>
              AI가 생성한 토론 주제를 확인한 뒤 승인 또는 거절할 수 있습니다.
            </DialogDescription>
          </DialogHeader>

          {selectedPost && (
            <div className="space-y-4">
              <div className="flex flex-wrap items-center gap-2">
                <Badge className={statusStyles[selectedPost.status]}>{statusLabels[selectedPost.status]}</Badge>
                <span className="text-sm text-zinc-400">생성일 {formatDateTime(selectedPost.createdAt)}</span>
              </div>

              <div className="space-y-2">
                <Label>제목</Label>
                <div className="rounded-md border border-zinc-800 bg-zinc-900 px-3 py-2 text-sm font-medium">
                  {selectedPost.title}
                </div>
              </div>

              <div className="space-y-2">
                <Label>본문</Label>
                <div className="max-h-[320px] overflow-y-auto whitespace-pre-wrap rounded-md border border-zinc-800 bg-zinc-900 px-3 py-2 text-sm leading-6">
                  {selectedPost.content}
                </div>
              </div>

              {selectedPost.rejectionReason && (
                <div className="space-y-2">
                  <Label>거절 사유</Label>
                  <div className="rounded-md border border-red-900/60 bg-red-950/40 px-3 py-2 text-sm text-red-300">
                    {selectedPost.rejectionReason}
                  </div>
                </div>
              )}
            </div>
          )}

          <DialogFooter className="gap-2 sm:gap-0">
            <Button
              variant="outline"
              className="border-zinc-800 bg-transparent text-zinc-200 hover:bg-zinc-900"
              onClick={() => setIsDetailOpen(false)}
            >
              닫기
            </Button>
            {selectedPost?.status === "PENDING" && (
              <>
                <Button variant="destructive" onClick={() => setIsRejectOpen(true)} disabled={isSubmitting}>
                  <XCircle className="mr-2 h-4 w-4" />
                  거절
                </Button>
                <Button onClick={() => approveDiscussion(selectedPost)} disabled={isSubmitting}>
                  {isSubmitting ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <CheckCircle className="mr-2 h-4 w-4" />}
                  승인
                </Button>
              </>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={isRejectOpen} onOpenChange={setIsRejectOpen}>
        <DialogContent className="border-zinc-800 bg-zinc-950 text-white">
          <DialogHeader>
            <DialogTitle>AI 토론 주제 거절</DialogTitle>
            <DialogDescription>거절 사유를 입력하면 관리자 기록에 함께 저장됩니다.</DialogDescription>
          </DialogHeader>

          <div className="space-y-2">
            <Label htmlFor="rejectReason">거절 사유</Label>
            <Textarea
              className="border-zinc-800 bg-zinc-900 text-white placeholder:text-zinc-500"
              id="rejectReason"
              value={rejectReason}
              onChange={(event) => setRejectReason(event.target.value)}
              placeholder="예: 주제가 커뮤니티 성격과 맞지 않습니다."
              rows={4}
            />
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              className="border-zinc-800 bg-transparent text-zinc-200 hover:bg-zinc-900"
              onClick={() => setIsRejectOpen(false)}
              disabled={isSubmitting}
            >
              취소
            </Button>
            <Button variant="destructive" onClick={rejectDiscussion} disabled={isSubmitting}>
              {isSubmitting ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <XCircle className="mr-2 h-4 w-4" />}
              거절 처리
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

function extractDiscussionList(data: unknown): AiDiscussionPostResponse[] {
  if (Array.isArray(data)) {
    return data as AiDiscussionPostResponse[]
  }

  if (isPageResponse<AiDiscussionPostResponse>(data)) {
    return data.content
  }

  if (isObject(data) && Array.isArray(data.data)) {
    return data.data as AiDiscussionPostResponse[]
  }

  if (isObject(data) && isPageResponse<AiDiscussionPostResponse>(data.data)) {
    return data.data.content
  }

  return []
}

function isPageResponse<T>(value: unknown): value is PageResponse<T> {
  return isObject(value) && Array.isArray(value.content)
}

function isObject(value: unknown): value is Record<string, any> {
  return typeof value === "object" && value !== null
}

function formatDateTime(value: string | null) {
  if (!value) {
    return "-"
  }

  const matched = value.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/)
  if (!matched) {
    return value
  }

  const [, year, month, day, hour, minute] = matched
  return `${year}. ${month}. ${day}. ${hour}:${minute}`
}
