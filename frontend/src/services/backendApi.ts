export type LoginPayload = {
  unidade: string
  email: string
  password: string
}

export type LoginResponse = {
  token?: string
  message?: string
  error?: string
}

export type CreateAdminPayload = {
  unidade: string
  email: string
  name: string
  password: string
  role: 'SUPER_ADMIN' | 'EDITOR'
}

export type CreateAdminResponse = {
  email?: string
  name?: string
  role?: string
  status?: string
  createdAt?: string
  updatedAt?: string
  error?: string
}

export type AdminLookupResponse = CreateAdminResponse & {
  error?: string
}

export type TokenValidationResponse = {
  tokenResponse?: string
  tempoLimite?: string
  error?: string
}

export type UploadDocumentResponse = {
  sucesso?: boolean
  mensagem?: string
  key?: string | null
  error?: string
}

type UploadProgressHandler = (progress: number) => void

const API_BASE_URL = ((import.meta as ImportMeta & { env?: { VITE_API_BASE_URL?: string } }).env?.VITE_API_BASE_URL ?? 'http://localhost:8082').trim()

// Determine AI upload URL. Default to localhost when not provided.
let AI_UPLOAD_URL = ((import.meta as ImportMeta & { env?: { VITE_AI_UPLOAD_URL?: string } }).env?.VITE_AI_UPLOAD_URL ?? 'http://localhost:8002/upload').trim()

// If running in the browser and the configured URL points to the Docker-only host 'ai',
// rewrite the hostname to the current page hostname so the browser can resolve it (localhost).
if (typeof window !== 'undefined') {
  try {
    const parsed = new URL(AI_UPLOAD_URL)
    if (parsed.hostname === 'ai') {
      parsed.hostname = window.location.hostname
      AI_UPLOAD_URL = parsed.toString()
    }
  } catch (e) {
    // ignore URL parse errors and keep the configured value
  }
}
// Normalize AI_UPLOAD_URL:
// - If it's a relative path (starts with '/'), keep it so Vite dev proxy can intercept.
// - If it's an absolute URL with no path (e.g. 'http://localhost:8002'), append '/upload'.
if (!AI_UPLOAD_URL.startsWith('/')) {
  try {
    const parsed2 = new URL(AI_UPLOAD_URL)
    if (!parsed2.pathname || parsed2.pathname === '/') {
      parsed2.pathname = '/upload'
      AI_UPLOAD_URL = parsed2.toString()
    }
  } catch (e) {
    // ignore parse errors
  }
}

// When running in a browser during development, prefer using the relative
// path `/upload` so the Vite dev server proxy handles forwarding to the
// containerized AI service. This avoids DNS issues with Docker-only hostnames
// like `ai` that the user's browser cannot resolve.
if (typeof window !== 'undefined') {
  try {
    const lower = AI_UPLOAD_URL.toLowerCase()
    if (lower.includes('://ai') || lower.includes('://localhost') || lower.startsWith('http://')) {
      AI_UPLOAD_URL = '/upload'
    }
  } catch (e) {
    // ignore
  }
}
const TOKEN_STORAGE_KEY = 'fateco_auth_token'

function buildUrl(path: string): string {
  if (!API_BASE_URL) {
    return path
  }

  return `${API_BASE_URL.replace(/\/$/, '')}${path}`
}

async function readResponseBody(response: Response): Promise<string> {
  try {
    return await response.text()
  } catch {
    return ''
  }
}

function parseBody<T>(body: string): T | undefined {
  if (!body) {
    return undefined
  }

  try {
    return JSON.parse(body) as T
  } catch {
    return undefined
  }
}

async function requestJson<T>(path: string, init: RequestInit = {}, token?: string): Promise<T> {
  const headers = new Headers(init.headers)
  const isFormData = init.body instanceof FormData

  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  if (init.body !== undefined && !isFormData && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(buildUrl(path), {
    ...init,
    headers,
  })

  const responseBody = await readResponseBody(response)
  const parsedBody = parseBody<Record<string, unknown>>(responseBody)

  if (!response.ok) {
    const errorMessage =
      (parsedBody?.error as string | undefined) ??
      (parsedBody?.message as string | undefined) ??
      responseBody ??
      `Request failed with status ${response.status}`

    throw new Error(errorMessage)
  }

  if (parsedBody) {
    return parsedBody as T
  }

  return responseBody as T
}

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_STORAGE_KEY)
}

export function storeToken(token: string): void {
  localStorage.setItem(TOKEN_STORAGE_KEY, token)
}

export function clearStoredToken(): void {
  localStorage.removeItem(TOKEN_STORAGE_KEY)
}

export async function loginAdmin(payload: LoginPayload): Promise<LoginResponse> {
  return requestJson<LoginResponse>('/admin/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function createAdmin(payload: CreateAdminPayload): Promise<CreateAdminResponse> {
  return requestJson<CreateAdminResponse>('/admin/create', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function getAdminByEmail(email: string): Promise<AdminLookupResponse> {
  return requestJson<AdminLookupResponse>(`/admin/${encodeURIComponent(email)}`)
}

export async function validateStoredToken(): Promise<TokenValidationResponse> {
  const token = getStoredToken()
  if (!token) {
    throw new Error('Nenhum token encontrado. Faça login primeiro.')
  }

  return requestJson<TokenValidationResponse>('/admin/token', {
    method: 'POST',
  }, token)
}

export async function uploadDocument(file: File, onProgress?: UploadProgressHandler): Promise<UploadDocumentResponse> {
  const formData = new FormData()
  // FastAPI expects field name 'file'
  formData.append('file', file)

  return new Promise<UploadDocumentResponse>((resolve, reject) => {
    const xhr = new XMLHttpRequest()

    xhr.open('POST', AI_UPLOAD_URL)
    xhr.responseType = 'text'

    xhr.upload.onprogress = (event) => {
      if (!event.lengthComputable) {
        return
      }

      const progress = Math.round((event.loaded / event.total) * 100)
      onProgress?.(progress)
    }

    xhr.onload = () => {
      const responseBody = xhr.responseText ?? ''
      const parsedBody = parseBody<Record<string, unknown>>(responseBody)

      if (xhr.status < 200 || xhr.status >= 300) {
        const errorMessage =
          (parsedBody?.error as string | undefined) ??
          (parsedBody?.message as string | undefined) ??
          responseBody ??
          `Request failed with status ${xhr.status}`

        reject(new Error(errorMessage))
        return
      }

      if (parsedBody) {
        resolve(parsedBody as UploadDocumentResponse)
        return
      }

      resolve(responseBody as UploadDocumentResponse)
    }

    xhr.onerror = () => {
      reject(new Error('Falha ao enviar documento'))
    }

    xhr.send(formData)
  })
}
