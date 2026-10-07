import { COMMON_PASSWORDS } from './commonPasswords'

// Mismas reglas y mensajes que el servidor (AccountService.java).
const USERNAME_PATTERN = /^[A-Za-z0-9][A-Za-z0-9._-]*$/
const PASSWORD_MIN = 10
const PASSWORD_MAX_BYTES = 72

export const USERNAME_HELP =
  '3 a 30 caracteres: letras, números, punto, guion y guion bajo. Tiene que empezar con una letra o un número.'

export function validateUsername(username: string): string | null {
  if (username.length < 3 || username.length > 30) return 'El usuario debe tener entre 3 y 30 caracteres.'
  if (!USERNAME_PATTERN.test(username)) {
    return 'El usuario solo puede tener letras, números, punto, guion y guion bajo, y debe empezar con una letra o un número.'
  }
  return null
}

function passwordBytes(password: string): number {
  return new TextEncoder().encode(password).length
}

export function validatePassword(password: string, username: string): string | null {
  if ([...password].length < PASSWORD_MIN) return 'La contraseña debe tener al menos 10 caracteres.'
  if (passwordBytes(password) > PASSWORD_MAX_BYTES) {
    return 'La contraseña no puede superar los 72 bytes en UTF-8 (las tildes y los emojis ocupan más de un byte).'
  }
  if (password.toLowerCase() === username.toLowerCase()) {
    return 'La contraseña no puede ser igual al nombre de usuario.'
  }
  if (COMMON_PASSWORDS.has(password.toLowerCase())) return 'Esa contraseña es demasiado común. Elegí otra.'
  return null
}

export type PasswordRule = { id: string; text: string; met: boolean }

/** Reglas visibles como ayuda, con su estado actual para la contraseña escrita. */
export function passwordRules(password: string, username: string): PasswordRule[] {
  const typed = password.length > 0
  return [
    { id: 'length', text: 'Al menos 10 caracteres.', met: [...password].length >= PASSWORD_MIN },
    {
      id: 'bytes',
      text: 'Hasta 72 bytes en total (con tildes o emojis entran menos caracteres).',
      met: typed && passwordBytes(password) <= PASSWORD_MAX_BYTES,
    },
    {
      id: 'different',
      text: 'Distinta de tu usuario.',
      met: typed && password.toLowerCase() !== username.toLowerCase(),
    },
    {
      id: 'common',
      text: 'Que no sea una contraseña muy común.',
      met: typed && !COMMON_PASSWORDS.has(password.toLowerCase()),
    },
  ]
}

export function formatWait(seconds: number | null): string {
  if (seconds === null || seconds <= 0) return 'unos minutos'
  if (seconds < 60) return `${seconds} ${seconds === 1 ? 'segundo' : 'segundos'}`
  const minutes = Math.ceil(seconds / 60)
  return `${minutes} ${minutes === 1 ? 'minuto' : 'minutos'}`
}
