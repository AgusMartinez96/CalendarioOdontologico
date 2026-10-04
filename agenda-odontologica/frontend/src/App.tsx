import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import luxonPlugin from '@fullcalendar/luxon3'
import esLocale from '@fullcalendar/core/locales/es'
import type { DateSelectArg, EventClickArg, EventInput, DatesSetArg } from '@fullcalendar/core'
import { DateTime } from 'luxon'
import {
  api,
  getSession,
  login,
  logout,
  type Appointment,
  type AppointmentInput,
  type AppointmentStatus,
  type Patient,
  type PatientInput,
} from './api'

const TIME_ZONE = import.meta.env.VITE_APP_TIME_ZONE || 'America/Argentina/Buenos_Aires'
const STATUSES: AppointmentStatus[] = ['PROGRAMADO', 'CONFIRMADO', 'CANCELADO', 'ATENDIDO']
const EMPTY_PATIENT: PatientInput = { nombre: '', apellido: '', dni: '', telefono: '', email: '' }

type AppointmentDraft = {
  id?: number
  startAt: string
  endAt: string
  patientId: string
  motivo: string
  notas: string
  estado: AppointmentStatus
}

function localInput(date: Date): string {
  return DateTime.fromJSDate(date).setZone(TIME_ZONE).toFormat("yyyy-MM-dd'T'HH:mm")
}

function toInstant(value: string): string {
  const date = DateTime.fromISO(value, { zone: TIME_ZONE })
  if (!date.isValid) throw new Error('Revisá la fecha y hora del turno.')
  const instant = date.toUTC().toISO()
  if (!instant) throw new Error('No se pudo interpretar la fecha y hora.')
  return instant
}

function draftFrom(appointment: Appointment): AppointmentDraft {
  return {
    id: appointment.id,
    startAt: localInput(new Date(appointment.startAt)),
    endAt: localInput(new Date(appointment.endAt)),
    patientId: String(appointment.patientId),
    motivo: appointment.motivo,
    notas: appointment.notas ?? '',
    estado: appointment.estado,
  }
}

function displayTime(value: string): string {
  return DateTime.fromISO(value).setZone(TIME_ZONE).toFormat('HH:mm')
}

