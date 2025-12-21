// 앱 컬러 팔레트
export const colors = {
  // Primary
  primary: '#007AFF',
  primaryLight: '#4DA3FF',
  primaryDark: '#0055CC',

  // Secondary
  secondary: '#5856D6',
  secondaryLight: '#7A79E0',
  secondaryDark: '#3634A3',

  // Grayscale
  white: '#FFFFFF',
  gray50: '#F9FAFB',
  gray100: '#F3F4F6',
  gray200: '#E5E7EB',
  gray300: '#D1D5DB',
  gray400: '#9CA3AF',
  gray500: '#6B7280',
  gray600: '#4B5563',
  gray700: '#374151',
  gray800: '#1F2937',
  gray900: '#111827',
  black: '#000000',

  // Semantic
  success: '#34C759',
  warning: '#FF9500',
  error: '#FF3B30',
  info: '#5AC8FA',

  // Background
  background: '#FFFFFF',
  backgroundSecondary: '#F2F2F7',

  // Text
  textPrimary: '#000000',
  textSecondary: '#6B7280',
  textTertiary: '#9CA3AF',
  textInverse: '#FFFFFF',

  // Border
  border: '#E5E7EB',
  borderFocused: '#007AFF',
};

// 다크모드 컬러
export const darkColors: typeof colors = {
  // Primary (동일)
  primary: '#0A84FF',
  primaryLight: '#4DA3FF',
  primaryDark: '#0055CC',

  // Secondary (동일)
  secondary: '#5E5CE6',
  secondaryLight: '#7A79E0',
  secondaryDark: '#3634A3',

  // Grayscale (반전)
  white: '#000000',
  gray50: '#1C1C1E',
  gray100: '#2C2C2E',
  gray200: '#3A3A3C',
  gray300: '#48484A',
  gray400: '#636366',
  gray500: '#8E8E93',
  gray600: '#AEAEB2',
  gray700: '#C7C7CC',
  gray800: '#D1D1D6',
  gray900: '#E5E5EA',
  black: '#FFFFFF',

  // Semantic (조금 밝게)
  success: '#30D158',
  warning: '#FF9F0A',
  error: '#FF453A',
  info: '#64D2FF',

  // Background
  background: '#000000',
  backgroundSecondary: '#1C1C1E',

  // Text
  textPrimary: '#FFFFFF',
  textSecondary: '#8E8E93',
  textTertiary: '#636366',
  textInverse: '#000000',

  // Border
  border: '#38383A',
  borderFocused: '#0A84FF',
};

export type ThemeColors = typeof colors;
