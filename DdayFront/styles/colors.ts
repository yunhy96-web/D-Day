// 앱 컬러 팔레트
export const colors = {
  // Primary (Indigo)
  primary: '#6366F1',
  primaryLight: '#818CF8',
  primaryDark: '#4F46E5',

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
  borderFocused: '#6366F1',
};

// 다크모드 컬러
export const darkColors: typeof colors = {
  // Primary (Indigo - 다크모드용 밝은 버전)
  primary: '#818CF8',
  primaryLight: '#A5B4FC',
  primaryDark: '#6366F1',

  // Secondary (동일)
  secondary: '#5E5CE6',
  secondaryLight: '#7A79E0',
  secondaryDark: '#3634A3',

  // Grayscale (반전)
  white: '#0D0D0D',
  gray50: '#1A1A1A',
  gray100: '#252525',
  gray200: '#333333',
  gray300: '#444444',
  gray400: '#666666',
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
  background: '#121212',
  backgroundSecondary: '#1A1A1A',

  // Text
  textPrimary: '#FFFFFF',
  textSecondary: '#A0A0A0',
  textTertiary: '#6B6B6B',
  textInverse: '#000000',

  // Border
  border: '#2A2A2A',
  borderFocused: '#818CF8',
};

export type ThemeColors = typeof colors;