function App() {
  const [authenticated, setAuthenticated] = useState<boolean | null>(null)
  const [username, setUsername] = useState('')
  const [loginName, setLoginName] = useState('')
  const [password, setPassword] = useState('')
  const [loginError, setLoginError] = useState('')
  const [view, setView] = useState<'calendar' | 'patients'>('calendar')
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [patients, setPatients] = useState<Patient[]>([])
  const [appointmentDraft, setAppointmentDraft] = useState<AppointmentDraft | null>(null)
  const [patientDraft, setPatientDraft] = useState<Patient | null | 'new'>(null)
  const [patientInput, setPatientInput] = useState<PatientInput>(EMPTY_PATIENT)
  const [patientSearch, setPatientSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [range, setRange] = useState<{ from: string; to: string } | null>(null)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    getSession()
      .then((session) => {
        setAuthenticated(session.authenticated)
        setUsername(session.username)
      })
      .catch((reason: unknown) => {
        setAuthenticated(false)
        setError(reason instanceof Error ? reason.message : 'No se pudo conectar con la aplicación.')
      })
  }, [])

  const refreshPatients = useCallback(async () => {
    setPatients(await api.listPatients(patientSearch))
  }, [patientSearch])

  const refreshAppointments = useCallback(async () => {
    if (!range) return
    setLoading(true)
    setError('')
    try {
      setAppointments(await api.listAppointments(range.from, range.to, statusFilter, patientSearch))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudieron cargar los turnos.')
    } finally {
      setLoading(false)
    }
  }, [patientSearch, range, statusFilter])

  useEffect(() => {
    if (authenticated) {
      refreshPatients().catch((reason: unknown) =>
        setError(reason instanceof Error ? reason.message : 'No se pudieron cargar los pacientes.'),
      )
    }
  }, [authenticated, refreshPatients])

  useEffect(() => {
    if (authenticated && view === 'calendar' && range) void refreshAppointments()
  }, [authenticated, range, refreshAppointments, view])

  const onDatesSet = useCallback((info: DatesSetArg) => {
    const from = info.start.toISOString()
    const to = info.end.toISOString()
    setRange((current) => current?.from === from && current.to === to ? current : { from, to })
  }, [])

  const events = useMemo<EventInput[]>(
    () =>
      appointments.map((appointment) => ({
        id: String(appointment.id),
        title: `${displayTime(appointment.startAt)} · ${appointment.patientName} — ${appointment.motivo}`,
        start: appointment.startAt,
        end: appointment.endAt,
        classNames: [`appointment-${appointment.estado.toLowerCase()}`],
        extendedProps: { appointment },
      })),
    [appointments],
  )

  async function onLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setLoginError('')
    try {
      await login(loginName.trim(), password)
      setUsername(loginName.trim())
      setAuthenticated(true)
      setPassword('')
      setError('')
    } catch (reason) {
      setLoginError(reason instanceof Error ? reason.message : 'No se pudo iniciar sesión.')
    } finally {
      setSaving(false)
    }
  }

  async function onLogout() {
    setError('')
    try {
      await logout()
      setAuthenticated(false)
      setUsername('')
      setAppointments([])
      setPatients([])
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo cerrar la sesión.')
    }
  }

  function onSlotSelect(info: DateSelectArg) {
    const startAt = localInput(info.start)
    const endAt = localInput(info.end)
    info.view.calendar.unselect()
    setAppointmentDraft({
      startAt,
      endAt,
      patientId: '',
      motivo: '',
      notas: '',
      estado: 'PROGRAMADO',
    })
  }

  function onEventClick(info: EventClickArg) {
    const appointment = info.event.extendedProps.appointment as Appointment
    setAppointmentDraft(draftFrom(appointment))
  }

  async function onSaveAppointment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!appointmentDraft) return
    setSaving(true)
    setError('')
    try {
      const input: AppointmentInput = {
        startAt: toInstant(appointmentDraft.startAt),
        endAt: toInstant(appointmentDraft.endAt),
        patientId: Number(appointmentDraft.patientId),
        motivo: appointmentDraft.motivo.trim(),
        notas: appointmentDraft.notas.trim(),
        estado: appointmentDraft.estado,
      }
      if (appointmentDraft.id) await api.updateAppointment(appointmentDraft.id, input)
      else await api.createAppointment(input)
      setAppointmentDraft(null)
      setNotice(appointmentDraft.id ? 'Turno actualizado.' : 'Turno agendado.')
      await refreshAppointments()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo guardar el turno.')
    } finally {
      setSaving(false)
    }
  }

  async function onCancelAppointment() {
    if (!appointmentDraft?.id || !window.confirm('¿Querés cancelar este turno?')) return
    setSaving(true)
    try {
      await api.cancelAppointment(appointmentDraft.id)
      setAppointmentDraft(null)
      setNotice('Turno cancelado.')
      await refreshAppointments()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo cancelar el turno.')
    } finally {
      setSaving(false)
    }
  }

  function openNewPatient() {
    setPatientInput(EMPTY_PATIENT)
    setPatientDraft('new')
  }

  function openEditPatient(patient: Patient) {
    setPatientInput({
      nombre: patient.nombre,
      apellido: patient.apellido,
      dni: patient.dni,
      telefono: patient.telefono,
      email: patient.email ?? '',
    })
    setPatientDraft(patient)
  }

  async function onSavePatient(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (patientDraft === null) return
    setSaving(true)
    setError('')
    try {
      const input = { ...patientInput, email: patientInput.email || null }
      if (patientDraft === 'new') await api.createPatient(input)
      else await api.updatePatient(patientDraft.id, input)
      setPatientDraft(null)
      await refreshPatients()
      setNotice(patientDraft === 'new' ? 'Paciente creado.' : 'Paciente actualizado.')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo guardar el paciente.')
    } finally {
      setSaving(false)
    }
  }

  async function onDeletePatient(patient: Patient) {
    if (!window.confirm(`¿Querés eliminar a ${patient.nombre} ${patient.apellido}?`)) return
    try {
      await api.deletePatient(patient.id)
      await refreshPatients()
      setNotice('Paciente eliminado.')
      setError('')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo eliminar el paciente.')
    }
  }

  if (authenticated === null) {
    return <main className="login-shell"><div className="loading-card">Cargando agenda…</div></main>
  }

  if (!authenticated) {
    return (
      <main className="login-shell">
        <form className="login-card" onSubmit={onLogin}>
          <div className="brand-mark login-mark">✳</div>
          <p className="eyebrow">CONSULTORIO ODONTOLÓGICO</p>
          <h1>Agenda Dental</h1>
          <p className="muted">Iniciá sesión para gestionar pacientes y turnos.</p>
          {loginError && <div className="alert alert-error" role="alert">{loginError}</div>}
          {error && <div className="alert alert-error" role="alert">{error}</div>}
          <label>Usuario
            <input autoComplete="username" required value={loginName}
              onChange={(event) => setLoginName(event.target.value)} />
          </label>
          <label>Contraseña
            <input autoComplete="current-password" type="password" required value={password}
              onChange={(event) => setPassword(event.target.value)} />
          </label>
          <button className="primary-button full-width" type="submit" disabled={saving}>
            {saving ? 'Ingresando…' : 'Iniciar sesión'}
          </button>
        </form>
      </main>
    )
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="/" aria-label="Agenda Dental, inicio">
          <span className="brand-mark">✳</span><span>Agenda<span className="brand-light"> Dental</span></span>
        </a>
        <p className="nav-caption">MENÚ</p>
        <button className={`nav-link ${view === 'calendar' ? 'selected' : ''}`}
          onClick={() => setView('calendar')}><span>▦</span> Calendario</button>
        <button className={`nav-link ${view === 'patients' ? 'selected' : ''}`}
          onClick={() => setView('patients')}><span>♧</span> Pacientes</button>
        <div className="sidebar-note">
          <span className="online-dot" /> Datos guardados de forma segura.
        </div>
        <div className="account">
          <div className="avatar">{username.slice(0, 1).toUpperCase()}</div>
          <div className="account-name"><strong>{username}</strong><span>Administrador</span></div>
          <button className="icon-button logout-button" onClick={() => void onLogout()} title="Cerrar sesión" aria-label="Cerrar sesión">↪</button>
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div>
            <div className="breadcrumb">Consultorio <span>/</span> {view === 'calendar' ? 'Calendario' : 'Pacientes'}</div>
            <h1>{view === 'calendar' ? 'Agenda de turnos' : 'Pacientes'}</h1>
          </div>
          {view === 'patients' && <button className="primary-button" onClick={openNewPatient}>＋ Nuevo paciente</button>}
          {view === 'calendar' && <button className="primary-button" onClick={() => setAppointmentDraft({
            startAt: localInput(new Date()), endAt: localInput(new Date(Date.now() + 60 * 60 * 1000)),
            patientId: '', motivo: '', notas: '', estado: 'PROGRAMADO',
          })}>＋ Nuevo turno</button>}
        </header>

        {error && <div className="alert alert-error page-alert" role="alert">
          <span>{error}</span><button className="alert-dismiss" onClick={() => setError('')} aria-label="Cerrar">×</button>
        </div>}
        {notice && <div className="alert alert-success page-alert" role="status">
          <span>{notice}</span><button className="alert-dismiss" onClick={() => setNotice('')} aria-label="Cerrar">×</button>
        </div>}

        {view === 'calendar' ? (
          <section className="calendar-card" aria-label="Calendario de turnos">
            <div className="calendar-tools">
              <label className="search-field"><span>⌕</span>
                <input aria-label="Buscar paciente" placeholder="Buscar paciente…" value={patientSearch}
                  onChange={(event) => setPatientSearch(event.target.value)} />
              </label>
              <label className="filter-select"><span>Estado</span>
                <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
                  <option value="">Todos</option>
                  {STATUSES.map((status) => <option key={status} value={status}>{statusLabel(status)}</option>)}
                </select>
              </label>
              {loading && <span className="loading-inline">Actualizando…</span>}
            </div>
            <div className="calendar-wrap">
              <FullCalendar
                plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin, luxonPlugin]}
                locales={[esLocale]}
                locale="es"
                timeZone={TIME_ZONE}
                initialView="timeGridWeek"
                headerToolbar={{ left: 'prev,next today', center: 'title', right: 'dayGridMonth,timeGridWeek,timeGridDay' }}
                buttonText={{ today: 'Hoy', month: 'Mes', week: 'Semana', day: 'Día' }}
                slotMinTime="07:00:00"
                slotMaxTime="22:00:00"
                slotDuration="00:30:00"
                allDaySlot={false}
                selectable
                selectMirror
                select={onSlotSelect}
                eventClick={onEventClick}
                events={events}
                datesSet={onDatesSet}
                height="auto"
                nowIndicator
                noEventsText="No hay turnos para mostrar"
              />
            </div>
            <div className="calendar-legend">
              {STATUSES.map((status) => <span key={status}><i className={`legend-dot ${status.toLowerCase()}`} />{statusLabel(status)}</span>)}
            </div>
          </section>
        ) : (
          <section className="patients-card">
            <div className="patients-toolbar">
              <p className="muted">{patients.length} {patients.length === 1 ? 'paciente registrado' : 'pacientes registrados'}</p>
              <label className="search-field"><span>⌕</span>
                <input aria-label="Buscar pacientes" placeholder="Buscar por nombre o DNI…" value={patientSearch}
                  onChange={(event) => setPatientSearch(event.target.value)} />
              </label>
            </div>
            {patients.length === 0 ? <div className="empty-state">
              <div className="empty-icon">♧</div><h2>No hay pacientes todavía</h2>
              <p>Agregá una persona para poder agendar su primer turno.</p>
              <button className="primary-button" onClick={openNewPatient}>＋ Nuevo paciente</button>
            </div> : (
              <>
                <div className="patients-table-wrap">
                  <table className="patients-table">
                    <thead><tr><th>Paciente</th><th>DNI</th><th>Teléfono</th><th>Email</th><th>Acciones</th></tr></thead>
                    <tbody>{patients.map((patient) => <tr key={patient.id}>
                      <td><div className="patient-cell"><div className="patient-avatar">{patient.nombre.slice(0, 1)}{patient.apellido.slice(0, 1)}</div>
                        <strong>{patient.nombre} {patient.apellido}</strong></div></td>
                      <td>{patient.dni}</td><td>{patient.telefono}</td><td>{patient.email || '—'}</td>
                      <td><div className="table-actions">
                        <button className="text-button" onClick={() => openEditPatient(patient)}>Editar</button>
                        <button className="text-button danger-text" onClick={() => void onDeletePatient(patient)}>Eliminar</button>
                      </div></td>
                    </tr>)}</tbody>
                  </table>
                </div>
                <div className="mobile-patients">{patients.map((patient) => <article className="patient-mobile-card" key={patient.id}>
                  <div className="patient-cell"><div className="patient-avatar">{patient.nombre.slice(0, 1)}{patient.apellido.slice(0, 1)}</div>
                    <strong>{patient.nombre} {patient.apellido}</strong></div>
                  <p>DNI: {patient.dni} · Tel: {patient.telefono}</p>{patient.email && <p>{patient.email}</p>}
                  <div className="table-actions"><button className="text-button" onClick={() => openEditPatient(patient)}>Editar</button>
                    <button className="text-button danger-text" onClick={() => void onDeletePatient(patient)}>Eliminar</button></div>
                </article>)}</div>
              </>
            )}
          </section>
        )}
      </main>

      {appointmentDraft && (
        <div className="modal-backdrop" role="presentation" onMouseDown={(event) => {
          if (event.target === event.currentTarget && !saving) setAppointmentDraft(null)
        }}>
          <form className="modal-card" onSubmit={(event) => void onSaveAppointment(event)} role="dialog" aria-modal="true" aria-labelledby="appointment-title">
            <div className="modal-header"><div><p className="eyebrow">AGENDA</p>
              <h2 id="appointment-title">{appointmentDraft.id ? 'Editar turno' : 'Nuevo turno'}</h2></div>
              <button type="button" className="icon-button" aria-label="Cerrar" onClick={() => setAppointmentDraft(null)}>×</button></div>
            <div className="form-grid">
              <label className="span-two">Paciente
                <select required value={appointmentDraft.patientId} onChange={(event) => setAppointmentDraft({ ...appointmentDraft, patientId: event.target.value })}>
                  <option value="">Seleccionar paciente</option>
                  {patients.map((patient) => <option key={patient.id} value={patient.id}>{patient.nombre} {patient.apellido} · DNI {patient.dni}</option>)}
                </select>
              </label>
              <label>Inicio<input type="datetime-local" required value={appointmentDraft.startAt}
                onChange={(event) => setAppointmentDraft({ ...appointmentDraft, startAt: event.target.value })} /></label>
              <label>Fin<input type="datetime-local" required value={appointmentDraft.endAt}
                onChange={(event) => setAppointmentDraft({ ...appointmentDraft, endAt: event.target.value })} /></label>
              <label>Motivo<input required maxLength={200} value={appointmentDraft.motivo} placeholder="Ej. Control"
                onChange={(event) => setAppointmentDraft({ ...appointmentDraft, motivo: event.target.value })} /></label>
              <label>Estado<select value={appointmentDraft.estado}
                onChange={(event) => setAppointmentDraft({ ...appointmentDraft, estado: event.target.value as AppointmentStatus })}>
                {STATUSES.map((status) => <option key={status} value={status}>{statusLabel(status)}</option>)}</select></label>
              <label className="span-two">Notas<textarea maxLength={5000} rows={3} value={appointmentDraft.notas}
                onChange={(event) => setAppointmentDraft({ ...appointmentDraft, notas: event.target.value })} /></label>
            </div>
            <p className="timezone-note">Horario: {TIME_ZONE}</p>
            <div className="modal-actions">
              {appointmentDraft.id && appointmentDraft.estado !== 'CANCELADO' &&
                <button type="button" className="secondary-button danger-outline" disabled={saving} onClick={() => void onCancelAppointment()}>Cancelar turno</button>}
              <span className="action-spacer" />
              <button type="button" className="secondary-button" onClick={() => setAppointmentDraft(null)}>Cerrar</button>
              <button className="primary-button" type="submit" disabled={saving || patients.length === 0}>
                {saving ? 'Guardando…' : 'Guardar turno'}
              </button>
            </div>
            {patients.length === 0 && <p className="field-hint">Primero tenés que registrar un paciente.</p>}
          </form>
        </div>
      )}

      {patientDraft !== null && (
        <div className="modal-backdrop" role="presentation" onMouseDown={(event) => {
          if (event.target === event.currentTarget && !saving) setPatientDraft(null)
        }}>
          <form className="modal-card compact-modal" onSubmit={(event) => void onSavePatient(event)} role="dialog" aria-modal="true" aria-labelledby="patient-title">
            <div className="modal-header"><div><p className="eyebrow">PACIENTES</p>
              <h2 id="patient-title">{patientDraft === 'new' ? 'Nuevo paciente' : 'Editar paciente'}</h2></div>
              <button type="button" className="icon-button" aria-label="Cerrar" onClick={() => setPatientDraft(null)}>×</button></div>
            <div className="form-grid">
              <label>Nombre<input required maxLength={100} autoComplete="given-name" value={patientInput.nombre}
                onChange={(event) => setPatientInput({ ...patientInput, nombre: event.target.value })} /></label>
              <label>Apellido<input required maxLength={100} autoComplete="family-name" value={patientInput.apellido}
                onChange={(event) => setPatientInput({ ...patientInput, apellido: event.target.value })} /></label>
              <label>DNI<input required maxLength={30} value={patientInput.dni}
                onChange={(event) => setPatientInput({ ...patientInput, dni: event.target.value })} /></label>
              <label>Teléfono<input required maxLength={40} type="tel" autoComplete="tel" value={patientInput.telefono}
                onChange={(event) => setPatientInput({ ...patientInput, telefono: event.target.value })} /></label>
              <label className="span-two">Email (opcional)<input maxLength={254} type="email" autoComplete="email" value={patientInput.email ?? ''}
                onChange={(event) => setPatientInput({ ...patientInput, email: event.target.value })} /></label>
            </div>
            <div className="modal-actions">
              <span className="action-spacer" />
              <button type="button" className="secondary-button" onClick={() => setPatientDraft(null)}>Cerrar</button>
              <button className="primary-button" type="submit" disabled={saving}>{saving ? 'Guardando…' : 'Guardar paciente'}</button>
            </div>
          </form>
        </div>
      )}
    </div>
  )
}

function statusLabel(status: AppointmentStatus): string {
  return {
    PROGRAMADO: 'Programado',
    CONFIRMADO: 'Confirmado',
    CANCELADO: 'Cancelado',
    ATENDIDO: 'Atendido',
  }[status]
}

export default App
