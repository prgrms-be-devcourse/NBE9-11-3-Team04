"use client"

import { useState } from "react"
import {
  Search,
  Plus,
  MoreHorizontal,
  FileText,
  MessageSquare,
  Megaphone,
  FolderOpen,
  Edit,
  Trash2,
  Eye,
  EyeOff,
  Pin,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Switch } from "@/components/ui/switch"

// Types
interface Category {
  id: string
  name: string
  slug: string
  description: string
  postCount: number
  isActive: boolean
}

interface Post {
  id: string
  title: string
  author: string
  category: string
  createdAt: string
  views: number
  comments: number
  isHidden: boolean
  isPinned: boolean
}

interface Notice {
  id: string
  title: string
  content: string
  createdAt: string
  isActive: boolean
  isPinned: boolean
}

// Mock data
const mockCategories: Category[] = [
  { id: "1", name: "IT 기술 정보", slug: "tech", description: "기술 관련 정보 공유", postCount: 1245, isActive: true },
  { id: "2", name: "취업 시장 정보", slug: "job-market", description: "취업 및 채용 정보", postCount: 892, isActive: true },
  { id: "3", name: "개발자 트렌드", slug: "trend", description: "개발자 트렌드 토론", postCount: 567, isActive: true },
  { id: "4", name: "자유 주제", slug: "free", description: "자유로운 대화", postCount: 2341, isActive: true },
  { id: "5", name: "Q&A", slug: "qa", description: "질문과 답변", postCount: 1823, isActive: false },
]

const mockPosts: Post[] = [
  { id: "1", title: "2026년 프론트엔드 개발자 로드맵", author: "김개발", category: "IT 기술 정보", createdAt: "2024-01-15", views: 1520, comments: 24, isHidden: false, isPinned: true },
  { id: "2", title: "네카라쿠배당토 신입 개발자 채용 트렌드", author: "박취준", category: "취업 시장 정보", createdAt: "2024-01-14", views: 892, comments: 15, isHidden: false, isPinned: false },
  { id: "3", title: "[광고] 비트코인 투자 꿀팁", author: "이스팸", category: "자유 주제", createdAt: "2024-01-13", views: 45, comments: 0, isHidden: true, isPinned: false },
  { id: "4", title: "AI 코딩 어시스턴트 비교", author: "이트렌드", category: "개발자 트렌드", createdAt: "2024-01-12", views: 3240, comments: 42, isHidden: false, isPinned: false },
  { id: "5", title: "Kubernetes 입문 가이드", author: "최데브옵스", category: "IT 기술 정보", createdAt: "2024-01-11", views: 2150, comments: 31, isHidden: false, isPinned: false },
]

const mockNotices: Notice[] = [
  { id: "1", title: "커뮤니티 이용 규칙 안내", content: "DevHub 커뮤니티 이용 규칙에 대해 안내드립니다...", createdAt: "2024-01-01", isActive: true, isPinned: true },
  { id: "2", title: "서버 점검 안내 (1/20)", content: "1월 20일 새벽 2시~4시 서버 점검이 예정되어 있습니다...", createdAt: "2024-01-15", isActive: true, isPinned: true },
  { id: "3", title: "신규 기능 업데이트 안내", content: "새로운 기능들이 추가되었습니다...", createdAt: "2024-01-10", isActive: true, isPinned: false },
  { id: "4", title: "2023년 연말 이벤트 종료", content: "연말 이벤트가 종료되었습니다...", createdAt: "2023-12-31", isActive: false, isPinned: false },
]

