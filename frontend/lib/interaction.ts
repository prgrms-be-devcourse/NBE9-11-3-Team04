import { apiFetch } from "./api"

export type LikeResponse = {
  postId: number
  liked: boolean
  likeCount: number
}

export type BookmarkResponse = {
  postId: number
  bookmarked: boolean
}

export async function likePost(postId: number): Promise<LikeResponse> {
  return apiFetch<LikeResponse>(`/api/posts/${postId}/likes`, {
    method: "POST",
    auth: true,
  })
}

export async function unlikePost(postId: number): Promise<LikeResponse> {
  return apiFetch<LikeResponse>(`/api/posts/${postId}/likes`, {
    method: "DELETE",
    auth: true,
  })
}

export async function bookmarkPost(postId: number): Promise<BookmarkResponse> {
  return apiFetch<BookmarkResponse>(`/api/posts/${postId}/bookmarks`, {
    method: "POST",
    auth: true,
  })
}

export async function unbookmarkPost(postId: number): Promise<BookmarkResponse> {
  return apiFetch<BookmarkResponse>(`/api/posts/${postId}/bookmarks`, {
    method: "DELETE",
    auth: true,
  })
}

export async function toggleLike(
  postId: number,
  liked: boolean
): Promise<LikeResponse> {
  return liked ? unlikePost(postId) : likePost(postId)
}

export async function toggleBookmark(
  postId: number,
  bookmarked: boolean
): Promise<BookmarkResponse> {
  return bookmarked ? unbookmarkPost(postId) : bookmarkPost(postId)
}
