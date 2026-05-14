"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { MessageCircle } from "lucide-react";
import { apiFetch } from "@/lib/api";
import { getAuthSnapshot } from "@/lib/auth-storage";

type SuccessResponse<T> = {
  code: string;
  message: string;
  timestamp: string;
  data: T;
};

type MyCommentItem = {
  commentId: number;
  postId: number;
  postTitle: string;
  content: string;
  createdAt: string;
};

type MyCommentsApiResponse = {
  comments: MyCommentItem[];
};

function formatDate(value: string) {
  if (!value) return "";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

export default function MyCommentsPage() {
  const router = useRouter();
  const [comments, setComments] = useState<MyCommentItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const auth = getAuthSnapshot();

    if (!auth.token) {
      router.replace("/login");
      return;
    }

    const fetchComments = async () => {
      try {
        setLoading(true);
        setError("");

        const res = await apiFetch<SuccessResponse<MyCommentsApiResponse>>(
          "/api/mypage/comments",
          {
            method: "GET",
            auth: true,
          }
        );

        setComments(res?.data?.comments ?? []);
      } catch (err) {
        console.error("댓글 조회 실패:", err);

        if (err instanceof Error && err.message === "UNAUTHORIZED") {
          router.replace("/login");
          return;
        }

        setError("내 댓글을 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    };

    void fetchComments();
  }, [router]);

  return (
    <main className="mx-auto max-w-4xl px-4 py-10">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold">내가 쓴 댓글</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            내가 작성한 댓글 목록입니다.
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

      {!loading && !error && comments.length === 0 && (
        <div className="rounded-2xl border p-6">작성한 댓글이 없습니다.</div>
      )}

      <div className="space-y-4">
        {!loading &&
          !error &&
          comments.map((comment) => (
            <Link
              key={comment.commentId}
              href={`/posts/${comment.postId}#comment-${comment.commentId}`}
              className="block rounded-2xl border p-5 transition-colors hover:border-primary/50 hover:bg-card/60"
            >
              <div className="mb-3 flex items-start justify-between gap-4">
                <div className="min-w-0">
                  <p className="mb-2 inline-flex items-center gap-2 rounded-md bg-primary/10 px-2 py-1 text-xs font-medium text-primary">
                    <MessageCircle className="h-3.5 w-3.5" />
                    내가 작성한 댓글
                  </p>

                  <h2 className="truncate text-base font-semibold text-foreground">
                    {comment.postTitle || `게시글 #${comment.postId}`}
                  </h2>
                </div>

                <span className="shrink-0 text-xs text-muted-foreground">
                  {formatDate(comment.createdAt)}
                </span>
              </div>

              <p className="mb-3 line-clamp-2 text-sm text-muted-foreground">
                {comment.content}
              </p>

              <p className="text-xs text-muted-foreground">
                클릭하면 해당 게시글로 이동합니다
              </p>
            </Link>
          ))}
      </div>
    </main>
  );
}