export const AUTH_TOKEN_KEY = "accessToken"
export const AUTH_NICKNAME_KEY = "nickname"
export const AUTH_EMAIL_KEY = "email"
export const AUTH_CHANGED_EVENT = "auth-changed"
export const AUTH_PROFILES_KEY = "userProfiles"
export const OAUTH_COOKIE_SESSION_TOKEN = "oauth-cookie-session"

export type AuthSnapshot = {
  token: string | null
  nickname: string | null
  email: string | null
  isLoggedIn: boolean
}

export type UserProfile = {
  email: string
  nickname: string
  username: string
  bio: string
  location: string
  website: string
  github: string
  twitter: string
}

type UserProfileMap = Record<string, UserProfile>

export function sanitizeAccessToken(token: string | null | undefined): string | null {
  if (!token) return null

  const trimmedToken = token.trim()
  if (!trimmedToken || trimmedToken === OAUTH_COOKIE_SESSION_TOKEN) {
    return null
  }

  return trimmedToken
}

export function getAuthSnapshot(): AuthSnapshot {
  if (typeof window === "undefined") {
    return { token: null, nickname: null, email: null, isLoggedIn: false }
  }

  const token = sanitizeAccessToken(localStorage.getItem(AUTH_TOKEN_KEY))
  const nickname = localStorage.getItem(AUTH_NICKNAME_KEY)
  const email = localStorage.getItem(AUTH_EMAIL_KEY)

  return {
    token,
    nickname,
    email,
    // 핵심 변경: 이메일 존재 여부가 아니라 "유효한 토큰 존재"로 로그인 판정
    isLoggedIn: Boolean(token),
  }
}

export function getAccessToken(): string | null {
  if (typeof window === "undefined") {
    return null
  }

  return sanitizeAccessToken(localStorage.getItem(AUTH_TOKEN_KEY))
}

function notifyAuthChanged() {
  if (typeof window === "undefined") return
  window.dispatchEvent(new CustomEvent(AUTH_CHANGED_EVENT))
}

function readProfileMap(): UserProfileMap {
  if (typeof window === "undefined") return {}

  const raw = localStorage.getItem(AUTH_PROFILES_KEY)
  if (!raw) return {}

  try {
    return JSON.parse(raw) as UserProfileMap
  } catch {
    return {}
  }
}

function writeProfileMap(profileMap: UserProfileMap) {
  if (typeof window === "undefined") return
  localStorage.setItem(AUTH_PROFILES_KEY, JSON.stringify(profileMap))
}

/**
 * accessToken:
 * - string 전달: 그 값으로 갱신
 * - null 전달: 토큰 제거
 * - undefined 전달: 기존 토큰 유지
 */
export function persistLoginSession(
  accessToken?: string | null,
  nickname?: string | null,
  email?: string | null
) {
  if (typeof window === "undefined") return

  if (accessToken !== undefined) {
    if (accessToken === OAUTH_COOKIE_SESSION_TOKEN) {
      // Placeholder value must not overwrite or clear an existing access token.
    } else {
      const normalizedToken = sanitizeAccessToken(accessToken)
      if (normalizedToken) {
        localStorage.setItem(AUTH_TOKEN_KEY, normalizedToken)
      } else {
        localStorage.removeItem(AUTH_TOKEN_KEY)
      }
    }
  }

  if (nickname !== undefined) {
    if (nickname && nickname.trim().length > 0) {
      localStorage.setItem(AUTH_NICKNAME_KEY, nickname.trim())
    } else {
      localStorage.removeItem(AUTH_NICKNAME_KEY)
    }
  }

  if (email !== undefined) {
    if (email && email.trim().length > 0) {
      localStorage.setItem(AUTH_EMAIL_KEY, email.trim())
    } else {
      localStorage.removeItem(AUTH_EMAIL_KEY)
    }
  }

  const normalizedEmail = email?.trim()
  const normalizedNickname = nickname?.trim()

  if (normalizedEmail && normalizedNickname) {
    const profileMap = readProfileMap()
    const prev = profileMap[normalizedEmail]
    const username = prev?.username || normalizedEmail.split("@")[0] || normalizedNickname

    profileMap[normalizedEmail] = {
      email: normalizedEmail,
      nickname: normalizedNickname,
      username,
      bio: prev?.bio || "",
      location: prev?.location || "",
      website: prev?.website || "",
      github: prev?.github || "",
      twitter: prev?.twitter || "",
    }

    writeProfileMap(profileMap)
  }

  notifyAuthChanged()
}

export function getCurrentUserProfile(): UserProfile | null {
  const auth = getAuthSnapshot()
  const email = auth.email?.trim()
  if (!email) return null
  return readProfileMap()[email] ?? null
}

export function saveCurrentUserProfile(nextProfile: UserProfile): void {
  if (typeof window === "undefined") return

  const auth = getAuthSnapshot()
  const prevEmail = auth.email?.trim()
  const nextEmail = nextProfile.email.trim()
  const profileMap = readProfileMap()

  if (prevEmail && prevEmail !== nextEmail) {
    delete profileMap[prevEmail]
  }

  profileMap[nextEmail] = {
    ...nextProfile,
    email: nextEmail,
    nickname: nextProfile.nickname.trim(),
    username: nextProfile.username.trim(),
    bio: nextProfile.bio,
    location: nextProfile.location,
    website: nextProfile.website,
    github: nextProfile.github,
    twitter: nextProfile.twitter,
  }

  writeProfileMap(profileMap)

  persistLoginSession(undefined, nextProfile.nickname, nextEmail)
}

export function clearLoginSession() {
  if (typeof window === "undefined") return

  localStorage.removeItem(AUTH_TOKEN_KEY)
  localStorage.removeItem(AUTH_NICKNAME_KEY)
  localStorage.removeItem(AUTH_EMAIL_KEY)
  notifyAuthChanged()
}