export default function ContentManagementPage() {
  const [categories, setCategories] = useState<Category[]>(mockCategories)
  const [posts, setPosts] = useState<Post[]>(mockPosts)
  const [notices, setNotices] = useState<Notice[]>(mockNotices)
  const [searchQuery, setSearchQuery] = useState("")

  // Category dialog
  const [categoryDialog, setCategoryDialog] = useState<{
    open: boolean
    mode: "create" | "edit"
    category: Category | null
  }>({ open: false, mode: "create", category: null })
  const [categoryForm, setCategoryForm] = useState({ name: "", slug: "", description: "" })

  // Notice dialog
  const [noticeDialog, setNoticeDialog] = useState<{
    open: boolean
    mode: "create" | "edit"
    notice: Notice | null
  }>({ open: false, mode: "create", notice: null })
  const [noticeForm, setNoticeForm] = useState({ title: "", content: "" })

  // Category handlers
  const openCategoryDialog = (mode: "create" | "edit", category?: Category) => {
    if (mode === "edit" && category) {
      setCategoryForm({ name: category.name, slug: category.slug, description: category.description })
    } else {
      setCategoryForm({ name: "", slug: "", description: "" })
    }
    setCategoryDialog({ open: true, mode, category: category || null })
  }

  const saveCategory = () => {
    if (categoryDialog.mode === "create") {
      const newCategory: Category = {
        id: String(categories.length + 1),
        ...categoryForm,
        postCount: 0,
        isActive: true,
      }
      setCategories([...categories, newCategory])
    } else if (categoryDialog.category) {
      setCategories(categories.map(c => 
        c.id === categoryDialog.category!.id 
          ? { ...c, ...categoryForm }
          : c
      ))
    }
    setCategoryDialog({ open: false, mode: "create", category: null })
  }

  const toggleCategoryActive = (id: string) => {
    setCategories(categories.map(c => 
      c.id === id ? { ...c, isActive: !c.isActive } : c
    ))
  }

  // Post handlers
  const togglePostVisibility = (id: string) => {
    setPosts(posts.map(p => 
      p.id === id ? { ...p, isHidden: !p.isHidden } : p
    ))
  }

  const togglePostPin = (id: string) => {
    setPosts(posts.map(p => 
      p.id === id ? { ...p, isPinned: !p.isPinned } : p
    ))
  }

  const deletePost = (id: string) => {
    setPosts(posts.filter(p => p.id !== id))
  }

  // Notice handlers
  const openNoticeDialog = (mode: "create" | "edit", notice?: Notice) => {
    if (mode === "edit" && notice) {
      setNoticeForm({ title: notice.title, content: notice.content })
    } else {
      setNoticeForm({ title: "", content: "" })
    }
    setNoticeDialog({ open: true, mode, notice: notice || null })
  }

  const saveNotice = () => {
    if (noticeDialog.mode === "create") {
      const newNotice: Notice = {
        id: String(notices.length + 1),
        ...noticeForm,
        createdAt: new Date().toISOString().split("T")[0],
        isActive: true,
        isPinned: false,
      }
      setNotices([newNotice, ...notices])
    } else if (noticeDialog.notice) {
      setNotices(notices.map(n => 
        n.id === noticeDialog.notice!.id 
          ? { ...n, ...noticeForm }
          : n
      ))
    }
    setNoticeDialog({ open: false, mode: "create", notice: null })
  }

  const toggleNoticeActive = (id: string) => {
    setNotices(notices.map(n => 
      n.id === id ? { ...n, isActive: !n.isActive } : n
    ))
  }

  const toggleNoticePinned = (id: string) => {
    setNotices(notices.map(n => 
      n.id === id ? { ...n, isPinned: !n.isPinned } : n
    ))
  }

  const filteredPosts = posts.filter(p => 
    p.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
    p.author.toLowerCase().includes(searchQuery.toLowerCase())
  )

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-foreground">콘텐츠 관리</h1>
        <p className="text-muted-foreground">
          카테고리, 게시글, 공지사항을 관리합니다.
        </p>
      </div>

      {/* Tabs */}
      <Tabs defaultValue="categories" className="w-full">
        <TabsList className="grid w-full max-w-md grid-cols-3 bg-secondary">
          <TabsTrigger value="categories" className="flex items-center gap-2">
            <FolderOpen className="h-4 w-4" />
            <span className="hidden sm:inline">카테고리</span>
          </TabsTrigger>
          <TabsTrigger value="posts" className="flex items-center gap-2">
            <FileText className="h-4 w-4" />
            <span className="hidden sm:inline">게시글</span>
          </TabsTrigger>
          <TabsTrigger value="notices" className="flex items-center gap-2">
            <Megaphone className="h-4 w-4" />
            <span className="hidden sm:inline">공지사항</span>
          </TabsTrigger>
        </TabsList>

        {/* Categories Tab */}
        <TabsContent value="categories" className="mt-6">
          <div className="mb-4 flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              총 {categories.length}개의 카테고리
            </p>
            <Button onClick={() => openCategoryDialog("create")} className="gap-2">
              <Plus className="h-4 w-4" />
              카테고리 추가
            </Button>
          </div>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {categories.map((category) => (
              <Card key={category.id} className={!category.isActive ? "opacity-60" : ""}>
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-base font-medium">{category.name}</CardTitle>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="icon">
                        <MoreHorizontal className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem onClick={() => openCategoryDialog("edit", category)}>
                        <Edit className="mr-2 h-4 w-4" />
                        수정
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => toggleCategoryActive(category.id)}>
                        {category.isActive ? (
                          <>
                            <EyeOff className="mr-2 h-4 w-4" />
                            비활성화
                          </>
                        ) : (
                          <>
                            <Eye className="mr-2 h-4 w-4" />
                            활성화
                          </>
                        )}
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </CardHeader>
                <CardContent>
                  <p className="mb-2 text-sm text-muted-foreground">{category.description}</p>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-muted-foreground">
                      게시글 {category.postCount.toLocaleString()}개
                    </span>
                    <span className={`rounded px-2 py-0.5 text-xs font-medium ${
                      category.isActive 
                        ? "bg-green-500/10 text-green-500" 
                        : "bg-muted text-muted-foreground"
                    }`}>
                      {category.isActive ? "활성" : "비활성"}
                    </span>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>

        {/* Posts Tab */}
        <TabsContent value="posts" className="mt-6">
          <div className="mb-4 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="relative flex-1 sm:max-w-xs">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="제목 또는 작성자 검색..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9"
              />
            </div>
            <p className="text-sm text-muted-foreground">
              총 {filteredPosts.length}개의 게시글
            </p>
          </div>

          <div className="overflow-hidden rounded-lg border border-border">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-secondary">
                  <tr>
                    <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">게시글</th>
                    <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">카테고리</th>
                    <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">조회/댓글</th>
                    <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">상태</th>
                    <th className="px-4 py-3 text-right text-sm font-medium text-muted-foreground">작업</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {filteredPosts.map((post) => (
                    <tr key={post.id} className={`bg-card hover:bg-secondary/50 ${post.isHidden ? "opacity-60" : ""}`}>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          {post.isPinned && <Pin className="h-4 w-4 text-primary" />}
                          <div>
                            <p className="text-sm font-medium text-foreground">{post.title}</p>
                            <p className="text-xs text-muted-foreground">{post.author} · {post.createdAt}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <span className="rounded bg-secondary px-2 py-1 text-xs text-secondary-foreground">
                          {post.category}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-sm text-muted-foreground">
                        {post.views.toLocaleString()} / {post.comments}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          {post.isHidden && (
                            <span className="rounded bg-destructive/10 px-2 py-0.5 text-xs text-destructive">숨김</span>
                          )}
                          {post.isPinned && (
                            <span className="rounded bg-primary/10 px-2 py-0.5 text-xs text-primary">고정</span>
                          )}
                          {!post.isHidden && !post.isPinned && (
                            <span className="text-xs text-muted-foreground">일반</span>
                          )}
                        </div>
                      </td>
                      <td className="px-4 py-3 text-right">
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon">
                              <MoreHorizontal className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={() => togglePostPin(post.id)}>
                              <Pin className="mr-2 h-4 w-4" />
                              {post.isPinned ? "고정 해제" : "상단 고정"}
                            </DropdownMenuItem>
                            <DropdownMenuItem onClick={() => togglePostVisibility(post.id)}>
                              {post.isHidden ? (
                                <>
                                  <Eye className="mr-2 h-4 w-4" />
                                  숨김 해제
                                </>
                              ) : (
                                <>
                                  <EyeOff className="mr-2 h-4 w-4" />
                                  숨기기
                                </>
                              )}
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem 
                              onClick={() => deletePost(post.id)}
                              className="text-destructive"
                            >
                              <Trash2 className="mr-2 h-4 w-4" />
                              삭제
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </TabsContent>

        {/* Notices Tab */}
        <TabsContent value="notices" className="mt-6">
          <div className="mb-4 flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              총 {notices.length}개의 공지사항
            </p>
            <Button onClick={() => openNoticeDialog("create")} className="gap-2">
              <Plus className="h-4 w-4" />
              공지 작성
            </Button>
          </div>

          <div className="space-y-4">
            {notices.map((notice) => (
              <Card key={notice.id} className={!notice.isActive ? "opacity-60" : ""}>
                <CardHeader className="flex flex-row items-start justify-between pb-2">
                  <div className="flex items-center gap-2">
                    {notice.isPinned && <Pin className="h-4 w-4 text-primary" />}
                    <div>
                      <CardTitle className="text-base font-medium">{notice.title}</CardTitle>
                      <p className="text-xs text-muted-foreground">{notice.createdAt}</p>
                    </div>
                  </div>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="icon">
                        <MoreHorizontal className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem onClick={() => openNoticeDialog("edit", notice)}>
                        <Edit className="mr-2 h-4 w-4" />
                        수정
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => toggleNoticePinned(notice.id)}>
                        <Pin className="mr-2 h-4 w-4" />
                        {notice.isPinned ? "고정 해제" : "상단 고정"}
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => toggleNoticeActive(notice.id)}>
                        {notice.isActive ? (
                          <>
                            <EyeOff className="mr-2 h-4 w-4" />
                            비활성화
                          </>
                        ) : (
                          <>
                            <Eye className="mr-2 h-4 w-4" />
                            활성화
                          </>
                        )}
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </CardHeader>
                <CardContent>
                  <p className="text-sm text-muted-foreground line-clamp-2">{notice.content}</p>
                  <div className="mt-3 flex items-center gap-2">
                    {notice.isPinned && (
                      <span className="rounded bg-primary/10 px-2 py-0.5 text-xs text-primary">고정</span>
                    )}
                    <span className={`rounded px-2 py-0.5 text-xs font-medium ${
                      notice.isActive 
                        ? "bg-green-500/10 text-green-500" 
                        : "bg-muted text-muted-foreground"
                    }`}>
                      {notice.isActive ? "활성" : "비활성"}
                    </span>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>
      </Tabs>

      {/* Category Dialog */}
      <Dialog 
        open={categoryDialog.open} 
        onOpenChange={(open) => !open && setCategoryDialog({ open: false, mode: "create", category: null })}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {categoryDialog.mode === "create" ? "카테고리 추가" : "카테고리 수정"}
            </DialogTitle>
            <DialogDescription>
              카테고리 정보를 입력하세요.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>카테고리명</Label>
              <Input
                value={categoryForm.name}
                onChange={(e) => setCategoryForm({ ...categoryForm, name: e.target.value })}
                placeholder="예: IT 기술 정보"
              />
            </div>
            <div className="space-y-2">
              <Label>슬러그 (URL)</Label>
              <Input
                value={categoryForm.slug}
                onChange={(e) => setCategoryForm({ ...categoryForm, slug: e.target.value })}
                placeholder="예: tech"
              />
            </div>
            <div className="space-y-2">
              <Label>설명</Label>
              <Textarea
                value={categoryForm.description}
                onChange={(e) => setCategoryForm({ ...categoryForm, description: e.target.value })}
                placeholder="카테고리에 대한 간단한 설명"
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCategoryDialog({ open: false, mode: "create", category: null })}>
              취소
            </Button>
            <Button onClick={saveCategory}>저장</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Notice Dialog */}
      <Dialog 
        open={noticeDialog.open} 
        onOpenChange={(open) => !open && setNoticeDialog({ open: false, mode: "create", notice: null })}
      >
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>
              {noticeDialog.mode === "create" ? "공지사항 작성" : "공지사항 수정"}
            </DialogTitle>
            <DialogDescription>
              공지사항 내용을 입력하세요.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>제목</Label>
              <Input
                value={noticeForm.title}
                onChange={(e) => setNoticeForm({ ...noticeForm, title: e.target.value })}
                placeholder="공지사항 제목"
              />
            </div>
            <div className="space-y-2">
              <Label>내용</Label>
              <Textarea
                value={noticeForm.content}
                onChange={(e) => setNoticeForm({ ...noticeForm, content: e.target.value })}
                placeholder="공지사항 내용을 입력하세요..."
                rows={6}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setNoticeDialog({ open: false, mode: "create", notice: null })}>
              취소
            </Button>
            <Button onClick={saveNotice}>저장</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
