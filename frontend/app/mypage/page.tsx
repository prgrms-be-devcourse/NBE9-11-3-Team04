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
}

type MyPostResponse = {
  postId: number
  title: string
  likeCount: number
  commentCount: number
  viewCount: number
  createdAt: string
}

type MyCommentItem = {
  commentId: number
  postId: number
  postTitle: string
  content: string
  createdAt: string
}

type MyCommentsResponse = {
  comments: MyCommentItem[]
}

type LikedPostResponse = {
  postId: number
  title: string
  authorNickname: string
  likeCount: number
  commentCount: number
  viewCount: number
  createdAt: string
}

type BookmarkedPostResponse = {
  postId: number
  title: string
  authorNickname: string
  likeCount: number
  commentCount: number
  viewCount: number
  createdAt: string
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
  displayName: string,
  likedPostIds: Set<number>,
  bookmarkedPostIds: Set<number>
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
    liked: likedPostIds.has(post.postId),
    bookmarked: bookmarkedPostIds.has(post.postId),
  }))
}

function mapBookmarkedPostsToPostCard(
  posts: BookmarkedPostResponse[],
  likedPostIds: Set<number>,
  bookmarkedPostIds: Set<number>
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
    liked: likedPostIds.has(post.postId),
    bookmarked: bookmarkedPostIds.has(post.postId),
  }))
}

