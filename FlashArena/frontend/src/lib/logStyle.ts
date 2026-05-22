/** 로그 라인 내용(이모지/키워드)에 따라 색상 클래스를 결정한다. */
export function classifyLog(line: string): string {
  if (line.includes('⚠️') || line.includes('오버셀')) {
    return 'text-red-400 font-semibold'
  }
  if (line.includes('❌') || line.includes('실패') || line.includes('부족')) {
    return 'text-amber-400'
  }
  if (line.includes('✅') || line.includes('성공')) {
    return 'text-emerald-400'
  }
  if (line.includes('🔒')) {
    return 'text-sky-400'
  }
  if (line.includes('🚀') || line.includes('🏁')) {
    return 'text-violet-300 font-semibold'
  }
  return 'text-slate-300'
}
