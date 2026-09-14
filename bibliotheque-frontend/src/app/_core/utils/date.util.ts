/**
 * Le backend sérialise les dates de Borrow (issueDate/dueDate/returnDate) au
 * format "dd-MM-yyyy" (JsonDataSerializer, cf. Borrow.java), contrairement au
 * reste de l'API qui renvoie de l'ISO par défaut (ex. Reservation). Le
 * constructeur natif `new Date("14-09-2026")` ne sait pas parser ce format
 * et produit un "Invalid Date" silencieux (aucune erreur levée) — d'où ce
 * parseur dédié, à utiliser pour tout champ de date issu de Borrow.
 */
export function parseBorrowDate(value: string | Date | null | undefined): Date | null {
  if (!value) {
    return null;
  }
  if (value instanceof Date) {
    return isNaN(value.getTime()) ? null : value;
  }
  const match = /^(\d{2})-(\d{2})-(\d{4})$/.exec(value);
  if (match) {
    const [, day, month, year] = match;
    return new Date(Number(year), Number(month) - 1, Number(day));
  }
  // Secours : au cas où le format changerait côté backend (ex. retour à l'ISO).
  const fallback = new Date(value);
  return isNaN(fallback.getTime()) ? null : fallback;
}

export function formatBorrowDate(value: string | Date | null | undefined): string {
  const date = parseBorrowDate(value);
  return date ? date.toLocaleDateString('fr-FR') : '';
}
