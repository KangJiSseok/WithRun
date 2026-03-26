// "2026-03-12T06:30:00" → "06:30"
export function formatStartTime(startedAt: string): string {
  const date = new Date(startedAt);
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

// "2026-03-16" -> "3월 16일 활동"
export function formatDateLabel(dateStr: string): string {
  const [, month, day] = dateStr.split('-');
  return `${parseInt(month)}월 ${parseInt(day)}일 활동`;
}
