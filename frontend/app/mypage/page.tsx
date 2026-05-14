"use client"

import { useEffect, useMemo, useState, useCallback } from "react"
import type React from "react"
import Link from "next/link"
import { useRouter, useSearchParams } from "next/navigation"
import { PostCard, type Post } from "@/components/post-card"
import { Button } from "@/components/ui/button"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
  Settings,
  FileText,
  Bookmark,
  Heart,
  MessageCircle,
  Calendar,
  MapPin,
  Link as LinkIcon,
  Github,
  Twitter,
} from "lucide-react"
import {
  AUTH_CHANGED_EVENT,
  getAuthSnapshot,
  getCurrentUserProfile,
  persistLoginSession,
} from "@/lib/auth-storage"
import { apiFetch } from "@/lib/api"

const PAGE_SIZE = 10

type SuccessResponse<T> = {
  code: string
  message: string
  timestamp: string
  data: T
}

type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  hasNext: boolean
}

type MyProfileResponse = {
  userId: number
  email: string
  nickname: string
}

type MyPostResponse = {
  postId: number
  title: string
  likeCount: number
  commentCount: number
  viewCount: number
  createdAt: string
  liked: boolean
  bookmarked: boolean
}

type MyCommentItem = {
  commentId: number
  postId: number
  postTitle: string
  content: string
  createdAt: string
}

type LikedPostResponse = {
  postId: number
  title: string
  authorNickname: string
  likeCount: number
  commentCount: number
  viewCount: number
  createdAt: string
  liked: boolean
  bookmarked: boolean
}

type BookmarkedPostResponse = {
  postId: number
  title: string
  authorNickname: string
  likeCount: number
  commentCount: number
  viewCount: number
  createdAt: string
  liked: boolean
  bookmarked: boolean
}

type LocalProfileData = {
  bio: string
  location: string
  website: string
  github: string
  twitter: string
}

const defaultUserData = {
  avatar: "",
  joinedAt: "2024년 1월",
}

const emptyProfileData: LocalProfileData = {
  bio: "",
  location: "",
  website: "",
  github: "",
  twitter: "",
}

const createEmptyPage = <T,>(): PageResponse<T> => ({
  content: [],
  page: 0,
  size: PAGE_SIZE,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
  hasNext: false,
})

const normalizePage = <T,>(
  page: PageResponse<T>,
  requestedPage: number
): PageResponse<T> => {
  const safeContent = Array.isArray(page.content) ? page.content : []

  const totalElements =
    page.totalElements && page.totalElements > 0
      ? page.totalElements
      : safeContent.length

  if (safeContent.length > PAGE_SIZE) {
    const start = requestedPage * PAGE_SIZE
    const end = start + PAGE_SIZE
    const slicedContent = safeContent.slice(start, end)
    const totalPages = Math.max(1, Math.ceil(safeContent.length / PAGE_SIZE))

    return {
      content: slicedContent,
      page: requestedPage,
      size: PAGE_SIZE,
      totalElements: safeContent.length,
      totalPages,
      first: requestedPage === 0,
      last: requestedPage >= totalPages - 1,
      hasNext: requestedPage < totalPages - 1,
    }
  }

  const totalPages =
    page.totalPages && page.totalPages > 0
      ? page.totalPages
      : Math.max(1, Math.ceil(totalElements / PAGE_SIZE))

  return {
    content: safeContent,
    page: typeof page.page === "number" ? page.page : requestedPage,
    size: page.size || PAGE_SIZE,
    totalElements,
    totalPages,
    first: typeof page.first === "boolean" ? page.first : requestedPage === 0,
    last:
      typeof page.last === "boolean"
        ? page.last
        : requestedPage >= totalPages - 1,
    hasNext:
      typeof page.hasNext === "boolean"
        ? page.hasNext
        : requestedPage < totalPages - 1,
  }
}

function normalizeWebsiteUrl(value: string) {
  const trimmed = value?.trim()
  if (!trimmed) return ""

  if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
    return trimmed
  }

  return `https://${trimmed}`
}

