// "2026-03-16" -> "3월 16일 활동"
export function formatDateLabel(dateStr: string): string {
  const [, month, day] = dateStr.split('-');
  return `${parseInt(month)}월 ${parseInt(day)}일 활동`;
}
