import { MemberCategory } from '../core/models';

/** Fee per category (BR-MEM-04). Used only to tell the librarian what to collect; the server decides. */
export const MEMBERSHIP_FEE: Record<MemberCategory, number> = { YOUTH: 0, ADULT: 12, SENIOR: 6 };

export function ageOn(birthDate: string, onDate: Date): number {
  const [year, month, day] = birthDate.split('-').map(Number);
  let age = onDate.getFullYear() - year;
  if (onDate.getMonth() + 1 < month || (onDate.getMonth() + 1 === month && onDate.getDate() < day)) {
    age -= 1;
  }
  return age;
}

/** The category on a date (F-MEM-01): under 18 YOUTH, 65 and over SENIOR, otherwise ADULT. */
export function categoryOn(birthDate: string, onDate: Date): MemberCategory {
  const age = ageOn(birthDate, onDate);
  if (age < 18) {
    return 'YOUTH';
  }
  return age >= 65 ? 'SENIOR' : 'ADULT';
}
