import React, { useState, useEffect } from 'react';
import {
  View,
  StyleSheet,
  ScrollView,
} from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { Button, Input, Loading } from '@/components';
import { useArticles } from '@/hooks';
import { useTheme } from '@/contexts';
import { confirm, showAlert } from '@/utils/alert';
import { spacing, layout } from '@/styles';

export default function EditArticleScreen() {
  const { colors } = useTheme();
  const { uuid } = useLocalSearchParams<{ uuid: string }>();
  const { getArticle, updateArticle, deleteArticle, isLoading } = useArticles();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [errors, setErrors] = useState<{ title?: string; content?: string }>({});
  const [initialLoading, setInitialLoading] = useState(true);

  useEffect(() => {
    loadArticle();
  }, [uuid]);

  const loadArticle = async () => {
    if (!uuid) return;

    const article = await getArticle(uuid);
    if (article) {
      setTitle(article.title);
      setContent(article.content);
    } else {
      showAlert('Error', 'Article not found.');
      router.back();
    }
    setInitialLoading(false);
  };

  const validate = () => {
    const newErrors: { title?: string; content?: string } = {};

    if (!title.trim()) {
      newErrors.title = 'Please enter a title';
    }

    if (!content.trim()) {
      newErrors.content = 'Please enter content';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSave = async () => {
    if (!validate() || !uuid) return;

    const result = await updateArticle(uuid, {
      title: title.trim(),
      content: content.trim(),
    });

    if (result) {
      router.back();
    } else {
      showAlert('Error', 'Failed to update article.');
    }
  };

  const handleDelete = async () => {
    if (!uuid) return;

    const confirmed = await confirm({
      title: 'Delete Article',
      message: 'Are you sure you want to delete this article?',
      confirmText: 'Delete',
    });

    if (confirmed) {
      const success = await deleteArticle(uuid);
      if (success) {
        router.back();
      } else {
        showAlert('Error', 'Failed to delete article.');
      }
    }
  };

  if (initialLoading) {
    return <Loading fullScreen text="Loading article..." />;
  }

  return (
    <ScrollView
      style={[styles.container, { backgroundColor: colors.background }]}
      contentContainerStyle={styles.content}
      keyboardShouldPersistTaps="handled"
    >
      <Input
        label="Title"
        placeholder="Enter article title"
        value={title}
        onChangeText={setTitle}
        error={errors.title}
      />

      <Input
        label="Content"
        placeholder="Write your article content here..."
        value={content}
        onChangeText={setContent}
        error={errors.content}
        multiline
        numberOfLines={10}
        containerStyle={styles.contentInput}
      />

      <View style={styles.buttonContainer}>
        <Button
          title="Save"
          onPress={handleSave}
          loading={isLoading}
          fullWidth
        />
        <Button
          title="Delete"
          variant="outline"
          onPress={handleDelete}
          fullWidth
          style={{ ...styles.deleteButton, borderColor: colors.error }}
          textStyle={{ color: colors.error }}
        />
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    padding: layout.screenPadding,
  },
  contentInput: {
    marginBottom: spacing[4],
  },
  buttonContainer: {
    marginTop: spacing[8],
  },
  deleteButton: {
    marginTop: spacing[3],
  },
});
