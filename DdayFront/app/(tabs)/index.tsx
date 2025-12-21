import React, { useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  RefreshControl,
  TouchableOpacity,
} from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import { confirm, showAlert } from '@/utils/alert';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { Loading } from '@/components';
import { ArticleCard, ArticleEmpty } from '@/components/article';
import { useArticles } from '@/hooks';
import { useTheme } from '@/contexts';
import { Article } from '@/types/api';
import { typography, spacing, layout } from '@/styles';

export default function HomeScreen() {
  const { colors } = useTheme();
  const { articles, isLoading, error, fetchArticles, deleteArticle } = useArticles();

  // 화면이 focus될 때마다 새로고침
  useFocusEffect(
    useCallback(() => {
      fetchArticles();
    }, [fetchArticles])
  );

  const handleRefresh = useCallback(() => {
    fetchArticles();
  }, [fetchArticles]);

  const handleAddPress = () => {
    router.push('/article/add');
  };

  const handleCardPress = (article: Article) => {
    router.push(`/article/${article.uuid}`);
  };

  const handleDelete = async (article: Article) => {
    const confirmed = await confirm({
      title: 'Delete Article',
      message: `Are you sure you want to delete "${article.title}"?`,
      confirmText: 'Delete',
    });

    if (confirmed) {
      const success = await deleteArticle(article.uuid);
      if (!success) {
        showAlert('Error', 'Failed to delete article.');
      }
    }
  };

  const renderItem = ({ item }: { item: Article }) => (
    <ArticleCard
      article={item}
      onPress={() => handleCardPress(item)}
      onDelete={() => handleDelete(item)}
    />
  );

  if (isLoading && articles.length === 0) {
    return <Loading fullScreen text="Loading articles..." />;
  }

  return (
    <SafeAreaView
      style={[styles.container, { backgroundColor: colors.backgroundSecondary }]}
      edges={['top']}
    >
      <View
        style={[
          styles.header,
          { backgroundColor: colors.background, borderBottomColor: colors.border },
        ]}
      >
        <Text style={[styles.title, { color: colors.textPrimary }]}>Articles</Text>
        <TouchableOpacity onPress={handleAddPress} style={styles.addButton}>
          <Ionicons name="add-circle" size={32} color={colors.primary} />
        </TouchableOpacity>
      </View>

      {error && (
        <View
          style={[
            styles.errorContainer,
            { backgroundColor: colors.error + '10' },
          ]}
        >
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
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: layout.screenPadding,
    paddingVertical: spacing[4],
    borderBottomWidth: 1,
  },
  title: {
    ...typography.h2,
  },
  addButton: {
    padding: spacing[1],
  },
  listContent: {
    padding: layout.screenPadding,
  },
  errorContainer: {
    padding: spacing[4],
    marginHorizontal: layout.screenPadding,
    marginTop: spacing[4],
    borderRadius: 8,
  },
  errorText: {
    ...typography.body,
    textAlign: 'center',
  },
});
