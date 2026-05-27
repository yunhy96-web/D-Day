import { useEffect, useId, useRef, useState } from 'react'

interface MermaidProps {
  /** Mermaid 다이어그램 정의 문자열 (flowchart / sequenceDiagram 등). */
  chart: string
  className?: string
}

// mermaid.initialize 는 전역 1회만 호출하면 충분하다 (테마 고정).
let initialized = false

/**
 * Mermaid 다이어그램을 SVG 로 렌더링하는 컴포넌트.
 * - mermaid 본체는 dynamic import 로 lazy 로딩 → 초기 번들에 포함되지 않는다.
 * - 다크(slate) 테마에 맞춘 themeVariables.
 * - chart 가 바뀌면 다시 렌더링하고, 파싱 실패 시 원본 소스를 fallback 으로 보여준다.
 */
export default function Mermaid({ chart, className }: MermaidProps) {
  // useId 는 ":r0:" 처럼 콜론을 포함 → SVG/CSS id 로 못 쓰므로 제거한다.
  const id = `mmd-${useId().replace(/:/g, '')}`
  const ref = useRef<HTMLDivElement>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function render() {
      try {
        const mermaid = (await import('mermaid')).default

        if (!initialized) {
          mermaid.initialize({
            startOnLoad: false,
            theme: 'dark',
            securityLevel: 'strict',
            fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
            themeVariables: {
              background: 'transparent',
              primaryColor: '#1e293b', // slate-800
              primaryBorderColor: '#334155', // slate-700
              primaryTextColor: '#e2e8f0', // slate-200
              lineColor: '#64748b', // slate-500
              secondaryColor: '#0f172a', // slate-900
              tertiaryColor: '#1e293b',
              noteBkgColor: '#334155',
              noteTextColor: '#e2e8f0',
              noteBorderColor: '#475569',
            },
          })
          initialized = true
        }

        const { svg, bindFunctions } = await mermaid.render(id, chart)
        if (cancelled || !ref.current) return
        ref.current.innerHTML = svg
        bindFunctions?.(ref.current)
        setError(null)
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : String(e))
        }
      }
    }

    render()
    return () => {
      cancelled = true
    }
  }, [chart, id])

  if (error) {
    return (
      <pre className="overflow-x-auto rounded-xl border border-red-900/60 bg-red-950/30 p-4 text-xs text-red-300">
        {`Mermaid 렌더링 실패: ${error}\n\n${chart}`}
      </pre>
    )
  }

  return (
    <div
      ref={ref}
      className={className ?? 'flex justify-center overflow-x-auto [&_svg]:max-w-full [&_svg]:h-auto'}
    />
  )
}
