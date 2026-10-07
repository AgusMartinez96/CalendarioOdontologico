import { useEffect, useId, useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import { ApiRequestError, getAuthConfig, login, register } from './api'
import { formatWait, passwordRules, USERNAME_HELP, validatePassword, validateUsername } from './passwordRules'

type Mode = 'login' | 'register'
type FieldErrors = { username?: string; password?: string; confirm?: string }

type Props = {
  onAuthenticated: (username: string) => void
  notice?: string
}

function AuthScreen({ onAuthenticated, notice = '' }: Props) {
  const id = useId()
  const [mode, setMode] = useState<Mode>('login')
  const [registrationOpen, setRegistrationOpen] = useState(false)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)
  const usernameRef = useRef<HTMLInputElement>(null)
  const passwordRef = useRef<HTMLInputElement>(null)
  const confirmRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    void refreshConfig()
  }, [])

  async function refreshConfig(): Promise<boolean> {
    try {
      const config = await getAuthConfig()
      setRegistrationOpen(config.registrationOpen)
      if (!config.registrationOpen) setMode('login')
      return config.registrationOpen
    } catch {
      setRegistrationOpen(false)
      return false
    }
  }

  function switchMode(next: Mode) {
    if (next === mode || saving) return
    setMode(next)
    setPassword('')
    setConfirm('')
    setShowPassword(false)
    setFieldErrors({})
    setFormError('')
  }

  function onTabKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    if (!registrationOpen) return
    if (event.key === 'ArrowRight' || event.key === 'ArrowLeft') {
      event.preventDefault()
      switchMode(mode === 'login' ? 'register' : 'login')
    }
  }

  async function onLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setFormError('')
    try {
      const name = await login(username.trim(), password)
      setPassword('')
      onAuthenticated(name)
    } catch (reason) {
      if (reason instanceof ApiRequestError && reason.status === 429) {
        setFormError(`Hiciste demasiados intentos fallidos. Volvé a probar en ${formatWait(reason.retryAfterSeconds)}.`)
      } else {
        setFormError(reason instanceof Error ? reason.message : 'No se pudo iniciar sesión.')
      }
    } finally {
      setSaving(false)
    }
  }

  async function onRegister(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const errors: FieldErrors = {}
    const usernameError = validateUsername(username)
    if (usernameError) errors.username = usernameError
    const passwordError = validatePassword(password, username)
    if (passwordError) errors.password = passwordError
    if (confirm !== password) errors.confirm = 'Las contraseñas no coinciden.'
    setFieldErrors(errors)
    setFormError('')
    if (errors.username || errors.password || errors.confirm) {
      const target = errors.username ? usernameRef : errors.password ? passwordRef : confirmRef
      target.current?.focus()
      return
    }

    setSaving(true)
    try {
      const name = await register(username, password)
      setPassword('')
      setConfirm('')
      onAuthenticated(name)
    } catch (reason) {
      await showRegisterError(reason)
    } finally {
      setSaving(false)
    }
  }

  async function showRegisterError(reason: unknown) {
    if (!(reason instanceof ApiRequestError)) {
      setFormError(reason instanceof Error ? reason.message : 'No se pudo crear la cuenta.')
      return
    }
    if (reason.status === 400 && reason.fieldErrors.length > 0) {
      const errors: FieldErrors = {}
      for (const error of reason.fieldErrors) {
        if (error.field === 'username') errors.username = error.message
        if (error.field === 'password') errors.password = error.message
      }
      setFieldErrors(errors)
      if (!errors.username && !errors.password) setFormError('Revisá los datos ingresados.')
      return
    }
    if (reason.status === 409 && reason.message.startsWith('Ese nombre de usuario')) {
      setFieldErrors({ username: 'Ese nombre de usuario ya está en uso. Probá con otro.' })
      usernameRef.current?.focus()
      return
    }
    if (reason.status === 409) {
      await refreshConfig()
      setFormError('El registro de cuentas está cerrado porque se alcanzó el cupo de usuarios. Si ya tenés una cuenta, iniciá sesión.')
      return
    }
    if (reason.status === 403) {
      await refreshConfig()
      setFormError('El registro de cuentas nuevas está deshabilitado por el momento.')
      return
    }
    if (reason.status === 429) {
      setFormError(`Hiciste demasiados intentos de registro. Volvé a probar en ${formatWait(reason.retryAfterSeconds)}.`)
      return
    }
    setFormError(reason.message || 'No se pudo crear la cuenta.')
  }

  const rules = passwordRules(password, username)
  const passwordType = showPassword ? 'text' : 'password'
  const registering = mode === 'register'

  return (
    <main className="login-shell">
      <div className="login-card">
        <div className="brand-mark login-mark">✳</div>
        <p className="eyebrow">CONSULTORIO ODONTOLÓGICO</p>
        <h1>Agenda Dental</h1>
        <p className="muted">
          {registering
            ? 'Creá tu cuenta para cargar tus propios pacientes y turnos.'
            : 'Iniciá sesión para gestionar pacientes y turnos.'}
        </p>

        {registrationOpen && (
          <div className="auth-tabs" role="tablist" aria-label="Acceso">
            <button type="button" role="tab" id={`${id}-tab-login`} aria-selected={!registering}
              aria-controls={`${id}-panel`} tabIndex={registering ? -1 : 0}
              className={`auth-tab ${registering ? '' : 'selected'}`} onKeyDown={onTabKeyDown}
              onClick={() => switchMode('login')}>Iniciar sesión</button>
            <button type="button" role="tab" id={`${id}-tab-register`} aria-selected={registering}
              aria-controls={`${id}-panel`} tabIndex={registering ? 0 : -1}
              className={`auth-tab ${registering ? 'selected' : ''}`} onKeyDown={onTabKeyDown}
              onClick={() => switchMode('register')}>Crear cuenta</button>
          </div>
        )}

        <div aria-live="polite" className="auth-messages">
          {notice && !formError && <div className="alert alert-error" role="status">{notice}</div>}
          {formError && <div className="alert alert-error" role="alert">{formError}</div>}
        </div>

        <form className="auth-form" id={`${id}-panel`} role={registrationOpen ? 'tabpanel' : undefined}
          aria-labelledby={registrationOpen ? `${id}-tab-${mode}` : undefined}
          onSubmit={registering ? onRegister : onLogin} noValidate={registering}>
          <div className="field">
            <label htmlFor={`${id}-username`}>Usuario</label>
            <input id={`${id}-username`} ref={usernameRef} name="username" autoComplete="username"
              autoCapitalize="none" autoCorrect="off" spellCheck={false} required value={username}
              aria-invalid={fieldErrors.username ? true : undefined}
              aria-describedby={registering ? `${id}-username-help ${fieldErrors.username ? `${id}-username-error` : ''}`.trim() : undefined}
              onChange={(event) => setUsername(event.target.value)} />
            {registering && <p className="field-help" id={`${id}-username-help`}>{USERNAME_HELP}</p>}
            {fieldErrors.username && <p className="field-error" id={`${id}-username-error`} role="alert">{fieldErrors.username}</p>}
          </div>

          <div className="field">
            <label htmlFor={`${id}-password`}>Contraseña</label>
            <div className="password-field">
              <input id={`${id}-password`} ref={passwordRef} name="password" type={passwordType}
                autoComplete={registering ? 'new-password' : 'current-password'} required value={password}
                aria-invalid={fieldErrors.password ? true : undefined}
                aria-describedby={registering ? `${id}-password-rules ${fieldErrors.password ? `${id}-password-error` : ''}`.trim() : undefined}
                onChange={(event) => setPassword(event.target.value)} />
              <button type="button" className="password-toggle" aria-pressed={showPassword}
                onClick={() => setShowPassword((current) => !current)}>
                {showPassword ? 'Ocultar' : 'Mostrar'}
                <span className="sr-only"> contraseña</span>
              </button>
            </div>
            {fieldErrors.password && <p className="field-error" id={`${id}-password-error`} role="alert">{fieldErrors.password}</p>}
          </div>

          {registering && (
            <>
              <ul className="password-rules" id={`${id}-password-rules`} aria-label="Reglas de la contraseña">
                {rules.map((rule) => (
                  <li key={rule.id} className={rule.met ? 'met' : ''}>
                    <span aria-hidden="true">{rule.met ? '✓' : '○'}</span>
                    <span>{rule.text}<span className="sr-only">{rule.met ? ' (cumplida)' : ' (pendiente)'}</span></span>
                  </li>
                ))}
              </ul>
              <div className="field">
                <label htmlFor={`${id}-confirm`}>Repetir contraseña</label>
                <input id={`${id}-confirm`} ref={confirmRef} name="confirm" type={passwordType}
                  autoComplete="new-password" required value={confirm}
                  aria-invalid={fieldErrors.confirm ? true : undefined}
                  aria-describedby={fieldErrors.confirm ? `${id}-confirm-error` : undefined}
                  onChange={(event) => setConfirm(event.target.value)} />
                {fieldErrors.confirm && <p className="field-error" id={`${id}-confirm-error`} role="alert">{fieldErrors.confirm}</p>}
              </div>
              <p className="field-help">
                Sin email no hay recuperación de contraseña: guardala en un lugar seguro.
              </p>
            </>
          )}

          <button className="primary-button full-width" type="submit" disabled={saving}>
            {registering
              ? (saving ? 'Creando cuenta…' : 'Crear cuenta')
              : (saving ? 'Ingresando…' : 'Iniciar sesión')}
          </button>
        </form>
      </div>
    </main>
  )
}

export default AuthScreen
