// 스타일 통합 export
export { colors, darkColors } from './colors';
export { fontSize, fontWeight, lineHeight, typography } from './typography';
export { spacing, borderRadius, shadows, layout } from './spacing';

// 테마 객체 (한번에 import 가능)
import { colors } from './colors';
import { typography } from './typography';
import { spacing, borderRadius, shadows, layout } from './spacing';

export const theme = {
  colors,
  typography,
  spacing,
  borderRadius,
  shadows,
  layout,
};
