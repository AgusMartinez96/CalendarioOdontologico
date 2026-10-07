export type Patient = {
  id: number
  nombre: string
  apellido: string
  dni: string
  telefono: string
  email: string | null
}

export type PatientInput = Omit<Patient, 'id'>
export type AppointmentStatus = 'PROGRAMADO' | 'CONFIRMADO' | 'CANCELADO' | 'ATENDIDO'
export type Appointment = {
  id: number
  startAt: string
  endAt: string
  patientId: number
  patientName: string
  motivo: string
  notas: string | null
  estado: AppointmentStatus
}

export type AppointmentInput = {
  startAt: string
  patientId: number
  motivo: string
  notas: string
  estado: AppointmentStatus
}

export type FieldError = { field: string; message: string }
type ApiError = { message?: string; fieldErrors?: FieldError[] }

/** Error de la API con el código HTTP, la espera sugerida (429) y los errores por campo. */
export class ApiRequestError extends Error {
  readonly status: number
  readonly retryAfterSeconds: number | null
  readonly fieldErrors: FieldError[]

  constructor(message: string, status: number, retryAfterSeconds: number | null = null, fieldErrors: FieldError[] = []) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = status
    this.retryAfterSeconds = retryAfterSeconds
    this.fieldErrors = fieldErrors
  }
}

let csrfHeader = 'X-XSRF-TOKEN'
let onUnauthorized: (() => void) | null = null

/** Permite a la app volver a la pantalla de acceso cuando la sesión vence. */
export function setUnauthorizedHandler(handler: (() => void) | null): void {
  onUnauthorized = handler
}

function csrfCookie(): string {
  const cookie = document.cookie.split('; ').find((part) => part.startsWith('XSRF-TOKEN='))
  return cookie ? decodeURIComponent(cookie.substring('XSRF-TOKEN='.length)) : ''
}

const NETWORK_ERROR = 'No se pudo conectar con el servidor. Revisá tu conexión e intentá de nuevo.'

async function ensureCsrf(): Promise<void> {
  let response: Response
  try {
    response = await fetch('/api/auth/csrf', { credentials: 'same-origin' })
  } catch {
    throw new ApiRequestError(NETWORK_ERROR, 0)
  }
  if (!response.ok) throw new ApiRequestError('No se pudo preparar una solicitud segura.', response.status)
  const token = (await response.json()) as { token: string; headerName: string }
  csrfHeader = token.headerName
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = init.method ?? 'GET'
  const headers = new Headers(init.headers)
  if (init.body) headers.set('Content-Type', 'application/json')
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method.toUpperCase())) {
    headers.set(csrfHeader, csrfCookie())
  }

  let response: Response
  try {
    response = await fetch(path, { ...init, headers, credentials: 'same-origin' })
  } catch {
    throw new ApiRequestError(NETWORK_ERROR, 0)
  }
  if (response.status === 204) return undefined as T
  let body: ApiError & T
  try {
    body = (await response.json()) as ApiError & T
  } catch {
    throw new ApiRequestError(response.ok
      ? 'El servidor devolvió una respuesta inválida.'
      : `El servidor respondió con un error (${response.status}).`, response.status)
  }
  if (!response.ok) {
    if (response.status === 401 && !path.startsWith('/api/auth/')) {
      onUnauthorized?.()
      throw new ApiRequestError('Tu sesión venció. Volvé a iniciar sesión.', 401)
    }
    const retryAfter = Number.parseInt(response.headers.get('Retry-After') ?? '', 10)
    const fieldErrors = body.fieldErrors ?? []
    const details = fieldErrors.map((error) => `${error.field}: ${error.message}`).join(' ')
    throw new ApiRequestError(
      [body.message, details].filter(Boolean).join(' '),
      response.status,
      Number.isFinite(retryAfter) ? retryAfter : null,
      fieldErrors,
    )
  }
  return body
}

type SessionResponse = { authenticated: boolean; username: string }

export async function getSession(): Promise<SessionResponse> {
  return request('/api/auth/session')
}

export async function getAuthConfig(): Promise<{ registrationOpen: boolean }> {
  return request('/api/auth/config')
}

export async function login(username: string, password: string): Promise<string> {
  await ensureCsrf()
  const session = await request<SessionResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
  return session.username
}

export async function register(username: string, password: string): Promise<string> {
  await ensureCsrf()
  const session = await request<SessionResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
  return session.username
}

export async function logout(): Promise<void> {
  await ensureCsrf()
  await request('/api/auth/logout', { method: 'POST' })
}

export const api = {
  listPatients: (search = '') =>
    request<Patient[]>(`/api/v1/patients${search ? `?search=${encodeURIComponent(search)}` : ''}`),
  getPatient: (id: number) => request<Patient>(`/api/v1/patients/${id}`),
  createPatient: (patient: PatientInput) =>
    request<Patient>('/api/v1/patients', { method: 'POST', body: JSON.stringify(patient) }),
  updatePatient: (id: number, patient: PatientInput) =>
    request<Patient>(`/api/v1/patients/${id}`, { method: 'PUT', body: JSON.stringify(patient) }),
  deletePatient: (id: number) => request<void>(`/api/v1/patients/${id}`, { method: 'DELETE' }),
  listAppointments: (from: string, to: string, status: string, patientSearch: string) => {
    const params = new URLSearchParams({ from, to })
    if (status) params.set('status', status)
    if (patientSearch) params.set('patientSearch', patientSearch)
    return request<Appointment[]>(`/api/v1/appointments?${params}`)
  },
  createAppointment: (appointment: AppointmentInput) =>
    request<Appointment>('/api/v1/appointments', {
      method: 'POST',
      body: JSON.stringify(appointment),
    }),
  updateAppointment: (id: number, appointment: AppointmentInput) =>
    request<Appointment>(`/api/v1/appointments/${id}`, {
      method: 'PUT',
      body: JSON.stringify(appointment),
    }),
  cancelAppointment: (id: number) =>
    request<Appointment>(`/api/v1/appointments/${id}/cancel`, { method: 'PATCH' }),
}
