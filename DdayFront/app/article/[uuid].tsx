import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  RefreshControl,
} from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Loading } from '@/components';
import { useArticles } from '@/hooks';
import { useTheme, useAuth } from '@/contexts';
import { confirm, showAlert } from '@/utils/alert';
import { spacing, layout, borderRadius, shadows } from '@/styles';
import { Article } from '@/types/api';

type Language = 'original' | 'ko' | 'th';

export default function ArticleDetailScreen() {
  const { colors, isDark } = useTheme();
  const { user } = useAuth();
  const { uuid } = useLocalSearchParams<{ uuid: string }>();
  const { getArticle, deleteArticle, isLoading } = useArticles();

  const [article, setArticle] = useState<Article | null>(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const [selectedLang, setSelectedLang] = useState<Language>('original');

  useEffect(() => {
    loadArticle();
  }, [uuid]);

  const loadArticle = async () => {
    if (!uuid) return;

    const data = await getArticle(uuid);
    if (data) {
      setArticle(data);
    } else {
      showAlert('Error', 'Article not found.');
      router.back();
    }
    setInitialLoading(false);
  };

  const canModify = useCallback(() => {
    if (!user || !article) return false;
    // Check if user is the owner
    if (article.createdBy !== null && article.createdBy === user.userId) return true;
    // Check if user has admin or dev role
    if (user.role === 'ADMIN' || user.role === 'DEV') return true;
    return false;
  }, [user, article]);

  const handleEdit = () => {
    router.push(`/article/edit/${uuid}`);
  };

  const handleDelete = async () => {
    if (!uuid) return;

    const confirmed = await confirm({
      title: 'Delete Post',
      message: 'Are you sure you want to delete this post? This action cannot be undone.',
      confirmText: 'Delete',
    });

    if (confirmed) {
      const success = await deleteArticle(uuid);
      if (success) {
        router.back();
      } else {
        showAlert('Error', 'Failed to delete post.');
      }
    }
  };

  const getTitle = () => {
    if (!article || article.translationStatus !== 'COMPLETED') return article?.title || '';
    switch (selectedLang) {
      case 'ko': return article.titleKo || article.title;
      case 'th': return article.titleTh || article.title;
      default: return article.title;
    }
  };

  const getContent = () => {
    if (!article || article.translationStatus !== 'COMPLETED') return article?.content || '';
    switch (selectedLang) {
      case 'ko': return article.contentKo || article.content;
      case 'th': return article.contentTh || article.content;
      default: return article.content;
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getInitial = (name: string) => {
    return name?.charAt(0).toUpperCase() || '?';
  };

  const toggleLanguage = () => {
    setSelectedLang((prev) => {
      if (prev === 'original') return 'ko';
      if (prev === 'ko') return 'th';
      return 'original';
    });
  };

  const getLanguageLabel = () => {
    switch (selectedLang) {
      case 'ko': return '한국어';
      case 'th': return 'ไทย';
      default: return 'Original';
    }
  };

  if (initialLoading) {
    return <Loading fullScreen text="Loading post..." />;
  }

  if (!article) {
    return null;
  }

  return (
    <SafeAreaView
      style={[styles.container, { backgroundColor: colors.background }]}
      edges={['top', 'bottom']}
    >
      {/* Header - 블라인드 스타일 */}
      <View style={styles.header}>
        <TouchableOpacity
          onPress={() => router.back()}
          style={styles.headerIconButton}
        >
          <Ionicons name="chevron-back" size={24} color={colors.textPrimary} />
        </TouchableOpacity>

        <View style={styles.headerRight}>
          {article.translationStatus === 'COMPLETED' && (
            <TouchableOpacity
              onPress={toggleLanguage}
              style={styles.headerIconButton}
            >
              <Ionicons name="language" size={22} color={colors.textPrimary} />
            </TouchableOpacity>
          )}
          <TouchableOpacity style={styles.headerIconButton}>
            <Ionicons name="share-outline" size={22} color={colors.textPrimary} />
          </TouchableOpacity>
          <TouchableOpacity style={styles.headerIconButton}>
            <Ionicons name="bookmark-outline" size={22} color={colors.textPrimary} />
          </TouchableOpacity>
          <TouchableOpacity style={styles.headerIconButton}>
            <Ionicons name="ellipsis-horizontal" size={22} color={colors.textPrimary} />
          </TouchableOpacity>
        </View>
      </View>

      {/* Header Divider */}
      <View style={[styles.headerDivider, { backgroundColor: colors.border }]} />

      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={true}
        refreshControl={
          <RefreshControl
            refreshing={isLoading}
            onRefresh={loadArticle}
            tintColor={colors.primary}
          />
        }
      >
        {/* Article Card - 제목, 작성자, 수정/삭제 통합 */}
        <View style={[styles.articleCard, { backgroundColor: isDark ? colors.gray100 : colors.background }, !isDark && shadows.sm]}>
          {/* 제목 */}
          <Text style={[styles.title, { color: colors.textPrimary }]}>
            {getTitle()}
          </Text>

          {/* 작성자 정보 + 수정/삭제 버튼 */}
          <View style={styles.metaRow}>
            <View style={styles.authorSection}>
              <View style={[styles.avatar, { backgroundColor: colors.primary }]}>
                <Text style={styles.avatarText}>{getInitial(article.authorNickname)}</Text>
              </View>
              <View style={styles.authorInfo}>
                <View style={styles.authorNameRow}>
                  <Text style={[styles.authorName, { color: colors.textPrimary }]}>
                    {article.authorNickname || 'Anonymous'}
                  </Text>
                  {article.translationStatus === 'COMPLETED' && (
                    <View style={[styles.badge, { backgroundColor: colors.success + '20' }]}>
                      <Ionicons name="checkmark-circle" size={12} color={colors.success} />
                      <Text style={[styles.badgeText, { color: colors.success }]}>Translated</Text>
                    </View>
                  )}
                </View>
                <Text style={[styles.date, { color: colors.textTertiary }]}>
                  {formatDate(article.createdAt)}
                </Text>
              </View>
            </View>

            {canModify() && (
              <View style={styles.actionButtons}>
                <TouchableOpacity
                  onPress={handleEdit}
                  style={[styles.actionButton, { backgroundColor: colors.primary + '15' }]}
                >
                  <Ionicons name="pencil" size={16} color={colors.primary} />
                </TouchableOpacity>
                <TouchableOpacity
                  onPress={handleDelete}
                  style={[styles.actionButton, { backgroundColor: colors.error + '15' }]}
                >
                  <Ionicons name="trash" size={16} color={colors.error} />
                </TouchableOpacity>
              </View>
            )}
          </View>

          {/* 구분선 */}
          <View style={[styles.divider, { backgroundColor: isDark ? colors.gray200 : colors.gray100 }]} />

          {/* 본문 */}
          <Text style={[styles.content, { color: colors.textSecondary }]}>
            {getContent()}
          </Text>
        </View>

        {/* Translation Info */}
        {article.translationStatus === 'PENDING' && (
          <View style={[styles.infoCard, { backgroundColor: colors.warning + '15' }]}>
            <Ionicons name="time-outline" size={20} color={colors.warning} />
            <Text style={[styles.infoText, { color: colors.warning }]}>
              Translation in progress...
            </Text>
          </View>
        )}

        {article.updatedAt !== article.createdAt && (
          <Text style={[styles.editedText, { color: colors.textTertiary }]}>
            Edited {formatDate(article.updatedAt)}
          </Text>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing[2],
    paddingVertical: spacing[2],
  },
  headerIconButton: {
    width: 44,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerRight: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  headerDivider: {
    height: 1,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    padding: layout.screenPadding,
    paddingBottom: spacing[10],
  },
  articleCard: {
    padding: spacing[5],
    borderRadius: borderRadius.xl,
    marginBottom: spacing[4],
  },
  title: {
    fontSize: 22,
    fontWeight: '700',
    lineHeight: 30,
    marginBottom: spacing[4],
  },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: spacing[4],
  },
  authorSection: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
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
    flex: 1,
    marginLeft: spacing[3],
    gap: 2,
  },
  authorNameRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
  },
  authorName: {
    fontSize: 14,
    fontWeight: '600',
  },
  date: {
    fontSize: 12,
  },
  badge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 2,
    paddingHorizontal: spacing[2],
    paddingVertical: 2,
    borderRadius: borderRadius.full,
  },
  badgeText: {
    fontSize: 10,
    fontWeight: '600',
  },
  actionButtons: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
  },
  actionButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
  },
  divider: {
    height: 1,
    marginBottom: spacing[4],
  },
  content: {
    fontSize: 16,
    lineHeight: 26,
  },
  infoCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
    padding: spacing[4],
    borderRadius: borderRadius.xl,
    marginBottom: spacing[4],
  },
  infoText: {
    fontSize: 14,
    fontWeight: '500',
  },
  editedText: {
    fontSize: 12,
    textAlign: 'center',
  },
});