function mapLikedPostsToPostCard(
  posts: LikedPostResponse[],
  likedPostIds: Set<number>,
  bookmarkedPostIds: Set<number>
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
    liked: likedPostIds.has(post.postId),
    bookmarked: bookmarkedPostIds.has(post.postId),
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

  const [profileData, setProfileData] = useState<LocalProfileData>(emptyProfileData)
  const [myPosts, setMyPosts] = useState<MyPostResponse[]>([])
  const [bookmarkedPosts, setBookmarkedPosts] = useState<BookmarkedPostResponse[]>([])
  const [likedPosts, setLikedPosts] = useState<LikedPostResponse[]>([])
  const [myComments, setMyComments] = useState<MyCommentItem[]>([])

  const [bookmarkCount, setBookmarkCount] = useState(0)
  const [likeCount, setLikeCount] = useState(0)
  const [commentCount, setCommentCount] = useState(0)

  const [myLikedPostIds, setMyLikedPostIds] = useState<Set<number>>(new Set())
  const [myBookmarkedPostIds, setMyBookmarkedPostIds] = useState<Set<number>>(new Set())

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
    () =>
      mapMyPostsToPostCard(
        myPosts,
        displayName,
        myLikedPostIds,
        myBookmarkedPostIds
      ),
    [myPosts, displayName, myLikedPostIds, myBookmarkedPostIds]
  )

  const bookmarkedPostCards = useMemo(
    () =>
      mapBookmarkedPostsToPostCard(
        bookmarkedPosts,
        myLikedPostIds,
        myBookmarkedPostIds
      ),
    [bookmarkedPosts, myLikedPostIds, myBookmarkedPostIds]
  )

  const likedPostCards = useMemo(
    () =>
      mapLikedPostsToPostCard(
        likedPosts,
        myLikedPostIds,
        myBookmarkedPostIds
      ),
    [likedPosts, myLikedPostIds, myBookmarkedPostIds]
  )

  const refreshInteractionData = useCallback(async () => {
    const [bookmarksRes, likesRes, commentsRes] = await Promise.allSettled([
      apiFetch<SuccessResponse<BookmarkedPostResponse[]>>("/api/mypage/bookmarks", {
        method: "GET",
        auth: true,
      }),
      apiFetch<SuccessResponse<LikedPostResponse[]>>("/api/mypage/likes", {
        method: "GET",
        auth: true,
      }),
      apiFetch<SuccessResponse<MyCommentsResponse>>("/api/mypage/comments", {
        method: "GET",
        auth: true,
      }),
    ])

    if (bookmarksRes.status === "fulfilled") {
      const bookmarks = bookmarksRes.value?.data ?? []
      setBookmarkedPosts(bookmarks)
      setBookmarkCount(bookmarks.length)
      setMyBookmarkedPostIds(new Set(bookmarks.map((post) => post.postId)))
    }

    if (likesRes.status === "fulfilled") {
      const likes = likesRes.value?.data ?? []
      setLikedPosts(likes)
      setLikeCount(likes.length)
      setMyLikedPostIds(new Set(likes.map((post) => post.postId)))
    }

    if (commentsRes.status === "fulfilled") {
      const comments = commentsRes.value?.data?.comments ?? []
      setMyComments(comments)
      setCommentCount(comments.length)
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
          apiFetch<SuccessResponse<MyPostResponse[]>>("/api/mypage/posts", {
            method: "GET",
            auth: true,
          }),
        ])

        const profile = profileRes?.data
        const posts = postsRes?.data ?? []

        const nextName = profile?.nickname?.trim() || "김개발"
        const nextEmail = profile?.email?.trim() || ""
        const nextUsername = nextEmail
          ? nextEmail.split("@")[0]
          : nextName.replace(/\s+/g, "")

        setDisplayName(nextName)
        setDisplayEmail(nextEmail)
        setDisplayUsername(nextUsername)
        setMyPosts(posts)

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
        profile?.email?.trim() ||
        auth.email?.trim() ||
        displayEmail ||
        ""

      const usernameFromProfile = profile?.username?.trim()
      const usernameFromEmail = email ? email.split("@")[0] : ""
      const usernameFromName = nickname.trim().replace(/\s+/g, "")
      const username =
        usernameFromProfile ||
        usernameFromEmail ||
        usernameFromName ||
        "kimdev"

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
      window.removeEventListener(AUTH_CHANGED_EVENT, syncProfile as EventListener)
      window.removeEventListener("storage", syncProfile)
    }
  }, [isAuthReady, displayName, displayEmail])

  useEffect(() => {
    if (!isAuthReady) return
    if (activeTab !== "bookmarks" || bookmarkedPosts.length > 0) return

    const fetchBookmarks = async () => {
      try {
        setTabLoading(true)
        setError("")
        await refreshInteractionData()
      } catch (err) {
        console.error(err)
        setError("북마크 목록을 불러오지 못했습니다.")
      } finally {
        setTabLoading(false)
      }
    }

    fetchBookmarks()
  }, [activeTab, bookmarkedPosts.length, isAuthReady, refreshInteractionData])

  useEffect(() => {
    if (!isAuthReady) return
    if (activeTab !== "likes" || likedPosts.length > 0) return

    const fetchLikes = async () => {
      try {
        setTabLoading(true)
        setError("")
        await refreshInteractionData()
      } catch (err) {
        console.error(err)
        setError("좋아요 목록을 불러오지 못했습니다.")
      } finally {
        setTabLoading(false)
      }
    }

    fetchLikes()
  }, [activeTab, likedPosts.length, isAuthReady, refreshInteractionData])

  useEffect(() => {
    if (!isAuthReady) return
    if (activeTab !== "comments" || myComments.length > 0) return

    const fetchComments = async () => {
      try {
        setTabLoading(true)
        setError("")

        const res = await apiFetch<SuccessResponse<MyCommentsResponse>>("/api/mypage/comments", {
          method: "GET",
          auth: true,
        })

        const comments = res?.data?.comments ?? []
        setMyComments(comments)
        setCommentCount(comments.length)
      } catch (err) {
        console.error(err)
        setError("댓글 목록을 불러오지 못했습니다.")
      } finally {
        setTabLoading(false)
      }
    }

    fetchComments()
  }, [activeTab, myComments.length, isAuthReady])

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

  const handleBookmarkToggle = async (postId: number, nextBookmarked: boolean) => {
    setMyBookmarkedPostIds((prev) => {
      const next = new Set(prev)

      if (nextBookmarked) {
        next.add(postId)
      } else {
        next.delete(postId)
      }

      return next
    })

    setBookmarkCount((prev) => Math.max(0, prev + (nextBookmarked ? 1 : -1)))

    if (!nextBookmarked && activeTab === "bookmarks") {
      setBookmarkedPosts((prev) => prev.filter((post) => post.postId !== postId))
    }

    try {
      await refreshInteractionData()
    } catch (err) {
      console.error(err)
    }
  }

  const handleLikeToggle = async (
    postId: number,
    nextLiked: boolean,
    nextLikeCount: number
  ) => {
    setMyLikedPostIds((prev) => {
      const next = new Set(prev)

      if (nextLiked) {
        next.add(postId)
      } else {
        next.delete(postId)
      }

      return next
    })

    setLikeCount((prev) => Math.max(0, prev + (nextLiked ? 1 : -1)))

    setMyPosts((prev) =>
      prev.map((post) =>
        post.postId === postId
          ? { ...post, likeCount: nextLikeCount }
          : post
      )
    )

    setBookmarkedPosts((prev) =>
      prev.map((post) =>
        post.postId === postId
          ? { ...post, likeCount: nextLikeCount }
          : post
      )
    )

    if (!nextLiked && activeTab === "likes") {
      setLikedPosts((prev) => prev.filter((post) => post.postId !== postId))
    } else {
      setLikedPosts((prev) =>
        prev.map((post) =>
          post.postId === postId
            ? { ...post, likeCount: nextLikeCount }
            : post
        )
      )
    }

    try {
      await refreshInteractionData()
    } catch (err) {
      console.error(err)
    }
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
                <h1 className="text-2xl font-bold text-foreground">{displayName}</h1>
                {displayUsername ? (
                  <p className="text-sm text-muted-foreground">@{displayUsername}</p>
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
              <p className="mb-4 leading-relaxed text-foreground">{profileData.bio}</p>
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
            <p className="text-2xl font-bold text-foreground">{myPosts.length}</p>
            <p className="text-sm text-muted-foreground">글</p>
          </div>
          <div className="text-center">
            <p className="text-2xl font-bold text-foreground">{bookmarkCount}</p>
            <p className="text-sm text-muted-foreground">북마크</p>
          </div>
          <div className="text-center">
            <p className="text-2xl font-bold text-foreground">{likeCount}</p>
            <p className="text-sm text-muted-foreground">좋아요</p>
          </div>
          <div className="text-center">
            <p className="text-2xl font-bold text-foreground">{commentCount}</p>
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
          ) : (
            <EmptyState
              icon={<Heart className="h-12 w-12" />}
              title="좋아요한 글이 없습니다"
              description="마음에 드는 글에 좋아요를 눌러보세요"
            />
          )}
        </TabsContent>

        <TabsContent value="comments">
          {tabLoading ? null : myComments.length > 0 ? (
            <div className="grid gap-4">
              {myComments.map((comment) => (
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