
/**
 * Revisa que un texto tenga forma de correo: sin espacios, una sola arroba con algo antes y un
 * dominio con punto.
 *
 * @param email texto a revisar
 * @returns true si tiene forma de correo
 */
export function isValidEmail(email: string): boolean {
  if (/\s/.test(email)) return false; // sin espacios
  const at = email.indexOf('@');
  if (at < 1 || at !== email.lastIndexOf('@')) return false; // un solo @ y algo antes

  const domain = email.slice(at + 1);
  const dot = domain.lastIndexOf('.');
  return dot > 0 && dot < domain.length - 1; // un punto con texto a ambos lados
}
