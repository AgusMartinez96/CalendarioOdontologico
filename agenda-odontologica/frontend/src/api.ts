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
  endAt: string
  patientId: number
  motivo: string
  notas: string
  estado: AppointmentStatus
}

type ApiError = { message?: string; fieldErrors?: { field: string; message: string }[] }

let csrfHeader = 'X-XSRF-TOKEN'

function csrfCookie(): string {
  const cookie = document.cookie.split('; ').find((part) => part.startsWith('XSRF-TOKEN='))
  return cookie ? decodeURIComponent(cookie.substring('XSRF-TOKEN='.length)) : ''
}

async function ensureCsrf(): Promise<void> {
  const response = await fetch('/api/auth/csrf', { credentials: 'same-origin' })
  if (!response.ok) throw new Error('No se pudo preparar una solicitud segura.')
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

  const response = await fetch(path, { ...init, headers, credentials: 'same-origin' })
  if (response.status === 204) return undefined as T
  let body: ApiError & T
  try {
    body = (await response.json()) as ApiError & T
  } catch {
    throw new Error(response.ok
      ? 'El servidor devolvió una respuesta inválida.'
      : `El servidor respondió con un error (${response.status}).`)
  }
  if (!response.ok) {
    const details = body.fieldErrors?.map((error) => `${error.field}: ${error.message}`).join(' ')
    throw new Error([body.message, details].filter(Boolean).join(' '))
  }
  return body
}

export async function getSession(): Promise<{ authenticated: boolean; username: string }> {
  return request('/api/auth/session')
}

export async function login(username: string, password: string): Promise<void> {
  await ensureCsrf()
  await request('/api/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) })
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
