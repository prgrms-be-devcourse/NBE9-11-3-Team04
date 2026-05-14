import { getAccessToken, sanitizeAccessToken } from "./auth-storage"

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL

type ErrorResponse = {
  code?: string
  message?: string
  validation?: Record<string, string>
}

type RequestOptions = RequestInit & {
  auth?: boolean
  token?: string | null
  retryOnUnauthorized?: boolean
}

const ERROR_MESSAGE_MAP: Record<string, string> = {
  MYPAGE_409_NICKNAME_ALREADY_EXISTS: "중복된 이름입니다.",
}

export class ApiError extends Error {
  status: number
  code?: string
  validation?: Record<string, string>

  constructor(
    message: string,
    status: number,
    options?: { code?: string; validation?: Record<string, string> }
  ) {
    super(message)
    this.name = "ApiError"
    this.status = status
    this.code = options?.code
    this.validation = options?.validation
  }

  get isUnauthorized() {
    return this.status === 401
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}

function isSafeMethod(method?: string) {
  const normalized = (method ?? "GET").toUpperCase()
  return normalized === "GET" || normalized === "HEAD" || normalized === "OPTIONS"
}

function buildHeaders(
  rest: Omit<RequestInit, "headers">,
  headers: HeadersInit | undefined,
  auth: boolean,
  accessToken: string | null
): Headers {
  const finalHeaders = new Headers()

  if (!(rest.body instanceof FormData)) {
    finalHeaders.set("Content-Type", "application/json")
  }

  if (headers) {
    const incoming = new Headers(headers)
    incoming.forEach((value, key) => {
      finalHeaders.set(key, value)
    })
  }

  if (auth && accessToken) {
    finalHeaders.set("Authorization", `Bearer ${accessToken}`)
  }

  return finalHeaders
}

async function performFetch(path: string, requestInit: RequestInit): Promise<Response> {
  return fetch(`${API_BASE_URL}${path}`, {
    ...requestInit,
    credentials: "include",
  })
}

function getUserFriendlyErrorMessage(
  err: ErrorResponse | null,
  status: number
): string {
  const validationMessage = err?.validation
    ? Object.values(err.validation)[0]
    : undefined

  const errorKey = err?.code || err?.message || ""

  if (errorKey && ERROR_MESSAGE_MAP[errorKey]) {
    return ERROR_MESSAGE_MAP[errorKey]
  }

  if (validationMessage) {
    return validationMessage
  }

  if (err?.message) {
    return err.message
  }

  return `API request failed (${status})`
}

export async function apiFetch<T>(
  path: string,
  options: RequestOptions = {}
): Promise<T> {
  if (!API_BASE_URL) {
    throw new ApiError("NEXT_PUBLIC_API_BASE_URL is not set", 500)
  }

  const {
    auth = false,
    token,
    headers,
    retryOnUnauthorized = true,
    ...rest
  } = options

  let accessToken =
    token !== undefined ? sanitizeAccessToken(token) : getAccessToken()

  let finalHeaders = buildHeaders(rest, headers, auth, accessToken)

  let res: Response
  try {
    res = await performFetch(path, {
      ...rest,
      headers: finalHeaders,
    })
  } catch (error) {
    console.error("FETCH ERROR =", error)
    throw new ApiError("백엔드 서버 연결 실패 (CORS/서버실행/주소 확인)", 0)
  }

  if (
    res.status === 401 &&
    auth &&
    retryOnUnauthorized &&
    isSafeMethod(rest.method)
  ) {
    accessToken = getAccessToken()
    finalHeaders = buildHeaders(rest, headers, auth, accessToken)

    try {
      res = await performFetch(path, {
        ...rest,
        headers: finalHeaders,
      })
    } catch (error) {
      console.error("RETRY FETCH ERROR =", error)
      throw new ApiError("백엔드 서버 연결 실패 (재시도)", 0)
    }
  }

  const text = await res.text()

  let parsed: T | ErrorResponse | null = null
  try {
    parsed = text ? (JSON.parse(text) as T | ErrorResponse) : null
  } catch {
    parsed = null
  }

  if (!res.ok) {
    const err = parsed as ErrorResponse | null

    throw new ApiError(getUserFriendlyErrorMessage(err, res.status), res.status, {
      code: err?.code,
      validation: err?.validation,
    })
  }

  return parsed as T
}