import Link from "next/link"
import { MessageCircle, Eye } from "lucide-react"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import InteractionButtons from "@/components/interaction-buttons"

export interface Post {
  id: string
  title: string
  excerpt: string
  author: {
    name: string
    avatar?: string
    userId?: number
  }
  category: string
  categorySlug: string
  categoryId: number
  createdAt: string
  likes: number
  comments: number
  views: number
  tags: string[]
  liked?: boolean
  bookmarked?: boolean
}

interface PostCardProps {
  post: Post
  onLikeToggle?: (postId: number, nextLiked: boolean, nextLikeCount: number) => void
  onBookmarkToggle?: (postId: number, nextBookmarked: boolean) => void
}

const toPlainText = (value: string) =>
  value
    .replace(/<style[\s\S]*?>[\s\S]*?<\/style>/gi, " ")
    .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, " ")
    .replace(/<[^>]+>/g, " ")
    .replace(/&nbsp;/gi, " ")
    .replace(/&amp;/gi, "&")
    .replace(/&lt;/gi, "<")
    .replace(/&gt;/gi, ">")
    .replace(/&quot;/gi, '"')
    .replace(/&#39;/gi, "'")
    .replace(/\s+/g, " ")
    .trim()

export function PostCard({ post, onLikeToggle, onBookmarkToggle }: PostCardProps) {
  const authorProfileHref = post.author.userId
    ? `/users/${post.author.userId}`
    : undefined

  const excerptText = toPlainText(post.excerpt)

  return (
    <article className="group overflow-hidden rounded-lg border border-border bg-card p-6 transition-all hover:border-primary/50 hover:bg-card/80">
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0 flex-1">
          <div className="mb-2 flex items-center gap-2">
            <Link
              href={`/category/${post.categorySlug}`}
              className="rounded-md bg-primary/10 px-2 py-1 text-xs font-medium text-primary transition-colors hover:bg-primary/20"
            >
              {post.category}
            </Link>
            <span className="text-xs text-muted-foreground">
              {post.createdAt}
            </span>
          </div>

          <Link href={`/posts/${post.id}`}>
            <h3 className="mb-2 break-words text-lg font-semibold text-foreground transition-colors group-hover:text-primary">
              {post.title}
            </h3>
          </Link>

          <p className="mb-4 line-clamp-2 overflow-hidden break-all text-sm leading-relaxed text-muted-foreground">
            {excerptText}
          </p>

          <div className="flex items-center justify-between">
            {authorProfileHref ? (
              <Link href={authorProfileHref} className="flex items-center gap-2">
                <Avatar className="h-10 w-10">
                  <AvatarImage src={post.author.avatar} alt={post.author.name} />
                  <AvatarFallback className="bg-secondary text-xs text-secondary-foreground">
                    작성자
                  </AvatarFallback>
                </Avatar>
                <span className="text-sm text-muted-foreground transition-colors hover:text-foreground">
                  {post.author.name}
                </span>
              </Link>
            ) : (
              <div className="flex items-center gap-2">
                <Avatar className="h-10 w-10">
                  <AvatarImage src={post.author.avatar} alt={post.author.name} />
                  <AvatarFallback className="bg-secondary text-xs text-secondary-foreground">
                    작성자
                  </AvatarFallback>
                </Avatar>
                <span className="text-sm text-muted-foreground">
                  {post.author.name}
                </span>
              </div>
            )}
            <div className="flex items-center gap-4 text-muted-foreground">
              <div className="flex items-center gap-1">
                <MessageCircle className="h-4 w-4" />
                <span className="text-xs">{post.comments}</span>
              </div>
              <div className="flex items-center gap-1">
                <Eye className="h-4 w-4" />
                <span className="text-xs">{post.views}</span>
              </div>
            </div>
          </div>
        </div>

        <InteractionButtons
          postId={Number(post.id)}
          initialLiked={post.liked ?? false}
          initialBookmarked={post.bookmarked ?? false}
          initialLikeCount={post.likes ?? 0}
          onLikeToggle={(nextLiked, nextLikeCount) =>
            onLikeToggle?.(Number(post.id), nextLiked, nextLikeCount)
          }
          onBookmarkToggle={(nextBookmarked) =>
            onBookmarkToggle?.(Number(post.id), nextBookmarked)
          }
        />
      </div>
    </article>
  )
}