"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import { getAuthSnapshot } from "@/lib/auth-storage";

type SuccessResponse<T> = {
  code: string;
  message: string;
  timestamp: string;
  data: T;
};

type BookmarkedPostResponse = {
  postId: number;
  title: string;
  authorNickname: string;
  likeCount: number;
  commentCount: number;
  viewCount: number;
  createdAt: string;
};

export default function MyBookmarksPage() {
  const router = useRouter();
  const [bookmarks, setBookmarks] = useState<BookmarkedPostResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const auth = getAuthSnapshot();

    if (!auth.token) {
      router.replace("/login");
      return;
    }

    const fetchBookmarks = async () => {
      try {
        setLoading(true);
        setError("");

        const res = await apiFetch<SuccessResponse<BookmarkedPostResponse[]>>(
          "/api/mypage/bookmarks",
          {
            method: "GET",
            auth: true,
          }
        );

        setBookmarks(res?.data ?? []);
      } catch (err) {
        if (err instanceof Error && err.message === "UNAUTHORIZED") {
          router.replace("/login");
          return;
        }

        console.error(err);
        setError("북마크 목록을 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    };

    void fetchBookmarks();
  }, [router]);

  return (
    <main className="mx-auto max-w-4xl px-4 py-10">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">북마크 목록</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            내가 북마크한 게시글 목록입니다.
          </p>
        </div>

        <Link href="/mypage" className="rounded-lg border px-4 py-2 text-sm">
          마이페이지로
        </Link>
      </div>

      {loading && <div className="rounded-2xl border p-6">로딩 중...</div>}

      {!loading && error && (
        <div className="rounded-2xl border p-6">
          <p className="text-sm text-red-500">{error}</p>
        </div>
      )}

      {!loading && !error && bookmarks.length === 0 && (
        <div className="rounded-2xl border p-6">북마크한 글이 없습니다.</div>
      )}

      <div className="space-y-4">
        {!loading &&
          !error &&
          bookmarks.map((post) => (
            <div key={post.postId} className="rounded-2xl border p-5">
              <div className="mb-3 flex items-center justify-between gap-4">
                <h2 className="text-lg font-semibold">{post.title}</h2>
                <span className="shrink-0 text-xs text-muted-foreground">
                  {post.createdAt}
                </span>
              </div>

              <p className="mb-3 text-sm text-muted-foreground">
                작성자: {post.authorNickname}
              </p>

              <div className="flex gap-4 text-sm text-muted-foreground">
                <span>좋아요 {post.likeCount}</span>
                <span>댓글 {post.commentCount}</span>
                <span>조회수 {post.viewCount}</span>
              </div>
            </div>
          ))}
      </div>
    </main>
  );
}