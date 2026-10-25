// 미리 만든 CSS (설계 §2). 재생성: scripts/tailwind.sh
const v = n => `rgb(var(--${n}) / <alpha-value>)`;
module.exports = {
  darkMode: 'class',
  content: ['src/main/resources/templates/**/*.html', 'src/main/resources/static/*.js'],
  theme: {
    extend: {
      colors: { bg: v('bg'), card: v('card'), fg: v('fg'), muted: v('muted'), line: v('line'), soft: v('soft'), accent: v('accent'), 'on-accent': v('on-accent') },
      minHeight: { tap: '2.8rem' },
      height: { tap: '2.8rem' },
    },
  },
};
