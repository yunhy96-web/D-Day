import React, { useCallback, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  RefreshControl,
  TouchableOpacity,
  Modal,
  Pressable,
  ActivityIndicator,
} from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import { confirm, showAlert } from '@/utils/alert';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { Loading } from '@/components';
import { Sidebar } from '@/components/Sidebar';
import { ArticleCard, ArticleEmpty } from '@/components/article';
import { useArticles, useCommonCodes } from '@/hooks';
import { useTheme, useAuth } from '@/contexts';
import { Article } from '@/types/api';
import { spacing, layout, borderRadius } from '@/styles';

type Language = 'original' | 'ko' | 'th';

export default function HomeScreen() {
  const { colors, isDark } = useTheme();
  const { user } = useAuth();
  const { articles, isLoading, isLoadingMore, error, hasMore, fetchArticles, loadMore, deleteArticle } = useArticles();
  const { articleTypes, articleTopics } = useCommonCodes();
  const [selectedLang, setSelectedLang] = useState<Language>('original');
  const [selectedArticleType, setSelectedArticleType] = useState<string | null>(null);
  const [selectedTopic, setSelectedTopic] = useState<string | null>(null);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [topicModalOpen, setTopicModalOpen] = useState(false);

  // 화면이 focus될 때마다 새로고침
  useFocusEffect(
    useCallback(() => {
      fetchArticles({
        articleType: selectedArticleType || undefined,
        topic: selectedTopic || undefined,
      });
    }, [fetchArticles, selectedArticleType, selectedTopic])
  );

  const handleArticleTypeChange = (articleType: string | null) => {
    setSelectedArticleType(articleType);
    fetchArticles({
      articleType: articleType || undefined,
      topic: selectedTopic || undefined,
    });
  };

  const handleTopicChange = (topic: string | null) => {
    setSelectedTopic(topic);
    setTopicModalOpen(false);
    fetchArticles({
      articleType: selectedArticleType || undefined,
      topic: topic || undefined,
    });
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

  const handleRefresh = useCallback(() => {
    fetchArticles({
      articleType: selectedArticleType || undefined,
      topic: selectedTopic || undefined,
    });
  }, [fetchArticles, selectedArticleType, selectedTopic]);

  const handleAddPress = () => {
    router.push('/article/add');
  };

  const handleProfilePress = () => {
    router.push('/(tabs)/profile');
  };

  const handleCardPress = (article: Article) => {
    router.push(`/article/${article.uuid}`);
  };

  const handleDelete = async (article: Article) => {
    const confirmed = await confirm({
      title: 'Delete Post',
      message: `Are you sure you want to delete "${article.title}"?`,
      confirmText: 'Delete',
    });

    if (confirmed) {
      const success = await deleteArticle(article.uuid);
      if (!success) {
        showAlert('Error', 'Failed to delete post.');
      }
    }
  };

  const canDelete = (article: Article) => {
    if (!user) return false;
    // Check if user is the owner
    if (article.createdBy !== null && article.createdBy === user.userId) return true;
    // Check if user has admin or dev role
    if (user.role === 'ADMIN' || user.role === 'DEV') return true;
    return false;
  };

  const renderItem = ({ item }: { item: Article }) => (
    <ArticleCard
      article={item}
      selectedLang={selectedLang}
      onPress={() => handleCardPress(item)}
      onDelete={canDelete(item) ? () => handleDelete(item) : undefined}
    />
  );

  const getInitial = () => {
    if (user?.nickname) return user.nickname.charAt(0).toUpperCase();
    if (user?.email) return user.email.charAt(0).toUpperCase();
    return 'U';
  };

  const getSelectedTopicName = () => {
    if (!selectedTopic) return 'All Topics';
    const topic = articleTopics.find((t) => t.code === selectedTopic);
    return topic?.label || selectedTopic;
  };

  if (isLoading && articles.length === 0) {
    return <Loading fullScreen text="Loading posts..." />;
  }

  return (
    <SafeAreaView
      style={[styles.container, { backgroundColor: colors.backgroundSecondary }]}
      edges={['top']}
    >
      <View style={[styles.header, { backgroundColor: colors.background }]}>
        <View style={styles.headerTop}>
          {/* Menu Button */}
          <TouchableOpacity
            onPress={() => setSidebarOpen(true)}
            style={[styles.menuButton, { backgroundColor: isDark ? colors.gray200 : colors.gray100 }]}
          >
            <Ionicons name="menu" size={22} color={colors.textPrimary} />
          </TouchableOpacity>

          <Text style={[styles.title, { color: colors.textPrimary }]}>Community</Text>

          <View style={styles.headerRight}>
            {/* Refresh Button */}
            <TouchableOpacity
              onPress={handleRefresh}
              style={[styles.refreshButton, { backgroundColor: isDark ? colors.gray200 : colors.gray100 }]}
              disabled={isLoading}
            >
              <Ionicons
                name="refresh"
                size={20}
                color={isLoading ? colors.textTertiary : colors.textPrimary}
              />
            </TouchableOpacity>

            {/* Profile Button */}
            <TouchableOpacity
              onPress={handleProfilePress}
              style={[styles.profileButton, { backgroundColor: colors.primary }]}
            >
              <Text style={styles.profileInitial}>{getInitial()}</Text>
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.headerBottom}>
          {/* Topic Filter Dropdown */}
          <TouchableOpacity
            onPress={() => setTopicModalOpen(true)}
            style={[styles.topicButton, { backgroundColor: isDark ? colors.gray200 : colors.gray100 }]}
          >
            <Ionicons name="pricetag-outline" size={16} color={colors.primary} />
            <Text style={[styles.topicText, { color: colors.textPrimary }]} numberOfLines={1}>
              {getSelectedTopicName()}
            </Text>
            <Ionicons name="chevron-down" size={14} color={colors.textTertiary} />
          </TouchableOpacity>

          {/* Language Button */}
          <TouchableOpacity
            onPress={toggleLanguage}
            style={[styles.langButton, { backgroundColor: isDark ? colors.gray200 : colors.gray100 }]}
          >
            <Ionicons name="language" size={16} color={colors.primary} />
            <Text style={[styles.langText, { color: colors.textPrimary }]}>
              {getLanguageLabel()}
            </Text>
          </TouchableOpacity>
        </View>
      </View>

      {error && (
        <View
          style={[
            styles.errorContainer,
            { backgroundColor: colors.error + '15' },
          ]}
        >
          <Ionicons name="alert-circle" size={18} color={colors.error} />
          <Text style={[styles.errorText, { color: colors.error }]}>{error}</Text>
        </View>
      )}

      {articles.length === 0 ? (
        <ArticleEmpty onAdd={handleAddPress} />
      ) : (
        <FlatList
          data={articles}
          renderItem={renderItem}
          keyExtractor={(item) => item.uuid}
          contentContainerStyle={styles.listContent}
          refreshControl={
            <RefreshControl
              refreshing={isLoading}
              onRefresh={handleRefresh}
              tintColor={colors.primary}
            />
          }
          showsVerticalScrollIndicator={false}
          onEndReached={() => {
            if (hasMore && !isLoadingMore) {
              loadMore();
            }
          }}
          onEndReachedThreshold={0.5}
          ListFooterComponent={
            isLoadingMore ? (
              <View style={styles.loadingMore}>
                <ActivityIndicator size="small" color={colors.primary} />
              </View>
            ) : null
          }
        />
      )}

      {/* Sidebar for Article Types */}
      <Sidebar
        isOpen={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
        articleTypes={articleTypes}
        selectedType={selectedArticleType}
        onSelectType={handleArticleTypeChange}
      />

      {/* Topic Selection Modal */}
      <Modal
        visible={topicModalOpen}
        transparent
        animationType="fade"
        onRequestClose={() => setTopicModalOpen(false)}
      >
        <Pressable
          style={styles.modalOverlay}
          onPress={() => setTopicModalOpen(false)}
        >
          <View style={[styles.modalContent, { backgroundColor: colors.background }]}>
            <Text style={[styles.modalTitle, { color: colors.textPrimary }]}>Select Topic</Text>

            {/* All Topics Option */}
            <TouchableOpacity
              style={[
                styles.modalOption,
                selectedTopic === null && { backgroundColor: colors.primary + '15' },
              ]}
              onPress={() => handleTopicChange(null)}
            >
              <Ionicons
                name="apps-outline"
                size={20}
                color={selectedTopic === null ? colors.primary : colors.textSecondary}
              />
              <Text
                style={[
                  styles.modalOptionText,
                  { color: selectedTopic === null ? colors.primary : colors.textPrimary },
                ]}
              >
                All Topics
              </Text>
              {selectedTopic === null && (
                <Ionicons name="checkmark-circle" size={20} color={colors.primary} />
              )}
            </TouchableOpacity>

            {/* Topic Options */}
            {articleTopics.map((topic) => {
              const isSelected = selectedTopic === topic.code;
              return (
                <TouchableOpacity
                  key={topic.code}
                  style={[
                    styles.modalOption,
                    isSelected && { backgroundColor: colors.primary + '15' },
                  ]}
                  onPress={() => handleTopicChange(topic.code)}
                >
                  <Ionicons
                    name="pricetag-outline"
                    size={20}
                    color={isSelected ? colors.primary : colors.textSecondary}
                  />
                  <Text
                    style={[
                      styles.modalOptionText,
                      { color: isSelected ? colors.primary : colors.textPrimary },
                    ]}
                  >
                    {topic.label}
                  </Text>
                  {isSelected && (
                    <Ionicons name="checkmark-circle" size={20} color={colors.primary} />
                  )}
                </TouchableOpacity>
              );
            })}
          </View>
        </Pressable>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    paddingHorizontal: layout.screenPadding,
    paddingTop: spacing[4],
    paddingBottom: spacing[3],
  },
  headerTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing[3],
  },
  headerRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
  },
  menuButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  refreshButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerBottom: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
  },
  title: {
    fontSize: 24,
    fontWeight: '800',
  },
  topicButton: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing[3],
    paddingVertical: spacing[2],
    borderRadius: borderRadius.full,
    gap: spacing[1],
  },
  topicText: {
    flex: 1,
    fontSize: 13,
    fontWeight: '500',
  },
  langButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing[3],
    paddingVertical: spacing[2],
    borderRadius: borderRadius.full,
    gap: spacing[1],
  },
  langText: {
    fontSize: 13,
    fontWeight: '500',
  },
  profileButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  profileInitial: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
  listContent: {
    padding: layout.screenPadding,
  },
  loadingMore: {
    paddingVertical: spacing[4],
    alignItems: 'center',
  },
  errorContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
    padding: spacing[3],
    marginHorizontal: layout.screenPadding,
    marginTop: spacing[3],
    borderRadius: borderRadius.lg,
  },
  errorText: {
    fontSize: 14,
    fontWeight: '500',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: spacing[4],
  },
  modalContent: {
    width: '100%',
    maxWidth: 320,
    borderRadius: borderRadius.xl,
    padding: spacing[4],
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 12,
    elevation: 8,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '700',
    marginBottom: spacing[3],
    textAlign: 'center',
  },
  modalOption: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: spacing[3],
    borderRadius: borderRadius.lg,
    marginBottom: spacing[1],
    gap: spacing[3],
  },
  modalOptionText: {
    flex: 1,
    fontSize: 15,
    fontWeight: '500',
  },
});
