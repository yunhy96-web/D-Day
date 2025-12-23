import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  TextInput,
  KeyboardAvoidingView,
  Platform,
  Modal,
  FlatList,
} from 'react-native';
import { router } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useArticles, useCommonCodes } from '@/hooks';
import { useTheme, useAuth } from '@/contexts';
import { showAlert } from '@/utils/alert';
import { spacing, layout, borderRadius, shadows } from '@/styles';
import { CommonCode } from '@/types/api';

// Icon mapping for article types
const getTypeIcon = (code: string): keyof typeof Ionicons.glyphMap => {
  const iconMap: Record<string, keyof typeof Ionicons.glyphMap> = {
    'NORMAL': 'chatbubbles-outline',
    'SECRET': 'lock-closed-outline',
    'NOTICE': 'megaphone-outline',
    'QNA': 'help-circle-outline',
    'TIP': 'bulb-outline',
    'REVIEW': 'star-outline',
  };
  return iconMap[code] || 'folder-outline';
};

export default function AddArticleScreen() {
  const { colors, isDark } = useTheme();
  const { user } = useAuth();
  const { createArticle } = useArticles();
  const { articleTypes, articleTopics } = useCommonCodes();
  const [selectedArticleType, setSelectedArticleType] = useState<CommonCode | null>(null);
  const [selectedTopic, setSelectedTopic] = useState<CommonCode | null>(null);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [errors, setErrors] = useState<{ articleType?: string; topic?: string; title?: string; content?: string }>({});
  const [saving, setSaving] = useState(false);
  const [articleTypeModalVisible, setArticleTypeModalVisible] = useState(false);
  const [topicModalVisible, setTopicModalVisible] = useState(false);

  // Check if user can select article type (DEV or ADMIN only)
  const canSelectArticleType = user?.role === 'DEV' || user?.role === 'ADMIN';

  const validate = () => {
    const newErrors: { articleType?: string; topic?: string; title?: string; content?: string } = {};

    if (canSelectArticleType && !selectedArticleType) {
      newErrors.articleType = 'Please select an article type';
    }

    if (!selectedTopic) {
      newErrors.topic = 'Please select a topic';
    }

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

    setSaving(true);
    const result = await createArticle({
      topic: selectedTopic!.code,
      articleType: canSelectArticleType ? selectedArticleType?.code : undefined,
      title: title.trim(),
      content: content.trim(),
    });
    setSaving(false);

    if (result) {
      router.back();
    } else {
      showAlert('Error', 'Failed to create post.');
    }
  };

  const handleSelectArticleType = (type: CommonCode) => {
    setSelectedArticleType(type);
    setArticleTypeModalVisible(false);
    setErrors((prev) => ({ ...prev, articleType: undefined }));
  };

  const handleSelectTopic = (topic: CommonCode) => {
    setSelectedTopic(topic);
    setTopicModalVisible(false);
    setErrors((prev) => ({ ...prev, topic: undefined }));
  };

  const renderTopicItem = ({ item }: { item: CommonCode }) => (
    <TouchableOpacity
      style={[
        styles.topicModalItem,
        selectedTopic?.code === item.code && { backgroundColor: colors.primary + '15' },
      ]}
      onPress={() => handleSelectTopic(item)}
    >
      <Text
        style={[
          styles.topicModalItemText,
          { color: selectedTopic?.code === item.code ? colors.primary : colors.textPrimary },
        ]}
      >
        {item.label}
      </Text>
      {selectedTopic?.code === item.code && (
        <Ionicons name="checkmark-circle" size={20} color={colors.primary} />
      )}
    </TouchableOpacity>
  );

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
        {/* Article Type Selection (DEV/ADMIN only) */}
        {canSelectArticleType && (
          <View style={[styles.inputCard, { backgroundColor: colors.background }, !isDark && shadows.sm]}>
            <View style={styles.labelRow}>
              <Text style={[styles.label, { color: colors.textSecondary }]}>Article Type</Text>
              <View style={[styles.adminBadge, { backgroundColor: colors.warning + '20' }]}>
                <Ionicons name="shield-checkmark" size={12} color={colors.warning} />
                <Text style={[styles.adminBadgeText, { color: colors.warning }]}>Admin</Text>
              </View>
            </View>
            <TouchableOpacity
              style={[
                styles.topicSelector,
                { backgroundColor: isDark ? colors.gray100 : colors.gray50 },
                errors.articleType && { borderColor: colors.error, borderWidth: 1 },
              ]}
              onPress={() => setArticleTypeModalVisible(true)}
            >
              <Ionicons
                name={selectedArticleType ? getTypeIcon(selectedArticleType.code) : 'apps-outline'}
                size={20}
                color={selectedArticleType ? colors.primary : colors.textTertiary}
              />
              <Text
                style={[
                  styles.topicSelectorText,
                  { color: selectedArticleType ? colors.textPrimary : colors.textTertiary },
                ]}
              >
                {selectedArticleType?.label || 'Select article type'}
              </Text>
              <Ionicons name="chevron-down" size={20} color={colors.textTertiary} />
            </TouchableOpacity>
            {errors.articleType && (
              <Text style={[styles.errorText, { color: colors.error }]}>{errors.articleType}</Text>
            )}
            {selectedArticleType?.code === 'SECRET' && (
              <View style={[styles.secretWarning, { backgroundColor: colors.error + '10' }]}>
                <Ionicons name="lock-closed" size={14} color={colors.error} />
                <Text style={[styles.secretWarningText, { color: colors.error }]}>
                  This post will be visible only to authorized users
                </Text>
              </View>
            )}
          </View>
        )}

        {/* Topic Selection */}
        <View style={[styles.inputCard, { backgroundColor: colors.background }, !isDark && shadows.sm]}>
          <Text style={[styles.label, { color: colors.textSecondary }]}>Topic</Text>
          <TouchableOpacity
            style={[
              styles.topicSelector,
              { backgroundColor: isDark ? colors.gray100 : colors.gray50 },
              errors.topic && { borderColor: colors.error, borderWidth: 1 },
            ]}
            onPress={() => setTopicModalVisible(true)}
          >
            <Ionicons
              name="pricetag-outline"
              size={20}
              color={selectedTopic ? colors.primary : colors.textTertiary}
            />
            <Text
              style={[
                styles.topicSelectorText,
                { color: selectedTopic ? colors.textPrimary : colors.textTertiary },
              ]}
            >
              {selectedTopic?.label || 'Select a topic'}
            </Text>
            <Ionicons name="chevron-down" size={20} color={colors.textTertiary} />
          </TouchableOpacity>
          {errors.topic && (
            <Text style={[styles.errorText, { color: colors.error }]}>{errors.topic}</Text>
          )}
        </View>

        {/* Title Input */}
        <View style={[styles.inputCard, { backgroundColor: colors.background }, !isDark && shadows.sm]}>
          <Text style={[styles.label, { color: colors.textSecondary }]}>Title</Text>
          <TextInput
            style={[
              styles.titleInput,
              { color: colors.textPrimary },
              errors.title && { borderColor: colors.error, borderWidth: 1, borderRadius: borderRadius.lg, padding: spacing[2] },
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

      {/* Action Buttons */}
      <View style={[styles.footer, { backgroundColor: colors.background, borderTopColor: colors.border }]}>
        <TouchableOpacity
          style={[styles.cancelButton, { borderColor: colors.border }]}
          onPress={() => router.back()}
        >
          <Text style={[styles.cancelButtonText, { color: colors.textSecondary }]}>Cancel</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.saveButton, { backgroundColor: colors.primary }]}
          onPress={handleSave}
          disabled={saving}
        >
          {saving ? (
            <Text style={styles.saveButtonText}>Saving...</Text>
          ) : (
            <>
              <Ionicons name="paper-plane" size={18} color="#FFFFFF" />
              <Text style={styles.saveButtonText}>Post</Text>
            </>
          )}
        </TouchableOpacity>
      </View>

      {/* Topic Selection Modal */}
      <Modal
        visible={topicModalVisible}
        animationType="slide"
        transparent
        onRequestClose={() => setTopicModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={[styles.modalContent, { backgroundColor: colors.background }]}>
            <View style={[styles.modalHeader, { borderBottomColor: colors.border }]}>
              <Text style={[styles.modalTitle, { color: colors.textPrimary }]}>Select Topic</Text>
              <TouchableOpacity
                onPress={() => setTopicModalVisible(false)}
                style={[styles.modalCloseButton, { backgroundColor: isDark ? colors.gray200 : colors.gray100 }]}
              >
                <Ionicons name="close" size={20} color={colors.textPrimary} />
              </TouchableOpacity>
            </View>
            <FlatList
              data={articleTopics}
              renderItem={renderTopicItem}
              keyExtractor={(item) => item.code}
              contentContainerStyle={styles.topicModalList}
              showsVerticalScrollIndicator={false}
            />
          </View>
        </View>
      </Modal>

      {/* Article Type Selection Modal (DEV/ADMIN only) */}
      <Modal
        visible={articleTypeModalVisible}
        animationType="slide"
        transparent
        onRequestClose={() => setArticleTypeModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={[styles.modalContent, { backgroundColor: colors.background }]}>
            <View style={[styles.modalHeader, { borderBottomColor: colors.border }]}>
              <Text style={[styles.modalTitle, { color: colors.textPrimary }]}>Select Article Type</Text>
              <TouchableOpacity
                onPress={() => setArticleTypeModalVisible(false)}
                style={[styles.modalCloseButton, { backgroundColor: isDark ? colors.gray200 : colors.gray100 }]}
              >
                <Ionicons name="close" size={20} color={colors.textPrimary} />
              </TouchableOpacity>
            </View>
            <FlatList
              data={articleTypes}
              renderItem={({ item }) => (
                <TouchableOpacity
                  style={[
                    styles.topicModalItem,
                    selectedArticleType?.code === item.code && { backgroundColor: colors.primary + '15' },
                  ]}
                  onPress={() => handleSelectArticleType(item)}
                >
                  <View style={styles.articleTypeItemRow}>
                    <View
                      style={[
                        styles.articleTypeIcon,
                        {
                          backgroundColor: selectedArticleType?.code === item.code
                            ? colors.primary + '20'
                            : (isDark ? colors.gray200 : colors.gray100),
                        },
                      ]}
                    >
                      <Ionicons
                        name={getTypeIcon(item.code)}
                        size={20}
                        color={selectedArticleType?.code === item.code ? colors.primary : colors.textSecondary}
                      />
                    </View>
                    <View style={styles.articleTypeTextContainer}>
                      <Text
                        style={[
                          styles.topicModalItemText,
                          { color: selectedArticleType?.code === item.code ? colors.primary : colors.textPrimary },
                        ]}
                      >
                        {item.label}
                      </Text>
                      {item.code === 'SECRET' && (
                        <Text style={[styles.articleTypeDesc, { color: colors.textTertiary }]}>
                          Restricted visibility
                        </Text>
                      )}
                      {item.code === 'NOTICE' && (
                        <Text style={[styles.articleTypeDesc, { color: colors.textTertiary }]}>
                          Important announcement
                        </Text>
                      )}
                    </View>
                  </View>
                  {selectedArticleType?.code === item.code && (
                    <Ionicons name="checkmark-circle" size={20} color={colors.primary} />
                  )}
                </TouchableOpacity>
              )}
              keyExtractor={(item) => item.code}
              contentContainerStyle={styles.topicModalList}
              showsVerticalScrollIndicator={false}
            />
          </View>
        </View>
      </Modal>
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
  topicSelector: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
    padding: spacing[3],
    borderRadius: borderRadius.lg,
  },
  topicSelectorText: {
    flex: 1,
    fontSize: 16,
  },
  titleInput: {
    fontSize: 18,
    fontWeight: '600',
    paddingVertical: spacing[2],
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
    flexDirection: 'row',
    gap: spacing[3],
    padding: layout.screenPadding,
    paddingTop: spacing[3],
    paddingBottom: spacing[6],
    borderTopWidth: 1,
  },
  cancelButton: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: spacing[4],
    borderRadius: borderRadius.xl,
    borderWidth: 1,
  },
  cancelButtonText: {
    fontSize: 16,
    fontWeight: '600',
  },
  saveButton: {
    flex: 2,
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
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'flex-end',
  },
  modalContent: {
    borderTopLeftRadius: borderRadius.xl,
    borderTopRightRadius: borderRadius.xl,
    maxHeight: '70%',
  },
  modalHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: spacing[4],
    borderBottomWidth: 1,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '700',
  },
  modalCloseButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
  },
  topicModalList: {
    padding: spacing[3],
  },
  topicModalItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: spacing[4],
    borderRadius: borderRadius.lg,
    marginBottom: spacing[1],
  },
  topicModalItemText: {
    fontSize: 16,
  },
  labelRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: spacing[2],
  },
  adminBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[1],
    paddingHorizontal: spacing[2],
    paddingVertical: spacing[1],
    borderRadius: borderRadius.full,
  },
  adminBadgeText: {
    fontSize: 11,
    fontWeight: '600',
  },
  secretWarning: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
    marginTop: spacing[2],
    padding: spacing[2],
    borderRadius: borderRadius.md,
  },
  secretWarningText: {
    fontSize: 12,
    flex: 1,
  },
  articleTypeItemRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[3],
    flex: 1,
  },
  articleTypeIcon: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  articleTypeTextContainer: {
    flex: 1,
  },
  articleTypeDesc: {
    fontSize: 12,
    marginTop: 2,
  },
});
