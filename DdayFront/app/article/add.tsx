import React, { useState } from 'react';
import {
  View,
  StyleSheet,
  ScrollView,
} from 'react-native';
import { router } from 'expo-router';
import { Button, Input } from '@/components';
import { useArticles } from '@/hooks';
import { useTheme } from '@/contexts';
import { showAlert } from '@/utils/alert';
import { spacing, layout } from '@/styles';

export default function AddArticleScreen() {
  const { colors } = useTheme();
  const { createArticle, isLoading } = useArticles();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [errors, setErrors] = useState<{ title?: string; content?: string }>({});

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
    if (!validate()) return;

    const result = await createArticle({
      title: title.trim(),
      content: content.trim(),
    });

    if (result) {
      router.back();
    } else {
      showAlert('Error', 'Failed to create article.');
    }
  };

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
        autoFocus
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
          title="Cancel"
          variant="outline"
          onPress={() => router.back()}
          fullWidth
          style={styles.cancelButton}
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
  cancelButton: {
    marginTop: spacing[3],
  },
});
