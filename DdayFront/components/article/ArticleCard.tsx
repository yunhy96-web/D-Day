import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Article } from '@/types/api';
import { useTheme } from '@/contexts';
import { spacing, borderRadius, shadows } from '@/styles';

type Language = 'original' | 'ko' | 'th';

interface ArticleCardProps {
  article: Article;
  selectedLang?: Language;
  onPress?: () => void;
  onDelete?: () => void;
}

export function ArticleCard({ article, selectedLang = 'original', onPress, onDelete }: ArticleCardProps) {
  const { colors, isDark } = useTheme();

  // 선택된 언어에 맞는 번역본 사용
  const getTitle = () => {
    if (article.translationStatus !== 'COMPLETED') return article.title;
    switch (selectedLang) {
      case 'ko': return article.titleKo || article.title;
      case 'th': return article.titleTh || article.title;
      default: return article.title;
    }
  };

  const getContent = () => {
    if (article.translationStatus !== 'COMPLETED') return article.content;
    switch (selectedLang) {
      case 'ko': return article.contentKo || article.content;
      case 'th': return article.contentTh || article.content;
      default: return article.content;
    }
  };

  const title = getTitle();
  const content = getContent();

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
    });
  };

  const truncateContent = (text: string, maxLength: number = 80) => {
    if (text.length <= maxLength) return text;
    return text.slice(0, maxLength) + '...';
  };

  const getInitial = (name: string) => {
    return name?.charAt(0).toUpperCase() || '?';
  };

  return (
    <TouchableOpacity
      style={[
        styles.container,
        {
          backgroundColor: isDark ? colors.gray100 : colors.background,
        },
        !isDark && shadows.md,
      ]}
      onPress={onPress}
      activeOpacity={0.8}
    >
      {/* Author Info */}
      <View style={styles.header}>
        <View style={styles.authorSection}>
          <View style={[styles.avatar, { backgroundColor: colors.primary }]}>
            <Text style={styles.avatarText}>{getInitial(article.authorNickname)}</Text>
          </View>
          <View style={styles.authorInfo}>
            <Text style={[styles.authorName, { color: colors.textPrimary }]}>
              {article.authorNickname || 'Anonymous'}
            </Text>
            <Text style={[styles.date, { color: colors.textTertiary }]}>
              {formatDate(article.createdAt)}
            </Text>
          </View>
        </View>
        {onDelete && (
          <TouchableOpacity
            style={[styles.deleteButton, { backgroundColor: colors.error + '15' }]}
            onPress={(e) => {
              e.stopPropagation();
              onDelete();
            }}
            hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
          >
            <Ionicons name="trash-outline" size={16} color={colors.error} />
          </TouchableOpacity>
        )}
      </View>

      {/* Content */}
      <View style={styles.content}>
        <Text style={[styles.title, { color: colors.textPrimary }]} numberOfLines={2}>
          {title}
        </Text>
        <Text style={[styles.preview, { color: colors.textSecondary }]} numberOfLines={2}>
          {truncateContent(content)}
        </Text>
      </View>

      {/* Badges */}
      <View style={styles.badgeContainer}>
        {article.topic && (
          <View style={[styles.badge, { backgroundColor: colors.primary + '15' }]}>
            <Ionicons name="folder-outline" size={12} color={colors.primary} />
            <Text style={[styles.badgeText, { color: colors.primary }]}>{article.topic}</Text>
          </View>
        )}
        {article.translationStatus === 'COMPLETED' && (
          <View style={[styles.badge, { backgroundColor: colors.success + '20' }]}>
            <Ionicons name="language" size={12} color={colors.success} />
            <Text style={[styles.badgeText, { color: colors.success }]}>Translated</Text>
          </View>
        )}
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: {
    borderRadius: borderRadius.xl,
    padding: spacing[4],
    marginBottom: spacing[4],
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: spacing[3],
  },
  authorSection: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[3],
  },
  avatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
  authorInfo: {
    gap: 2,
  },
  authorName: {
    fontSize: 14,
    fontWeight: '600',
  },
  date: {
    fontSize: 12,
  },
  deleteButton: {
    padding: spacing[2],
    borderRadius: borderRadius.full,
  },
  content: {
    gap: spacing[2],
  },
  title: {
    fontSize: 17,
    fontWeight: '700',
    lineHeight: 24,
  },
  preview: {
    fontSize: 14,
    lineHeight: 20,
  },
  badgeContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing[2],
    marginTop: spacing[3],
  },
  badge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: spacing[2],
    paddingVertical: 4,
    borderRadius: borderRadius.full,
  },
  badgeText: {
    fontSize: 11,
    fontWeight: '600',
  },
});
