"use client"

import { useCallback, useEffect, useMemo, useRef, useState } from "react"
import { useRouter } from "next/navigation"
import { useSearchParams } from "next/navigation"
import {
  ArrowLeft,
  Save,
  Eye,
  ImagePlus,
  Code,
  Link2,
  Bold,
  Italic,
  List,
  ListOrdered,
  Quote,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import Link from "next/link"
import { clearLoginSession, getAuthSnapshot } from "@/lib/auth-storage"
import { apiFetch, isApiError } from "@/lib/api"
import { categoryLabelMap, categorySlugMap } from "@/constants/category"

const categories = Object.entries(categoryLabelMap).map(([id, name]) => ({
  id: Number(id),
  name,
}))



type SuccessResponse<T> = {
  code?: string
  message?: string
  timestamp?: string
  data?: T
}

type MyInfoResponse = {
  userId: number
  email: string
  nickname: string
}

type PostCreateResponse = {
  postId: number
}

type PostDetailResponse = {
  postId: number
  title: string
  content: string
  categoryId: number
}

type FormatState = {
  bold: boolean
  italic: boolean
  code: boolean
  quote: boolean
  list: boolean
  ordered: boolean
  link: boolean
}

const stripHtml = (html: string) =>
  html
    .replace(/<[^>]*>/g, "")
    .replace(/&nbsp;/g, " ")
    .replace(/\u200b/g, "")
    .trim()

const escapeHtml = (text: string) =>
  text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;")

const readFileAsDataUrl = (file: File) =>
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result ?? ""))
    reader.onerror = reject
    reader.readAsDataURL(file)
  })

  const ALLOWED_TAGS = new Set([
    "p",
    "br",
    "strong",
    "b",
    "em",
    "i",
    "a",
    "blockquote",
    "ul",
    "ol",
    "li",
    "pre",
    "code",
    "img",
  ])
  
  function sanitizeRichTextHtml(rawHtml: string) {
    // SSR-safe fallback (DOMParser 없는 환경)
    if (typeof window === "undefined" || typeof DOMParser === "undefined") {
      return rawHtml
        .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, "")
        .replace(/<style[\s\S]*?>[\s\S]*?<\/style>/gi, "")
        .replace(/<(iframe|object|embed|link|meta|base)[^>]*>/gi, "")
        .replace(/\son\w+=(?:"[^"]*"|'[^']*'|[^\s>]+)/gi, "")
        .replace(/\sstyle=(?:"[^"]*"|'[^']*'|[^\s>]+)/gi, "")
        .replace(/\shref=(?:"\s*javascript:[^"]*"|'\s*javascript:[^']*'|javascript:[^\s>]+)/gi, "")
        .replace(/\ssrc=(?:"\s*javascript:[^"]*"|'\s*javascript:[^']*'|javascript:[^\s>]+)/gi, "")
    }
  
    // Client-side strict sanitize
    const parser = new DOMParser()
    const doc = parser.parseFromString(`<div>${rawHtml}</div>`, "text/html")
    const root = doc.body.firstElementChild as HTMLElement | null
    if (!root) return ""
  
    const elements = Array.from(root.querySelectorAll("*"))
  
    for (const el of elements) {
      const tag = el.tagName.toLowerCase()
  
      if (!ALLOWED_TAGS.has(tag)) {
        const parent = el.parentNode
        if (!parent) continue
        while (el.firstChild) {
          parent.insertBefore(el.firstChild, el)
        }
        parent.removeChild(el)
        continue
      }
  
      const href = el.getAttribute("href")?.trim() ?? ""
      const src = el.getAttribute("src")?.trim() ?? ""
      const alt = el.getAttribute("alt")?.trim() ?? ""
  
      for (const attr of Array.from(el.attributes)) {
        el.removeAttribute(attr.name)
      }
  
      if (tag === "a") {
        const isSafeHref =
          href.startsWith("http://") ||
          href.startsWith("https://") ||
          href.startsWith("mailto:")
        if (isSafeHref) {
          el.setAttribute("href", href)
          el.setAttribute("target", "_blank")
          el.setAttribute("rel", "noreferrer noopener")
        } else {
          const parent = el.parentNode
          if (!parent) continue
          while (el.firstChild) {
            parent.insertBefore(el.firstChild, el)
          }
          parent.removeChild(el)
        }
      }
  
      if (tag === "img") {
        const isSafeSrc =
          src.startsWith("http://") ||
          src.startsWith("https://") ||
          src.startsWith("data:image/")
        if (!isSafeSrc) {
          el.remove()
          continue
        }
        el.setAttribute("src", src)
        if (alt) el.setAttribute("alt", alt)
      }
    }
  
    return root.innerHTML
  }  

const getSelectionContainer = () => {
  const selection = window.getSelection()
  if (!selection || selection.rangeCount === 0) return null

  const node = selection.anchorNode
  if (!node) return null

  return node.nodeType === Node.TEXT_NODE ? node.parentElement : (node as Element)
}

const normalizeEditorNodes = (root: HTMLElement) => {
  root.querySelectorAll("ul").forEach((el) => {
    el.classList.add("list-disc", "pl-6", "my-2")
  })

  root.querySelectorAll("ol").forEach((el) => {
    el.classList.add("list-decimal", "pl-6", "my-2")
  })

  root.querySelectorAll("blockquote").forEach((el) => {
    el.classList.add("border-l-4", "border-border", "pl-3", "italic", "my-2")
  })

  root.querySelectorAll("pre").forEach((el) => {
    el.classList.add(
      "my-3",
      "max-w-full",
      "overflow-x-auto",
      "rounded-md",
      "border",
      "border-zinc-300",
      "bg-zinc-200",
      "p-3",
      "font-mono",
      "text-sm",
      "leading-6",
      "text-zinc-900"
    )
  })

  root.querySelectorAll("code").forEach((el) => {
    if (el.parentElement?.tagName === "PRE") {
      el.classList.add("bg-transparent", "p-0")
    } else {
      el.classList.add("rounded", "bg-muted", "px-1", "py-0.5", "font-mono", "text-sm")
    }
  })

  root.querySelectorAll("img").forEach((el) => {
    el.classList.add("my-2", "block", "h-auto", "max-w-full", "rounded-md")
  })
}

export default function WritePage() {
  const router = useRouter()
  const editorRef = useRef<HTMLDivElement>(null)
  const imageInputRef = useRef<HTMLInputElement>(null)
  const savedRangeRef = useRef<Range | null>(null)

  const [isLoading, setIsLoading] = useState(false)
  const [isPreview, setIsPreview] = useState(false)
  const [isAuthReady, setIsAuthReady] = useState(false)
  const [formatState, setFormatState] = useState<FormatState>({
    bold: false,
    italic: false,
    code: false,
    quote: false,
    list: false,
    ordered: false,
    link: false,
  })

  const [formData, setFormData] = useState({
    title: "",
    category: "",
    tags: "",
    content: "",
  })

  const searchParams = useSearchParams()
  const categorySlug = searchParams.get("category")

  const postId = searchParams.get("postId")
  const isEditMode = !!postId

  const previewHtml = useMemo(() => sanitizeRichTextHtml(formData.content), [formData.content])

  useEffect(() => {
    const auth = getAuthSnapshot()
    if (!auth.isLoggedIn) {
      router.replace("/login")
      return
    }

    const verifyAuth = async () => {
      try {
        await apiFetch<SuccessResponse<MyInfoResponse>>("/api/users/me", {
          method: "GET",
          auth: true,
          cache: "no-store",
        })
      } catch (error) {
        if (isApiError(error) && error.isUnauthorized) {
          clearLoginSession()
          router.replace("/login")
          return
        }
      }

      setIsAuthReady(true)
    }

    void verifyAuth()
  }, [router])

  useEffect(() => {
    if (!categorySlug) return
    const categoryId = categorySlug
    ? Number(
        Object.keys(categorySlugMap).find(
          (key) => categorySlugMap[Number(key)] === categorySlug
        )
      )
    : null
    if (categoryId) {
      setFormData((prev) => ({ ...prev, category: String(categoryId) }))
    }
  }, [categorySlug])

  useEffect(() => {
    if (!isEditMode || !postId) return

    const fetchPost = async () => {
      try {
          const res = await apiFetch<SuccessResponse<PostDetailResponse>>(`/api/posts/${postId}`, {
          method: "GET",
          auth: true,
        })

        const post = res.data!

        setFormData({
          title: post.title,
          content: sanitizeRichTextHtml(post.content),
          category: String(post.categoryId),
          tags: "",
        })
      } catch (err) {
        console.error(err)
        alert("게시글 불러오기 실패")
      }
    }

    void fetchPost()
  }, [isEditMode, postId])

  useEffect(() => {
    if (!editorRef.current || isPreview) return
    if (editorRef.current.innerHTML !== formData.content) {
      editorRef.current.innerHTML = formData.content
      normalizeEditorNodes(editorRef.current)
    }
  }, [formData.content, isPreview])

  const saveSelection = () => {
    const selection = window.getSelection()
    if (!selection || selection.rangeCount === 0 || !editorRef.current) return
    const container = getSelectionContainer()
    if (!container || !editorRef.current.contains(container)) return
    savedRangeRef.current = selection.getRangeAt(0).cloneRange()
  }

  const restoreSelection = () => {
    if (!savedRangeRef.current) return
    const selection = window.getSelection()
    if (!selection) return
    selection.removeAllRanges()
    selection.addRange(savedRangeRef.current)
  }

  const syncContentFromEditor = () => {
    if (!editorRef.current) return
    normalizeEditorNodes(editorRef.current)
    const html = editorRef.current.innerHTML
    setFormData((prev) => ({ ...prev, content: html }))
  }

  const updateFormatState = useCallback(() => {
    if (!editorRef.current) return

    const container = getSelectionContainer()
    if (!container || !editorRef.current.contains(container)) {
      setFormatState({
        bold: false,
        italic: false,
        code: false,
        quote: false,
        list: false,
        ordered: false,
        link: false,
      })
      return
    }

    const bold = document.queryCommandState("bold")
    const italic = document.queryCommandState("italic")
    const list = document.queryCommandState("insertUnorderedList")
    const ordered = document.queryCommandState("insertOrderedList")

    setFormatState({
      bold,
      italic,
      list,
      ordered,
      code: !!container.closest("code, pre"),
      quote: !!container.closest("blockquote"),
      link: !!container.closest("a"),
    })
  }, [])

  useEffect(() => {
    const handler = () => updateFormatState()
    document.addEventListener("selectionchange", handler)
    document.addEventListener("keyup", handler)
    document.addEventListener("mouseup", handler)
    return () => {
      document.removeEventListener("selectionchange", handler)
      document.removeEventListener("keyup", handler)
      document.removeEventListener("mouseup", handler)
    }
  }, [updateFormatState])

  const applyFormat = (type: string) => {
    if (!editorRef.current) return

    editorRef.current.focus()
    restoreSelection()

    const selection = window.getSelection()
    const selectedText = selection?.toString() ?? ""

    switch (type) {
      case "bold":
        document.execCommand("bold")
        break

      case "italic":
        document.execCommand("italic")
        break

      case "code": {
        const content = selectedText
          ? escapeHtml(selectedText).replace(/\n/g, "<br/>")
          : "<br/>"

        document.execCommand(
          "insertHTML",
          false,
          `<pre><code>${content}</code></pre><p><br/></p>`
        )
        break
      }

      case "link": {
        const url = window.prompt("링크 URL을 입력하세요")
        if (!url) return
        if (!selectedText) {
          document.execCommand(
            "insertHTML",
            false,
            `<a href="${escapeHtml(url)}">${escapeHtml(url)}</a>`
          )
        } else {
          document.execCommand("createLink", false, url)
        }
        break
      }

      case "quote":
        document.execCommand("formatBlock", false, "blockquote")
        break

      case "list":
        document.execCommand("insertUnorderedList")
        break

      case "ordered":
        document.execCommand("insertOrderedList")
        break

      default:
        break
    }

    syncContentFromEditor()
    updateFormatState()
  }

  const handleImageButtonClick = () => {
    if (!editorRef.current) return
    editorRef.current.focus()
    saveSelection()
    imageInputRef.current?.click()
  }

  const handleImageFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files ?? [])
    event.target.value = ""

    if (files.length === 0 || !editorRef.current) return

    editorRef.current.focus()
    restoreSelection()

    for (const file of files) {
      if (!file.type.startsWith("image/")) continue
      const dataUrl = await readFileAsDataUrl(file)
      document.execCommand(
        "insertHTML",
        false,
        `<img src="${escapeHtml(dataUrl)}" alt="${escapeHtml(file.name)}" />`
      )
    }

    syncContentFromEditor()
    updateFormatState()
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!formData.category) {
      alert("카테고리를 선택해주세요.")
      return
    }

    const sanitizedContent = sanitizeRichTextHtml(formData.content)

    if (!stripHtml(sanitizedContent)) {
      alert("내용을 입력해주세요.")
      return
    }

    setIsLoading(true)

    try {
      if (isEditMode && postId) {
        await apiFetch(`/api/posts/${postId}`, {
          method: "PUT",
          auth: true,
          body: JSON.stringify({
            title: formData.title,
            content: sanitizedContent,
            categoryId: Number(formData.category),
          }),
        })
        router.push(`/posts/${postId}`)
      } else {
        const res = await apiFetch<SuccessResponse<PostCreateResponse>>("/api/posts", {
          method: "POST",
          auth: true,
          body: JSON.stringify({
            title: formData.title,
            content: sanitizedContent,
            categoryId: Number(formData.category),
          }),
        })
        
        router.push(`/posts/${res.data?.postId}`)
      }
    } catch (err) {
      console.error(err)
      if (isApiError(err)) {
        alert(err.message)
      } else {
        alert("게시글 저장 중 오류가 발생했습니다.")
      }
    } finally {
      setIsLoading(false)
    }
  }

  if (!isAuthReady) {
    return null
  }

  const hasContent = !!stripHtml(previewHtml)
  const activeClass = "bg-accent text-accent-foreground shadow-inner ring-1 ring-border"

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-8 flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Link href="/">
            <Button variant="ghost" size="icon">
              <ArrowLeft className="h-5 w-5" />
            </Button>
          </Link>

          <h1 className="text-2xl font-bold text-foreground">
            {isEditMode ? "글 수정" : "글 쓰기"}
          </h1>
        </div>

        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => setIsPreview(!isPreview)} className="gap-2">
            <Eye className="h-4 w-4" />
            {isPreview ? "편집" : "미리보기"}
          </Button>

          <Button
            onClick={handleSubmit}
            disabled={isLoading || !formData.title || !hasContent}
            className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
          >
            <Save className="h-4 w-4" />
            {isEditMode ? (isLoading ? "수정 중..." : "수정") : isLoading ? "저장 중..." : "저장"}
          </Button>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="space-y-2">
          <Input
            id="title"
            type="text"
            placeholder="제목을 입력하세요"
            value={formData.title}
            onChange={(e) => setFormData({ ...formData, title: e.target.value })}
            required
            className="border-none bg-transparent text-3xl font-bold placeholder:text-muted-foreground focus-visible:ring-0"
          />
        </div>

        <div className="flex flex-wrap gap-4">
          <div className="w-full sm:w-48">
            <Label htmlFor="category" className="sr-only">카테고리</Label>
            <Select
              value={formData.category}
              onValueChange={(value) => setFormData({ ...formData, category: value })}
            >
              <SelectTrigger className="bg-secondary">
                <SelectValue placeholder="카테고리 선택" />
              </SelectTrigger>
              <SelectContent>
                {categories.map((category) => (
                  <SelectItem key={category.id} value={String(category.id)}>
                    {category.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex-1">
            <Label htmlFor="tags" className="sr-only">태그</Label>
            <Input
              id="tags"
              type="text"
              placeholder="태그 입력 (쉼표로 구분)"
              value={formData.tags}
              onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
              className="bg-secondary"
            />
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-1 rounded-lg border border-border bg-secondary p-2">
          <Button type="button" variant="ghost" size="icon" onMouseDown={(e) => e.preventDefault()} onClick={() => applyFormat("bold")} className={formatState.bold ? activeClass : ""}>
            <Bold className="h-4 w-4" />
          </Button>
          <Button type="button" variant="ghost" size="icon" onMouseDown={(e) => e.preventDefault()} onClick={() => applyFormat("italic")} className={formatState.italic ? activeClass : ""}>
            <Italic className="h-4 w-4" />
          </Button>

          <div className="mx-2 h-6 w-px bg-border" />

          <Button type="button" variant="ghost" size="icon" onMouseDown={(e) => e.preventDefault()} onClick={() => applyFormat("code")} className={formatState.code ? activeClass : ""}>
            <Code className="h-4 w-4" />
          </Button>
          <Button type="button" variant="ghost" size="icon" onMouseDown={(e) => e.preventDefault()} onClick={() => applyFormat("link")} className={formatState.link ? activeClass : ""}>
            <Link2 className="h-4 w-4" />
          </Button>
          <Button type="button" variant="ghost" size="icon" onMouseDown={(e) => e.preventDefault()} onClick={handleImageButtonClick}>
            <ImagePlus className="h-4 w-4" />
          </Button>

          <div className="mx-2 h-6 w-px bg-border" />

          <Button type="button" variant="ghost" size="icon" onMouseDown={(e) => e.preventDefault()} onClick={() => applyFormat("quote")} className={formatState.quote ? activeClass : ""}>
            <Quote className="h-4 w-4" />
          </Button>
          <Button type="button" variant="ghost" size="icon" onMouseDown={(e) => e.preventDefault()} onClick={() => applyFormat("list")} className={formatState.list ? activeClass : ""}>
            <List className="h-4 w-4" />
          </Button>
          <Button type="button" variant="ghost" size="icon" onMouseDown={(e) => e.preventDefault()} onClick={() => applyFormat("ordered")} className={formatState.ordered ? activeClass : ""}>
            <ListOrdered className="h-4 w-4" />
          </Button>

          <input
            ref={imageInputRef}
            type="file"
            accept="image/*"
            multiple
            className="hidden"
            onChange={handleImageFileChange}
          />
        </div>

        {isPreview ? (
          <div className="min-h-[400px] rounded-lg border border-border bg-card p-6">
            {hasContent ? (
              <div
                className="prose max-w-none text-foreground [&_pre]:max-w-full [&_pre]:overflow-x-auto [&_pre]:rounded-md [&_pre]:border [&_pre]:border-zinc-300 [&_pre]:bg-zinc-200 [&_pre]:p-3 [&_pre]:text-zinc-900 [&_img]:block [&_img]:max-w-full [&_img]:h-auto"
                dangerouslySetInnerHTML={{ __html: previewHtml }}
              />
            ) : (
              <div className="text-muted-foreground">미리볼 내용이 없습니다.</div>
            )}
          </div>
        ) : (
          <div className="space-y-2">
            <Label htmlFor="content" className="sr-only">내용</Label>
            <div
              id="content"
              ref={editorRef}
              contentEditable
              suppressContentEditableWarning
              onInput={() => {
                syncContentFromEditor()
                updateFormatState()
              }}
              onKeyUp={updateFormatState}
              onMouseUp={updateFormatState}
              data-placeholder="내용을 작성하세요. 툴바 버튼으로 서식을 적용할 수 있습니다."
              className="min-h-[400px] rounded-md bg-secondary p-3 text-sm outline-none ring-offset-background empty:before:pointer-events-none empty:before:text-muted-foreground empty:before:content-[attr(data-placeholder)] focus:ring-2 focus:ring-ring [&_ul]:list-disc [&_ul]:pl-6 [&_ol]:list-decimal [&_ol]:pl-6 [&_pre]:my-3 [&_pre]:max-w-full [&_pre]:overflow-x-auto [&_pre]:rounded-md [&_pre]:border [&_pre]:border-zinc-300 [&_pre]:bg-zinc-200 [&_pre]:p-3 [&_pre]:font-mono [&_pre]:text-sm [&_pre]:leading-6 [&_pre]:text-zinc-900 [&_img]:block [&_img]:max-w-full [&_img]:h-auto [&_img]:rounded-md"
            />
          </div>
        )}
      </form>
    </div>
  )
}