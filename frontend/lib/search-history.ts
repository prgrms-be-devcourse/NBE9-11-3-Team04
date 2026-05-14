const SEARCH_HISTORY_KEY = "searchHistory"
const MAX_SEARCH_HISTORY = 10

export function getSearchHistory(): string[] {
  if (typeof window === "undefined") return []

  const raw = localStorage.getItem(SEARCH_HISTORY_KEY)
  if (!raw) return []

  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []

    return parsed.filter(
      (item): item is string => typeof item === "string" && item.trim().length > 0
    )
  } catch {
    return []
  }
}

export function saveSearchHistory(keyword: string): string[] {
  if (typeof window === "undefined") return []

  const trimmed = keyword.trim()
  if (!trimmed) return getSearchHistory()

  const prev = getSearchHistory()
  const next = [
    trimmed,
    ...prev.filter((item) => item.toLowerCase() !== trimmed.toLowerCase()),
  ].slice(0, MAX_SEARCH_HISTORY)

  localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(next))
  return next
}

export function removeSearchHistory(keyword: string): string[] {
  if (typeof window === "undefined") return []

  const next = getSearchHistory().filter(
    (item) => item.toLowerCase() !== keyword.trim().toLowerCase()
  )

  localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(next))
  return next
}

export function clearSearchHistory(): void {
  if (typeof window === "undefined") return
  localStorage.removeItem(SEARCH_HISTORY_KEY)
}