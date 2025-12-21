import React, { ReactNode } from 'react';
import { View, StyleSheet, ViewStyle, TouchableOpacity } from 'react-native';
import { useTheme } from '@/contexts';
import { borderRadius, spacing, shadows } from '@/styles';

interface CardProps {
  children: ReactNode;
  onPress?: () => void;
  style?: ViewStyle;
  padding?: keyof typeof spacing;
  shadow?: keyof typeof shadows;
}

export function Card({
  children,
  onPress,
  style,
  padding = 4,
  shadow = 'base',
}: CardProps) {
  const { colors, isDark } = useTheme();

  const cardStyle = [
    styles.card,
    {
      padding: spacing[padding],
      backgroundColor: isDark ? colors.gray100 : colors.background,
      borderColor: colors.border,
    },
    !isDark && shadows[shadow],
    style,
  ];

  if (onPress) {
    return (
      <TouchableOpacity style={cardStyle} onPress={onPress} activeOpacity={0.7}>
        {children}
      </TouchableOpacity>
    );
  }

  return <View style={cardStyle}>{children}</View>;
}

const styles = StyleSheet.create({
  card: {
    borderRadius: borderRadius.md,
    borderWidth: 1,
  },
});
