// Tailwind CDN 대신 미리 만든 CSS (설계 §15-11). 재생성: scripts/tailwind.sh
// JS 가 className 문자열로 넣는 클래스도 잡도록 static/*.js 를 content 에 포함
const v = n => `rgb(var(--${n}) / <alpha-value>)`;
module.exports = {
  content: ['src/main/resources/templates/**/*.html', 'src/main/resources/static/*.js'],
  theme: { extend: { colors: { mist: v('mist'), ink: v('ink'), sky: v('sky'), cloud: v('cloud'), jasmine: v('jasmine'), night: '#0F141B' } } },
};
