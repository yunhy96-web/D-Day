import { useState, useEffect } from 'react';
import { StyleSheet, Text, View, TextInput, TouchableOpacity, ScrollView, Alert } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter, useLocalSearchParams } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useSites } from '@/contexts';
import { colors, layout, spacing, shadows } from '@/styles';

export default function AddSiteScreen() {
  const router = useRouter();
  const { id } = useLocalSearchParams<{ id?: string }>();
  const { sites, addSite, updateSite } = useSites();

  const editingSite = id ? sites.find((s) => s.id === id) : null;
  const isEditing = !!editingSite;

  const [name, setName] = useState('');
  const [url, setUrl] = useState('');
  const [baseUrl, setBaseUrl] = useState('');
  const [selector, setSelector] = useState('');
  const [countSelector, setCountSelector] = useState('');
  const [refreshInterval, setRefreshInterval] = useState('30');

  useEffect(() => {
    if (editingSite) {
      setName(editingSite.name);
      setUrl(editingSite.url);
      setBaseUrl(editingSite.baseUrl);
      setSelector(editingSite.selector);
      setCountSelector(editingSite.countSelector ?? '');
      setRefreshInterval(editingSite.refreshInterval.toString());
    }
  }, [editingSite]);

  const handleSave = async () => {
    if (!name.trim() || !url.trim() || !selector.trim()) {
      Alert.alert('입력 오류', '이름, URL, 셀렉터는 필수입니다.');
      return;
    }

    const computedBaseUrl = baseUrl.trim() || new URL(url.trim()).origin;

    try {
      if (isEditing && editingSite) {
        await updateSite(editingSite.id, {
          name: name.trim(),
          url: url.trim(),
          baseUrl: computedBaseUrl,
          selector: selector.trim(),
          countSelector: countSelector.trim() || undefined,
          refreshInterval: parseInt(refreshInterval, 10) || 30,
        });
      } else {
        await addSite({
          name: name.trim(),
          url: url.trim(),
          baseUrl: computedBaseUrl,
          selector: selector.trim(),
          countSelector: countSelector.trim() || undefined,
          refreshInterval: parseInt(refreshInterval, 10) || 30,
          isActive: true,
        });
      }
      router.back();
    } catch {
      Alert.alert('오류', '저장에 실패했습니다.');
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      {/* 헤더 */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
          <Ionicons name="arrow-back" size={24} color={colors.textPrimary} />
        </TouchableOpacity>
        <Text style={styles.title}>{isEditing ? '사이트 수정' : '사이트 추가'}</Text>
        <View style={{ width: 32 }} />
      </View>

      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.field}>
          <Text style={styles.label}>사이트 이름 *</Text>
          <TextInput
            style={styles.input}
            value={name}
            onChangeText={setName}
            placeholder="예: RRL Double RL 데님"
            placeholderTextColor={colors.textTertiary}
          />
        </View>

        <View style={styles.field}>
          <Text style={styles.label}>URL *</Text>
          <TextInput
            style={styles.input}
            value={url}
            onChangeText={setUrl}
            placeholder="https://..."
            placeholderTextColor={colors.textTertiary}
            autoCapitalize="none"
            keyboardType="url"
          />
        </View>

        <View style={styles.field}>
          <Text style={styles.label}>Base URL</Text>
          <Text style={styles.hint}>비워두면 URL에서 자동 추출</Text>
          <TextInput
            style={styles.input}
            value={baseUrl}
            onChangeText={setBaseUrl}
            placeholder="예: https://www.ralphlauren.co.kr"
            placeholderTextColor={colors.textTertiary}
            autoCapitalize="none"
          />
        </View>

        <View style={styles.field}>
          <Text style={styles.label}>상품 링크 CSS 셀렉터 *</Text>
          <Text style={styles.hint}>상품 이름이 포함된 a 태그의 셀렉터</Text>
          <TextInput
            style={styles.input}
            value={selector}
            onChangeText={setSelector}
            placeholder="예: a.name-link.js-pdp-link"
            placeholderTextColor={colors.textTertiary}
            autoCapitalize="none"
          />
        </View>

        <View style={styles.field}>
          <Text style={styles.label}>상품 수 셀렉터 (선택)</Text>
          <Text style={styles.hint}>비워두면 링크 개수로 카운트</Text>
          <TextInput
            style={styles.input}
            value={countSelector}
            onChangeText={setCountSelector}
            placeholder={'예: input[name="totalProductsCount"]'}
            placeholderTextColor={colors.textTertiary}
            autoCapitalize="none"
          />
        </View>

        <View style={styles.field}>
          <Text style={styles.label}>새로고침 주기 (초)</Text>
          <TextInput
            style={styles.input}
            value={refreshInterval}
            onChangeText={setRefreshInterval}
            placeholder="30"
            placeholderTextColor={colors.textTertiary}
            keyboardType="number-pad"
          />
        </View>
      </ScrollView>

      <TouchableOpacity style={styles.saveButton} onPress={handleSave} activeOpacity={0.7}>
        <Ionicons name="checkmark-outline" size={24} color={colors.white} />
        <Text style={styles.saveButtonText}>{isEditing ? '수정 완료' : '추가하기'}</Text>
      </TouchableOpacity>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
    paddingHorizontal: layout.screenPadding,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: spacing[4],
    marginBottom: spacing[5],
  },
  backButton: {
    padding: spacing[1],
  },
  title: {
    flex: 1,
    fontSize: 20,
    fontWeight: '700',
    color: colors.textPrimary,
    textAlign: 'center',
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingBottom: spacing[8],
  },
  field: {
    marginBottom: spacing[5],
  },
  label: {
    fontSize: 14,
    fontWeight: '600',
    color: colors.textPrimary,
    marginBottom: spacing[2],
  },
  hint: {
    fontSize: 12,
    color: colors.textTertiary,
    marginBottom: spacing[2],
  },
  input: {
    backgroundColor: colors.cardBackground,
    borderRadius: layout.buttonBorderRadius,
    borderWidth: 1,
    borderColor: colors.border,
    paddingHorizontal: spacing[4],
    paddingVertical: spacing[3],
    fontSize: 15,
    color: colors.textPrimary,
  },
  saveButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing[2],
    backgroundColor: colors.primary,
    borderRadius: layout.buttonBorderRadius,
    paddingVertical: spacing[4],
    marginBottom: spacing[6],
    ...shadows.md,
  },
  saveButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: colors.white,
  },
});