function normalizeGithubUrl(value: string) {
  const trimmed = value?.trim()
  if (!trimmed) return ""

  if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
    return trimmed
  }

  const normalizedId = trimmed
    .replace(/^https?:\/\/github\.com\//, "")
    .replace(/^github\.com\//, "")
    .replace(/^@/, "")
    .replace(/\/+$/, "")

  return normalizedId ? `https://github.com/${normalizedId}` : ""
}

function normalizeTwitterUrl(value: string) {
  const trimmed = value?.trim()
  if (!trimmed) return ""

  if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
    return trimmed
  }

  const normalizedId = trimmed
    .replace(/^https?:\/\/twitter\.com\//, "")
    .replace(/^https?:\/\/x\.com\//, "")
    .replace(/^twitter\.com\//, "")
    .replace(/^x\.com\//, "")
    .replace(/^@/, "")
    .replace(/\/+$/, "")

  return normalizedId ? `https://twitter.com/${normalizedId}` : ""
}

function formatRelativeDate(value: string) {
  if (!value) return ""

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value

  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMinutes = Math.floor(diffMs / 1000 / 60)
  const diffHours = Math.floor(diffMinutes / 60)
  const diffDays = Math.floor(diffHours / 24)

  if (diffMinutes < 1) return "방금 전"
  if (diffMinutes < 60) return `${diffMinutes}분 전`
  if (diffHours < 24) return `${diffHours}시간 전`
  if (diffDays < 30) return `${diffDays}일 전`

  return date.toLocaleDateString("ko-KR")
}

function formatJoinedAt(email?: string | null) {
  if (!email) return `${defaultUserData.joinedAt} 가입`
  return "가입 정보"
}

function mapMyPostsToPostCard(
  posts: MyPostResponse[],
  displayName: string
): Post[] {
  return posts.map((post) => ({
    id: String(post.postId),
    title: post.title,
    excerpt: "",
    author: { name: displayName },
    category: "내 글",
    categorySlug: "mypage-posts",
    categoryId: 0,
    createdAt: formatRelativeDate(post.createdAt),
    likes: post.likeCount,
    comments: post.commentCount,
    views: post.viewCount,
    tags: [],
    liked: post.liked,
    bookmarked: post.bookmarked,
  }))
}

function mapBookmarkedPostsToPostCard(
  posts: BookmarkedPostResponse[]
): Post[] {
  return posts.map((post) => ({
    id: String(post.postId),
    title: post.title,
    excerpt: "",
    author: { name: post.authorNickname },
    category: "북마크",
    categorySlug: "mypage-bookmarks",
    categoryId: 0,
    createdAt: formatRelativeDate(post.createdAt),
    likes: post.likeCount,
    comments: post.commentCount,
    views: post.viewCount,
    tags: [],
    liked: post.liked,
    bookmarked: post.bookmarked,
  }))
}

function mapLikedPostsToPostCard(
  posts: LikedPostResponse[]
): Post[] {
  return posts.map((post) => ({
    id: String(post.postId),
    title: post.title,
    excerpt: "",
    author: { name: post.authorNickname },
    category: "좋아요",
    categorySlug: "mypage-likes",
    categoryId: 0,
    createdAt: formatRelativeDate(post.createdAt),
    likes: post.likeCount,
    comments: post.commentCount,
    views: post.viewCount,
    tags: [],
    liked: post.liked,
    bookmarked: post.bookmarked,
  }))
}

export default function MyPage() {
  const router = useRouter()
  const searchParams = useSearchParams()

  const [activeTab, setActiveTab] = useState("posts")
  const [displayName, setDisplayName] = useState("김개발")
  const [displayUsername, setDisplayUsername] = useState("kimdev")
  const [displayEmail, setDisplayEmail] = useState("")
  const [isAuthReady, setIsAuthReady] = useState(false)
  const [loading, setLoading] = useState(true)
  const [tabLoading, setTabLoading] = useState(false)
  const [error, setError] = useState("")

  const [profileData, setProfileData] =
    useState<LocalProfileData>(emptyProfileData)

  const [myPosts, setMyPosts] = useState<PageResponse<MyPostResponse>>(
    createEmptyPage<MyPostResponse>()
  )
  const [bookmarkedPosts, setBookmarkedPosts] =
    useState<PageResponse<BookmarkedPostResponse>>(
      createEmptyPage<BookmarkedPostResponse>()
    )
  const [likedPosts, setLikedPosts] = useState<PageResponse<LikedPostResponse>>(
    createEmptyPage<LikedPostResponse>()
  )
  const [myComments, setMyComments] = useState<PageResponse<MyCommentItem>>(
    createEmptyPage<MyCommentItem>()
  )

  const [bookmarkCount, setBookmarkCount] = useState(0)
  const [likeCount, setLikeCount] = useState(0)
  const [commentCount, setCommentCount] = useState(0)

  const websiteHref = useMemo(
    () => normalizeWebsiteUrl(profileData.website),
    [profileData.website]
  )
  const githubHref = useMemo(
    () => normalizeGithubUrl(profileData.github),
    [profileData.github]
  )
  const twitterHref = useMemo(
    () => normalizeTwitterUrl(profileData.twitter),
    [profileData.twitter]
  )

  const myPostCards = useMemo(
    () => mapMyPostsToPostCard(myPosts.content, displayName),
    [myPosts.content, displayName]
  )

  const bookmarkedPostCards = useMemo(
    () => mapBookmarkedPostsToPostCard(bookmarkedPosts.content),
    [bookmarkedPosts.content]
  )

  const likedPostCards = useMemo(
    () => mapLikedPostsToPostCard(likedPosts.content),
    [likedPosts.content]
  )

  const fetchMyPostsPage = useCallback(async (page: number) => {
    const res = await apiFetch<SuccessResponse<PageResponse<MyPostResponse>>>(
      `/api/mypage/posts?page=${page}&size=${PAGE_SIZE}`,
      {
        method: "GET",
        auth: true,
      }
    )

    const postsPage = normalizePage(
      res?.data ?? createEmptyPage<MyPostResponse>(),
      page
    )

    setMyPosts(postsPage)
  }, [])

  const fetchBookmarkedPostsPage = useCallback(async (page: number) => {
    const res = await apiFetch<
      SuccessResponse<PageResponse<BookmarkedPostResponse>>
    >(`/api/mypage/bookmarks?page=${page}&size=${PAGE_SIZE}`, {
      method: "GET",
      auth: true,
    })

    const bookmarksPage = normalizePage(
      res?.data ?? createEmptyPage<BookmarkedPostResponse>(),
      page
    )

    setBookmarkedPosts(bookmarksPage)
    setBookmarkCount(bookmarksPage.totalElements)
  }, [])

  const fetchLikedPostsPage = useCallback(async (page: number) => {
    const res = await apiFetch<SuccessResponse<PageResponse<LikedPostResponse>>>(
      `/api/mypage/likes?page=${page}&size=${PAGE_SIZE}`,
      {
        method: "GET",
        auth: true,
      }
    )

    const likesPage = normalizePage(
      res?.data ?? createEmptyPage<LikedPostResponse>(),
      page
    )

    setLikedPosts(likesPage)
    setLikeCount(likesPage.totalElements)
  }, [])

  const fetchMyCommentsPage = useCallback(async (page: number) => {
    const res = await apiFetch<SuccessResponse<PageResponse<MyCommentItem>>>(
      `/api/mypage/comments?page=${page}&size=${PAGE_SIZE}`,
      {
        method: "GET",
        auth: true,
      }
    )

    const commentsPage = normalizePage(
      res?.data ?? createEmptyPage<MyCommentItem>(),
      page
    )

    setMyComments(commentsPage)
    setCommentCount(commentsPage.totalElements)
  }, [])

  const refreshInteractionData = useCallback(async () => {
    const [bookmarksRes, likesRes, commentsRes] = await Promise.allSettled([
      apiFetch<SuccessResponse<PageResponse<BookmarkedPostResponse>>>(
        `/api/mypage/bookmarks?page=0&size=${PAGE_SIZE}`,
        {
          method: "GET",
          auth: true,
        }
      ),
      apiFetch<SuccessResponse<PageResponse<LikedPostResponse>>>(
        `/api/mypage/likes?page=0&size=${PAGE_SIZE}`,
        {
          method: "GET",
          auth: true,
        }
      ),
      apiFetch<SuccessResponse<PageResponse<MyCommentItem>>>(
        `/api/mypage/comments?page=0&size=${PAGE_SIZE}`,
        {
          method: "GET",
          auth: true,
        }
      ),
    ])

    if (bookmarksRes.status === "fulfilled") {
      const bookmarksPage = normalizePage(
        bookmarksRes.value?.data ?? createEmptyPage<BookmarkedPostResponse>(),
        0
      )

      setBookmarkCount(bookmarksPage.totalElements)
    }

    if (likesRes.status === "fulfilled") {
      const likesPage = normalizePage(
        likesRes.value?.data ?? createEmptyPage<LikedPostResponse>(),
        0
      )

      setLikeCount(likesPage.totalElements)
    }

    if (commentsRes.status === "fulfilled") {
      const commentsPage = normalizePage(
        commentsRes.value?.data ?? createEmptyPage<MyCommentItem>(),
        0
      )

      setCommentCount(commentsPage.totalElements)
    }
  }, [])

  useEffect(() => {
    const tab = searchParams.get("tab")

    if (
      tab === "posts" ||
      tab === "bookmarks" ||
      tab === "likes" ||
      tab === "comments"
    ) {
      setActiveTab(tab)
    }
  }, [searchParams])

  useEffect(() => {
    const fetchInitialData = async () => {
      try {
        setLoading(true)
        setError("")

        const [profileRes, postsRes] = await Promise.all([
          apiFetch<SuccessResponse<MyProfileResponse>>("/api/mypage", {
            method: "GET",
            auth: true,
          }),
          apiFetch<SuccessResponse<PageResponse<MyPostResponse>>>(
            `/api/mypage/posts?page=0&size=${PAGE_SIZE}`,
            {
              method: "GET",
              auth: true,
            }
          ),
        ])

        const profile = profileRes?.data
        const postsPage = normalizePage(
          postsRes?.data ?? createEmptyPage<MyPostResponse>(),
          0
        )

        const nextName = profile?.nickname?.trim() || "김개발"
        const nextEmail = profile?.email?.trim() || ""
        const nextUsername = nextEmail
          ? nextEmail.split("@")[0]
          : nextName.replace(/\s+/g, "")

        setDisplayName(nextName)
        setDisplayEmail(nextEmail)
        setDisplayUsername(nextUsername)
        setMyPosts(postsPage)

        persistLoginSession(undefined, nextName, nextEmail)

        const savedProfile = getCurrentUserProfile()

        setProfileData({
          bio: savedProfile?.bio?.trim() ?? "",
          location: savedProfile?.location?.trim() ?? "",
          website: savedProfile?.website?.trim() ?? "",
          github: savedProfile?.github?.trim() ?? "",
          twitter: savedProfile?.twitter?.trim() ?? "",
        })

        setIsAuthReady(true)
      } catch (err) {
        console.error(err)
        router.replace("/login")
      } finally {
        setLoading(false)
      }
    }

    fetchInitialData()
  }, [router])

  useEffect(() => {
    if (!isAuthReady) return

    const fetchCounts = async () => {
      try {
        await refreshInteractionData()
      } catch (err) {
        console.error(err)
      }
    }

    fetchCounts()
  }, [isAuthReady, refreshInteractionData])

  useEffect(() => {
    if (!isAuthReady) return

    const syncProfile = () => {
      const auth = getAuthSnapshot()
      const profile = getCurrentUserProfile()

      const nickname =
        profile?.nickname?.trim() ||
        auth.nickname?.trim() ||
        displayName ||
        "김개발"

      const email =
        profile?.email?.trim() || auth.email?.trim() || displayEmail || ""

      const usernameFromProfile = profile?.username?.trim()
      const usernameFromEmail = email ? email.split("@")[0] : ""
      const usernameFromName = nickname.trim().replace(/\s+/g, "")
      const username =
        usernameFromProfile || usernameFromEmail || usernameFromName || "kimdev"

      setDisplayName(nickname)
      setDisplayEmail(email)
      setDisplayUsername(username)
      setProfileData({
        bio: profile?.bio?.trim() ?? "",
        location: profile?.location?.trim() ?? "",
        website: profile?.website?.trim() ?? "",
        github: profile?.github?.trim() ?? "",
        twitter: profile?.twitter?.trim() ?? "",
      })
    }

    syncProfile()
    window.addEventListener(AUTH_CHANGED_EVENT, syncProfile as EventListener)
    window.addEventListener("storage", syncProfile)

    return () => {
      window.removeEventListener(
        AUTH_CHANGED_EVENT,
        syncProfile as EventListener
      )
      window.removeEventListener("storage", syncProfile)
    }
  }, [isAuthReady, displayName, displayEmail])

  useEffect(() => {
    if (!isAuthReady) return
    if (activeTab !== "bookmarks") return

    const fetchBookmarks = async () => {
      try {
        setTabLoading(true)
        setError("")
        await fetchBookmarkedPostsPage(bookmarkedPosts.page)
      } catch (err) {
        console.error(err)
        setError("북마크 목록을 불러오지 못했습니다.")
      } finally {
        setTabLoading(false)
      }
    }

    fetchBookmarks()
  }, [activeTab, isAuthReady, fetchBookmarkedPostsPage])

  useEffect(() => {
    if (!isAuthReady) return
    if (activeTab !== "likes") return

    const fetchLikes = async () => {
      try {
        setTabLoading(true)
        setError("")
        await fetchLikedPostsPage(likedPosts.page)
      } catch (err) {
        console.error(err)
        setError("좋아요 목록을 불러오지 못했습니다.")
      } finally {
        setTabLoading(false)
      }
    }

    fetchLikes()
  }, [activeTab, isAuthReady, fetchLikedPostsPage])

  useEffect(() => {
    if (!isAuthReady) return
    if (activeTab !== "comments") return

    const fetchComments = async () => {
      try {
        setTabLoading(true)
        setError("")
        await fetchMyCommentsPage(myComments.page)
      } catch (err) {
        console.error(err)
        setError("댓글 목록을 불러오지 못했습니다.")
      } finally {
        setTabLoading(false)
      }
    }

    fetchComments()
  }, [activeTab, isAuthReady, fetchMyCommentsPage])

  useEffect(() => {
    const handleNotificationsUpdated = async () => {
      if (!isAuthReady) return

      try {
        await refreshInteractionData()
      } catch (err) {
        console.error(err)
      }
    }

    window.addEventListener(
      "notifications-updated",
      handleNotificationsUpdated as EventListener
    )

    return () => {
      window.removeEventListener(
        "notifications-updated",
        handleNotificationsUpdated as EventListener
      )
    }
  }, [isAuthReady, refreshInteractionData])

  const handleBookmarkToggle = async (
    postId: number,
    nextBookmarked: boolean
  ) => {
    setBookmarkCount((prev) =>
      Math.max(0, prev + (nextBookmarked ? 1 : -1))
    )

    setMyPosts((prev) => ({
      ...prev,
      content: prev.content.map((post) =>
        post.postId === postId
          ? { ...post, bookmarked: nextBookmarked }
          : post
      ),
    }))

    setLikedPosts((prev) => ({
      ...prev,
      content: prev.content.map((post) =>
        post.postId === postId
          ? { ...post, bookmarked: nextBookmarked }
          : post
      ),
    }))

    if (activeTab === "bookmarks") {
      try {
        setTabLoading(true)

        const targetPage =
          !nextBookmarked &&
          bookmarkedPosts.page > 0 &&
          bookmarkedPosts.content.length <= 1
            ? bookmarkedPosts.page - 1
            : bookmarkedPosts.page

        await fetchBookmarkedPostsPage(targetPage)
      } catch (err) {
        console.error(err)
        setError("북마크 목록을 새로고침하지 못했습니다.")
      } finally {
        setTabLoading(false)
      }

      return
    }

    setBookmarkedPosts((prev) => ({
      ...prev,
      content: nextBookmarked
        ? prev.content.map((post) =>
            post.postId === postId
              ? { ...post, bookmarked: nextBookmarked }
              : post
          )
        : prev.content.filter((post) => post.postId !== postId),
      totalElements: Math.max(0, prev.totalElements + (nextBookmarked ? 1 : -1)),
    }))
  }

  const handleLikeToggle = async (
    postId: number,
    nextLiked: boolean,
    nextLikeCount: number
  ) => {
    setLikeCount((prev) => Math.max(0, prev + (nextLiked ? 1 : -1)))

    setMyPosts((prev) => ({
      ...prev,
      content: prev.content.map((post) =>
        post.postId === postId
          ? {
              ...post,
              liked: nextLiked,
              likeCount: nextLikeCount,
            }
          : post
      ),
    }))

    setBookmarkedPosts((prev) => ({
      ...prev,
      content: prev.content.map((post) =>
        post.postId === postId
          ? {
              ...post,
              liked: nextLiked,
              likeCount: nextLikeCount,
            }
          : post
      ),
    }))

    if (activeTab === "likes") {
      try {
        setTabLoading(true)

        const targetPage =
          !nextLiked &&
          likedPosts.page > 0 &&
          likedPosts.content.length <= 1
            ? likedPosts.page - 1
            : likedPosts.page

        await fetchLikedPostsPage(targetPage)
      } catch (err) {
        console.error(err)
        setError("좋아요 목록을 새로고침하지 못했습니다.")
      } finally {
        setTabLoading(false)
      }

      return
    }

    setLikedPosts((prev) => ({
      ...prev,
      content: nextLiked
        ? prev.content.map((post) =>
            post.postId === postId
              ? { ...post, liked: nextLiked, likeCount: nextLikeCount }
              : post
          )
        : prev.content.filter((post) => post.postId !== postId),
      totalElements: Math.max(0, prev.totalElements + (nextLiked ? 1 : -1)),
    }))
  }

  if (loading) {
    return null
  }

  if (!isAuthReady) {
    return null
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-8 rounded-lg border border-border bg-card p-6">
        <div className="flex flex-col items-start gap-6 sm:flex-row">
          <Avatar className="h-24 w-24 border-4 border-primary/20">
            <AvatarImage src={defaultUserData.avatar} alt={displayName} />
            <AvatarFallback className="bg-primary text-2xl text-primary-foreground">
              {(displayName || displayUsername || "U").slice(0, 2)}
            </AvatarFallback>
          </Avatar>

          <div className="flex-1">
            <div className="mb-4 flex flex-wrap items-center gap-4">
              <div>
                <h1 className="text-2xl font-bold text-foreground">
                  {displayName}
                </h1>
                {displayUsername ? (
                  <p className="text-sm text-muted-foreground">
                    @{displayUsername}
                  </p>
                ) : null}
              </div>

              <Link href="/mypage/edit">
                <Button variant="outline" className="gap-2">
                  <Settings className="h-4 w-4" />
                  프로필 수정
                </Button>
              </Link>
            </div>

            {profileData.bio ? (
              <p className="mb-4 leading-relaxed text-foreground">
                {profileData.bio}
              </p>
            ) : null}

            <div className="flex flex-wrap gap-4 text-sm text-muted-foreground">
              {profileData.location ? (
                <div className="flex items-center gap-1">
                  <MapPin className="h-4 w-4" />
                  {profileData.location}
                </div>
              ) : null}

              {websiteHref ? (
                <a
                  href={websiteHref}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-1 hover:text-primary"
                >
                  <LinkIcon className="h-4 w-4" />
                  {profileData.website.replace(/^https?:\/\//, "")}
                </a>
              ) : null}

              <div className="flex items-center gap-1">
                <Calendar className="h-4 w-4" />
                {formatJoinedAt(displayEmail)}
              </div>
            </div>

            {(githubHref || twitterHref) && (
              <div className="mt-4 flex gap-3">
                {githubHref ? (
                  <a
                    href={githubHref}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-muted-foreground hover:text-foreground"
                  >
                    <Github className="h-5 w-5" />
                  </a>
                ) : null}

                {twitterHref ? (
                  <a
                    href={twitterHref}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-muted-foreground hover:text-foreground"
                  >
                    <Twitter className="h-5 w-5" />
                  </a>
                ) : null}
              </div>
            )}
          </div>
        </div>

        <div className="mt-6 grid grid-cols-4 gap-4 border-t border-border pt-6">
          <div className="text-center">
            <p className="text-2xl font-bold text-foreground">
              {myPosts.totalElements}
            </p>
            <p className="text-sm text-muted-foreground">글</p>
          </div>

          <div className="text-center">
            <p className="text-2xl font-bold text-foreground">
              {bookmarkCount}
            </p>
            <p className="text-sm text-muted-foreground">북마크</p>
          </div>

          <div className="text-center">
            <p className="text-2xl font-bold text-foreground">{likeCount}</p>
            <p className="text-sm text-muted-foreground">좋아요</p>
          </div>

          <div className="text-center">
            <p className="text-2xl font-bold text-foreground">
              {commentCount}
            </p>
            <p className="text-sm text-muted-foreground">댓글</p>
          </div>
        </div>
      </div>

      {error ? (
        <div className="mb-6 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
          {error}
        </div>
      ) : null}

      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="mb-6 w-full justify-start bg-secondary">
          <TabsTrigger
            value="posts"
            className="gap-2 data-[state=active]:bg-primary data-[state=active]:text-primary-foreground"
          >
            <FileText className="h-4 w-4" />
            내 글
          </TabsTrigger>

          <TabsTrigger
            value="bookmarks"
            className="gap-2 data-[state=active]:bg-primary data-[state=active]:text-primary-foreground"
          >
            <Bookmark className="h-4 w-4" />
            북마크
          </TabsTrigger>

          <TabsTrigger
            value="likes"
            className="gap-2 data-[state=active]:bg-primary data-[state=active]:text-primary-foreground"
          >
            <Heart className="h-4 w-4" />
            좋아요
          </TabsTrigger>

          <TabsTrigger
            value="comments"
            className="gap-2 data-[state=active]:bg-primary data-[state=active]:text-primary-foreground"
          >
            <MessageCircle className="h-4 w-4" />
            댓글
          </TabsTrigger>
        </TabsList>

        <TabsContent value="posts">
          {myPostCards.length > 0 ? (
            <>
              <div className="grid gap-6">
                {myPostCards.map((post) => (
                  <PostCard
                    key={post.id}
                    post={post}
                    onLikeToggle={handleLikeToggle}
                    onBookmarkToggle={handleBookmarkToggle}
                  />
                ))}
              </div>

              <PaginationControls
                page={myPosts}
                onPrev={() => fetchMyPostsPage(myPosts.page - 1)}
                onNext={() => fetchMyPostsPage(myPosts.page + 1)}
              />
            </>
          ) : (
            <EmptyState
              icon={<FileText className="h-12 w-12" />}
              title="작성한 글이 없습니다"
              description="첫 번째 글을 작성해보세요"
              action={{ label: "글 쓰기", href: "/write" }}
            />
          )}
        </TabsContent>

        <TabsContent value="bookmarks">
          {tabLoading ? null : bookmarkedPostCards.length > 0 ? (
            <>
              <div className="grid gap-6">
                {bookmarkedPostCards.map((post) => (
                  <PostCard
                    key={post.id}
                    post={post}
                    onLikeToggle={handleLikeToggle}
                    onBookmarkToggle={handleBookmarkToggle}
                  />
                ))}
              </div>

              <PaginationControls
                page={bookmarkedPosts}
                onPrev={() =>
                  fetchBookmarkedPostsPage(bookmarkedPosts.page - 1)
                }
                onNext={() =>
                  fetchBookmarkedPostsPage(bookmarkedPosts.page + 1)
                }
              />
            </>
          ) : (
            <EmptyState
              icon={<Bookmark className="h-12 w-12" />}
              title="북마크한 글이 없습니다"
              description="관심 있는 글을 북마크해보세요"
            />
          )}
        </TabsContent>

        <TabsContent value="likes">
          {tabLoading ? null : likedPostCards.length > 0 ? (
            <>
              <div className="grid gap-6">
                {likedPostCards.map((post) => (
                  <PostCard
                    key={post.id}
                    post={post}
                    onLikeToggle={handleLikeToggle}
                    onBookmarkToggle={handleBookmarkToggle}
                  />
                ))}
              </div>

              <PaginationControls
                page={likedPosts}
                onPrev={() => fetchLikedPostsPage(likedPosts.page - 1)}
                onNext={() => fetchLikedPostsPage(likedPosts.page + 1)}
              />
            </>
          ) : (
            <EmptyState
              icon={<Heart className="h-12 w-12" />}
              title="좋아요한 글이 없습니다"
              description="마음에 드는 글에 좋아요를 눌러보세요"
            />
          )}
        </TabsContent>

        <TabsContent value="comments">
          {tabLoading ? null : myComments.content.length > 0 ? (
            <>
              <div className="grid gap-4">
                {myComments.content.map((comment) => (
                  <Link
                    key={comment.commentId}
                    href={`/posts/${comment.postId}#comment-${comment.commentId}`}
                    className="block rounded-lg border border-border bg-card p-5 transition-colors hover:border-primary/50 hover:bg-card/80"
                  >
                    <div className="mb-3 flex items-start justify-between gap-4">
                      <div className="min-w-0">
                        <p className="mb-2 inline-flex items-center gap-2 rounded-md bg-primary/10 px-2 py-1 text-xs font-medium text-primary">
                          <MessageCircle className="h-3.5 w-3.5" />
                          내가 작성한 댓글
                        </p>

                        <h3 className="truncate text-base font-semibold text-foreground">
                          {comment.postTitle}
                        </h3>
                      </div>

                      <span className="shrink-0 text-xs text-muted-foreground">
                        {formatRelativeDate(comment.createdAt)}
                      </span>
                    </div>

                    <p className="line-clamp-2 text-sm text-muted-foreground">
                      {comment.content}
                    </p>
                  </Link>
                ))}
              </div>

              <PaginationControls
                page={myComments}
                onPrev={() => fetchMyCommentsPage(myComments.page - 1)}
                onNext={() => fetchMyCommentsPage(myComments.page + 1)}
              />
            </>
          ) : (
            <EmptyState
              icon={<MessageCircle className="h-12 w-12" />}
              title="작성한 댓글이 없습니다"
              description="글에 댓글을 남겨 소통해보세요"
            />
          )}
        </TabsContent>
      </Tabs>
    </div>
  )
}

function PaginationControls({
  page,
  onPrev,
  onNext,
}: {
  page: PageResponse<unknown>
  onPrev: () => void
  onNext: () => void
}) {
  return (
    <div className="mt-6 flex items-center justify-center gap-3">
      <Button
        variant="outline"
        disabled={page.first || page.page <= 0}
        onClick={onPrev}
      >
        이전
      </Button>

      <span className="text-sm text-muted-foreground">
        {page.page + 1} / {Math.max(page.totalPages, 1)}
        <span className="ml-2 text-xs text-muted-foreground">
          총 {page.totalElements}개
        </span>
      </span>

      <Button
        variant="outline"
        disabled={page.last || !page.hasNext}
        onClick={onNext}
      >
        다음
      </Button>
    </div>
  )
}

function EmptyState({
  icon,
  title,
  description,
  action,
}: {
  icon: React.ReactNode
  title: string
  description: string
  action?: { label: string; href: string }
}) {
  return (
    <div className="rounded-lg border border-border bg-card p-12 text-center">
      <div className="mx-auto mb-4 text-muted-foreground">{icon}</div>
      <h3 className="mb-2 text-lg font-semibold text-foreground">{title}</h3>
      <p className="text-sm text-muted-foreground">{description}</p>

      {action ? (
        <Link href={action.href}>
          <Button className="mt-4 bg-primary text-primary-foreground hover:bg-primary/90">
            {action.label}
          </Button>
        </Link>
      ) : null}
    </div>
  )
}