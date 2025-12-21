import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Article } from '@/types/api';
import { useTheme } from '@/contexts';
import { typography, spacing, borderRadius, shadows } from '@/styles';

interface ArticleCardProps {
  article: Article;
  onPress?: () => void;
  onDelete?: () => void;
}

export function ArticleCard({ article, onPress, onDelete }: ArticleCardProps) {
  const { colors, isDark } = useTheme();

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  const truncateContent = (content: string, maxLength: number = 100) => {
    if (content.length <= maxLength) return content;
    return content.slice(0, maxLength) + '...';
  };

  return (
    <View
      style={[
        styles.container,
        {
          backgroundColor: isDark ? colors.gray100 : colors.background,
          borderColor: colors.border,
        },
        !isDark && shadows.base,
      ]}
    >
      <TouchableOpacity
        style={styles.content}
        onPress={onPress}
        activeOpacity={0.7}
      >
        <Text style={[styles.title, { color: colors.textPrimary }]} numberOfLines={1}>
          {article.title}
        </Text>
        <Text style={[styles.preview, { color: colors.textSecondary }]} numberOfLines={2}>
          {truncateContent(article.content)}
        </Text>
        <Text style={[styles.date, { color: colors.textTertiary }]}>
          {formatDate(article.createdAt)}
        </Text>
      </TouchableOpacity>

      {onDelete && (
        <TouchableOpacity
          style={styles.deleteButton}
          onPress={onDelete}
          hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
        >
          <Ionicons name="trash-outline" size={18} color={colors.error} />
        </TouchableOpacity>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    borderRadius: borderRadius.md,
    padding: spacing[4],
    marginBottom: spacing[3],
    borderWidth: 1,
  },
  content: {
    flex: 1,
    paddingRight: spacing[6],
  },
  title: {
    ...typography.h4,
    marginBottom: spacing[2],
  },
  preview: {
    ...typography.body,
    marginBottom: spacing[2],
  },
  date: {
    ...typography.caption,
  },
  deleteButton: {
    position: 'absolute',
    top: spacing[2],
    right: spacing[2],
    padding: spacing[1],
  },
});
