import { apiFetch } from "./api"
import { persistLoginSession } from "./auth-storage"

type SuccessResponse<T> = {
  code: string
  message: string
  timestamp: string
  data: T
}

export type LoginRequest = {
  email: string
  password: string
}

export type OAuthExchangeRequest = {
  code: string
}

export type OAuthSignupCompleteRequest = {
  nickname: string
}

export type LoginData = {
  userId: number
  email: string
  nickname: string
  role: string
  status: string
  accessToken: string
}

export type SignUpRequest = {
  email: string
  password: string
  nickname: string
}

export type SignUpData = {
  userId: number
  email: string
  nickname: string
  role: string
  status: string
}

export async function login(body: LoginRequest): Promise<LoginData> {
  const res = await apiFetch<SuccessResponse<LoginData>>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(body),
  })

  const data = res.data

  // 🔥 디버깅 (이거 꼭 확인)
  console.log("🔥 login data:", data)
  console.log("🔥 accessToken:", data.accessToken)

  // 🔥 토큰 저장
  persistLoginSession(
    data.accessToken,
    data.nickname,
    data.email
  )

  // 🔥 저장 확인
  console.log("🔥 저장된 토큰:", localStorage.getItem("accessToken"))

  return data
}

export async function signup(body: SignUpRequest): Promise<SignUpData> {
  const res = await apiFetch<SuccessResponse<SignUpData>>("/api/auth/signup", {
    method: "POST",
    body: JSON.stringify(body),
  })

  return res.data
}

export async function exchangeOAuthCode(
  body: OAuthExchangeRequest
): Promise<LoginData> {
  const res = await apiFetch<SuccessResponse<LoginData>>(
    "/api/auth/oauth2/exchange",
    {
      method: "POST",
      body: JSON.stringify(body),
    }
  )

  const data = res.data

  console.log("🔥 oauth login data:", data)

  persistLoginSession(
    data.accessToken,
    data.nickname,
    data.email
  )

  return data
}

export async function completeOAuthSignup(
  body: OAuthSignupCompleteRequest
): Promise<LoginData> {
  const res = await apiFetch<SuccessResponse<LoginData>>(
    "/api/auth/oauth2/signup/complete",
    {
      method: "POST",
      body: JSON.stringify(body),
      auth: true,
    }
  )

  const data = res.data

  console.log("🔥 oauth signup complete:", data)

  persistLoginSession(
    data.accessToken,
    data.nickname,
    data.email
  )

  return data
}