import { useEffect, useState } from 'react';
import {
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  ScrollView,
  TextInput,
  ActivityIndicator,
  Alert,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useSites } from '@/contexts';
import { colors, layout, spacing } from '@/styles';
import type { Site } from '@/types';

type Draft = {
  include: string[];
  exclude: string[];
  includeInput: string;
  excludeInput: string;
  saving: boolean;
};

export default function SettingsScreen() {
  const router = useRouter();
  const { sites, updateSite, isLoading } = useSites();
  const [drafts, setDrafts] = useState<Record<string, Draft>>({});

  useEffect(() => {
    setDrafts((prev) => {
      const next: Record<string, Draft> = {};
      for (const s of sites) {
        const existing = prev[s.id];
        next[s.id] = existing
          ? existing
          : {
              include: [...s.includeKeywords],
              exclude: [...s.excludeKeywords],
              includeInput: '',
              excludeInput: '',
              saving: false,
            };
      }
      return next;
    });
  }, [sites]);

  const updateDraft = (id: string, patch: Partial<Draft>) => {
    setDrafts((prev) => ({ ...prev, [id]: { ...prev[id], ...patch } }));
  };

  const addKeyword = (id: string, kind: 'include' | 'exclude') => {
    const draft = drafts[id];
    if (!draft) return;
    const raw = kind === 'include' ? draft.includeInput : draft.excludeInput;
    const value = raw.trim();
    if (!value) return;
    const list = kind === 'include' ? draft.include : draft.exclude;
    if (list.includes(value)) {
      updateDraft(id, kind === 'include' ? { includeInput: '' } : { excludeInput: '' });
      return;
    }
    updateDraft(id, {
      [kind]: [...list, value],
      [kind === 'include' ? 'includeInput' : 'excludeInput']: '',
    } as Partial<Draft>);
  };

  const removeKeyword = (id: string, kind: 'include' | 'exclude', value: string) => {
    const draft = drafts[id];
    if (!draft) return;
    const list = kind === 'include' ? draft.include : draft.exclude;
    updateDraft(id, { [kind]: list.filter((k) => k !== value) } as Partial<Draft>);
  };

  const save = async (site: Site) => {
    const draft = drafts[site.id];
    if (!draft) return;
    updateDraft(site.id, { saving: true });
    try {
      await updateSite(site.id, {
        includeKeywords: draft.include,
        excludeKeywords: draft.exclude,
      });
      updateDraft(site.id, { saving: false });
      Alert.alert('저장 완료', '키워드가 적용되었습니다.');
    } catch (e) {
      updateDraft(site.id, { saving: false });
      Alert.alert('저장 실패', e instanceof Error ? e.message : '알 수 없는 오류');
    }
  };

  const isDirty = (site: Site) => {
    const draft = drafts[site.id];
    if (!draft) return false;
    const same = (a: string[], b: string[]) =>
      a.length === b.length && a.every((v, i) => v === b[i]);
    return !same(site.includeKeywords, draft.include) ||
      !same(site.excludeKeywords, draft.exclude);
  };

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
          <Ionicons name="arrow-back" size={24} color={colors.textPrimary} />
        </TouchableOpacity>
        <Text style={styles.title}>필터 설정</Text>
        <View style={styles.headerSpace} />
      </View>

      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView
          style={styles.scrollView}
          contentContainerStyle={styles.scrollContent}
          keyboardShouldPersistTaps="handled"
        >
          {isLoading && (
            <View style={styles.centered}>
              <ActivityIndicator color={colors.primary} />
            </View>
          )}

          {!isLoading && sites.length === 0 && (
            <View style={styles.centered}>
              <Text style={styles.emptyText}>등록된 사이트가 없습니다.</Text>
            </View>
          )}

          {!isLoading && sites.map((site) => {
            const draft = drafts[site.id];
            if (!draft) return null;
            const dirty = isDirty(site);
            return (
              <View key={site.id} style={styles.siteCard}>
                <View style={styles.siteHeader}>
                  <Text style={styles.siteName}>{site.name}</Text>
                  <View style={[styles.activeDot, { backgroundColor: site.isActive ? colors.success : colors.textTertiary }]} />
                </View>
                <Text style={styles.siteUrl} numberOfLines={1}>{site.url}</Text>

                <KeywordSection
                  label="포함 키워드"
                  hint="이 단어가 포함된 상품만 매칭됩니다. 비우면 모든 상품 매칭."
                  keywords={draft.include}
                  input={draft.includeInput}
                  onChangeInput={(v) => updateDraft(site.id, { includeInput: v })}
                  onAdd={() => addKeyword(site.id, 'include')}
                  onRemove={(v) => removeKeyword(site.id, 'include', v)}
                  accent={colors.primary}
                />

                <KeywordSection
                  label="제외 키워드"
                  hint="이 단어가 포함되면 매칭에서 제외됩니다."
                  keywords={draft.exclude}
                  input={draft.excludeInput}
                  onChangeInput={(v) => updateDraft(site.id, { excludeInput: v })}
                  onAdd={() => addKeyword(site.id, 'exclude')}
                  onRemove={(v) => removeKeyword(site.id, 'exclude', v)}
                  accent={colors.error}
                />

                <TouchableOpacity
                  style={[styles.saveButton, !dirty && styles.saveButtonDisabled]}
                  onPress={() => save(site)}
                  disabled={!dirty || draft.saving}
                >
                  {draft.saving ? (
                    <ActivityIndicator size="small" color={colors.white} />
                  ) : (
                    <Text style={styles.saveButtonText}>
                      {dirty ? '저장' : '변경 없음'}
                    </Text>
                  )}
                </TouchableOpacity>
              </View>
            );
          })}
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

interface KeywordSectionProps {
  label: string;
  hint: string;
  keywords: string[];
  input: string;
  onChangeInput: (v: string) => void;
  onAdd: () => void;
  onRemove: (v: string) => void;
  accent: string;
}

function KeywordSection({ label, hint, keywords, input, onChangeInput, onAdd, onRemove, accent }: KeywordSectionProps) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionLabel}>{label}</Text>
      <Text style={styles.sectionHint}>{hint}</Text>
      <View style={styles.chipContainer}>
        {keywords.length === 0 && (
          <Text style={styles.emptyChipText}>없음</Text>
        )}
        {keywords.map((k) => (
          <View key={k} style={[styles.chip, { borderColor: accent + '60' }]}>
            <Text style={[styles.chipText, { color: accent }]}>{k}</Text>
            <TouchableOpacity onPress={() => onRemove(k)} hitSlop={8}>
              <Ionicons name="close-circle" size={16} color={accent} />
            </TouchableOpacity>
          </View>
        ))}
      </View>
      <View style={styles.inputRow}>
        <TextInput
          style={styles.textInput}
          value={input}
          onChangeText={onChangeInput}
          onSubmitEditing={onAdd}
          placeholder="키워드 입력 후 추가"
          placeholderTextColor={colors.textTertiary}
          returnKeyType="done"
          autoCorrect={false}
          autoCapitalize="none"
        />
        <TouchableOpacity
          style={[styles.addButton, { backgroundColor: accent }]}
          onPress={onAdd}
        >
          <Ionicons name="add" size={20} color={colors.white} />
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: layout.screenPadding,
    paddingVertical: spacing[3],
    borderBottomWidth: 1,
    borderBottomColor: colors.divider,
  },
  backButton: {
    padding: spacing[1],
  },
  title: {
    flex: 1,
    fontSize: 18,
    fontWeight: '700',
    color: colors.textPrimary,
    marginLeft: spacing[2],
  },
  headerSpace: {
    width: 32,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    padding: layout.screenPadding,
    paddingBottom: spacing[10],
  },
  centered: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: spacing[10],
  },
  emptyText: {
    color: colors.textTertiary,
    fontSize: 14,
  },
  siteCard: {
    backgroundColor: colors.cardBackground,
    borderRadius: layout.cardBorderRadius,
    padding: spacing[4],
    marginBottom: spacing[4],
    borderWidth: 1,
    borderColor: colors.border,
  },
  siteHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
  },
  siteName: {
    flex: 1,
    fontSize: 16,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  activeDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  siteUrl: {
    fontSize: 12,
    color: colors.textTertiary,
    marginTop: spacing[1],
    marginBottom: spacing[4],
  },
  section: {
    marginTop: spacing[3],
  },
  sectionLabel: {
    fontSize: 13,
    fontWeight: '600',
    color: colors.textSecondary,
  },
  sectionHint: {
    fontSize: 11,
    color: colors.textTertiary,
    marginTop: 2,
    marginBottom: spacing[2],
  },
  chipContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing[2],
    marginBottom: spacing[2],
  },
  emptyChipText: {
    fontSize: 12,
    color: colors.textTertiary,
    fontStyle: 'italic',
  },
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[1],
    paddingHorizontal: spacing[2],
    paddingVertical: spacing[1],
    borderRadius: 999,
    borderWidth: 1,
    backgroundColor: colors.background,
  },
  chipText: {
    fontSize: 13,
    fontWeight: '500',
  },
  inputRow: {
    flexDirection: 'row',
    gap: spacing[2],
  },
  textInput: {
    flex: 1,
    backgroundColor: colors.background,
    borderRadius: spacing[2],
    borderWidth: 1,
    borderColor: colors.border,
    paddingHorizontal: spacing[3],
    paddingVertical: spacing[2],
    fontSize: 14,
    color: colors.textPrimary,
  },
  addButton: {
    width: 44,
    height: 44,
    borderRadius: spacing[2],
    alignItems: 'center',
    justifyContent: 'center',
  },
  saveButton: {
    marginTop: spacing[4],
    backgroundColor: colors.primary,
    borderRadius: layout.buttonBorderRadius,
    paddingVertical: spacing[3],
    alignItems: 'center',
  },
  saveButtonDisabled: {
    backgroundColor: colors.cardBackground,
    borderWidth: 1,
    borderColor: colors.border,
  },
  saveButtonText: {
    fontSize: 15,
    fontWeight: '700',
    color: colors.white,
  },
});
