import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  TextInput,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Loading } from '@/components';
import { useArticles } from '@/hooks';
import { useTheme, useAuth } from '@/contexts';
import { showAlert } from '@/utils/alert';
import { spacing, layout, borderRadius, shadows } from '@/styles';

export default function EditArticleScreen() {
  const { colors, isDark } = useTheme();
  const { user } = useAuth();
  const { uuid } = useLocalSearchParams<{ uuid: string }>();
  const { getArticle, updateArticle, isLoading } = useArticles();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [errors, setErrors] = useState<{ title?: string; content?: string }>({});
  const [initialLoading, setInitialLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadArticle();
  }, [uuid]);

  const loadArticle = async () => {
    if (!uuid) return;

    const article = await getArticle(uuid);
    if (article) {
      // Check permission
      const canEdit =
        (article.createdBy !== null && article.createdBy === user?.userId) ||
        user?.role === 'ADMIN' ||
        user?.role === 'DEV';

      if (!canEdit) {
        showAlert('Error', 'You do not have permission to edit this post.');
        router.back();
        return;
      }

      setTitle(article.title);
      setContent(article.content);
    } else {
      showAlert('Error', 'Post not found.');
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

    setSaving(true);
    const result = await updateArticle(uuid, {
      title: title.trim(),
      content: content.trim(),
    });
    setSaving(false);

    if (result) {
      router.back();
    } else {
      showAlert('Error', 'Failed to update post.');
    }
  };

  if (initialLoading) {
    return <Loading fullScreen text="Loading post..." />;
  }

  return (
    <KeyboardAvoidingView
      style={[styles.container, { backgroundColor: colors.backgroundSecondary }]}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        {/* Title Input */}
        <View style={[styles.inputCard, { backgroundColor: colors.background }, !isDark && shadows.sm]}>
          <Text style={[styles.label, { color: colors.textSecondary }]}>Title</Text>
          <TextInput
            style={[
              styles.titleInput,
              { color: colors.textPrimary },
              errors.title && { borderColor: colors.error, borderWidth: 1 },
            ]}
            placeholder="Enter post title"
            placeholderTextColor={colors.textTertiary}
            value={title}
            onChangeText={setTitle}
            maxLength={100}
          />
          {errors.title && (
            <Text style={[styles.errorText, { color: colors.error }]}>{errors.title}</Text>
          )}
        </View>

        {/* Content Input */}
        <View style={[styles.inputCard, { backgroundColor: colors.background }, !isDark && shadows.sm]}>
          <Text style={[styles.label, { color: colors.textSecondary }]}>Content</Text>
          <TextInput
            style={[
              styles.contentInput,
              { color: colors.textPrimary, backgroundColor: isDark ? colors.gray100 : colors.gray50 },
              errors.content && { borderColor: colors.error, borderWidth: 1 },
            ]}
            placeholder="Write your post content here..."
            placeholderTextColor={colors.textTertiary}
            value={content}
            onChangeText={setContent}
            multiline
            textAlignVertical="top"
          />
          {errors.content && (
            <Text style={[styles.errorText, { color: colors.error }]}>{errors.content}</Text>
          )}
        </View>

        {/* Info */}
        <View style={[styles.infoCard, { backgroundColor: colors.primary + '10' }]}>
          <Ionicons name="information-circle-outline" size={18} color={colors.primary} />
          <Text style={[styles.infoText, { color: colors.primary }]}>
            Your post will be automatically translated to Korean and Thai.
          </Text>
        </View>
      </ScrollView>

      {/* Save Button */}
      <View style={[styles.footer, { backgroundColor: colors.background, borderTopColor: colors.border }]}>
        <TouchableOpacity
          style={[styles.saveButton, { backgroundColor: colors.primary }]}
          onPress={handleSave}
          disabled={saving}
        >
          {saving ? (
            <Text style={styles.saveButtonText}>Saving...</Text>
          ) : (
            <>
              <Ionicons name="checkmark" size={20} color="#FFFFFF" />
              <Text style={styles.saveButtonText}>Save Changes</Text>
            </>
          )}
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    padding: layout.screenPadding,
    paddingBottom: spacing[4],
  },
  inputCard: {
    padding: spacing[4],
    borderRadius: borderRadius.xl,
    marginBottom: spacing[4],
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
    marginBottom: spacing[2],
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  titleInput: {
    fontSize: 18,
    fontWeight: '600',
    paddingVertical: spacing[2],
    borderRadius: borderRadius.lg,
  },
  contentInput: {
    fontSize: 16,
    lineHeight: 24,
    minHeight: 200,
    padding: spacing[3],
    borderRadius: borderRadius.lg,
  },
  errorText: {
    fontSize: 12,
    marginTop: spacing[2],
  },
  infoCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
    padding: spacing[3],
    borderRadius: borderRadius.lg,
  },
  infoText: {
    flex: 1,
    fontSize: 13,
  },
  footer: {
    padding: layout.screenPadding,
    paddingTop: spacing[3],
    paddingBottom: spacing[6],
    borderTopWidth: 1,
  },
  saveButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing[2],
    paddingVertical: spacing[4],
    borderRadius: borderRadius.xl,
  },
  saveButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
});
