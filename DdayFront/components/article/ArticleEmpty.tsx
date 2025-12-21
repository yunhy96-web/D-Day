import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useTheme } from '@/contexts';
import { typography, spacing } from '@/styles';

interface ArticleEmptyProps {
  onAdd?: () => void;
}

export function ArticleEmpty({ onAdd }: ArticleEmptyProps) {
  const { colors } = useTheme();

  return (
    <View style={styles.container}>
      <Ionicons name="document-text-outline" size={64} color={colors.gray300} />
      <Text style={[styles.title, { color: colors.textSecondary }]}>
        No articles yet
      </Text>
      <Text style={[styles.description, { color: colors.textTertiary }]}>
        Create your first article to get started
      </Text>
      {onAdd && (
        <TouchableOpacity
          style={[styles.addButton, { backgroundColor: colors.primary }]}
          onPress={onAdd}
        >
          <Ionicons name="add" size={20} color="#FFFFFF" />
          <Text style={styles.addButtonText}>Create Article</Text>
        </TouchableOpacity>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: spacing[8],
  },
  title: {
    ...typography.h3,
    marginTop: spacing[4],
    marginBottom: spacing[2],
  },
  description: {
    ...typography.body,
    textAlign: 'center',
    marginBottom: spacing[6],
  },
  addButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: spacing[3],
    paddingHorizontal: spacing[5],
    borderRadius: 8,
  },
  addButtonText: {
    color: '#FFFFFF',
    ...typography.button,
    marginLeft: spacing[2],
  },
});
