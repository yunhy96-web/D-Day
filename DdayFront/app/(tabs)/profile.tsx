import React from 'react';
import { View, Text, StyleSheet, Alert, Switch, TouchableOpacity } from 'react-native';
import { router } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Button, Card } from '@/components';
import { useAuth, useTheme } from '@/contexts';
import { typography, spacing, layout, borderRadius } from '@/styles';
import { SafeAreaView } from 'react-native-safe-area-context';

export default function ProfileScreen() {
  const { colors, isDark, themeMode, setThemeMode } = useTheme();
  const { user, logout } = useAuth();

  const handleLogout = () => {
    Alert.alert(
      'Logout',
      'Are you sure you want to logout?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Logout',
          style: 'destructive',
          onPress: async () => {
            await logout();
            router.replace('/(auth)/login');
          },
        },
      ]
    );
  };

  const handleThemeChange = (mode: 'light' | 'dark' | 'system') => {
    setThemeMode(mode);
  };

  const themeOptions = [
    { key: 'light', label: 'Light', icon: 'sunny-outline' },
    { key: 'dark', label: 'Dark', icon: 'moon-outline' },
    { key: 'system', label: 'System', icon: 'phone-portrait-outline' },
  ] as const;

  return (
    <SafeAreaView
      style={[styles.container, { backgroundColor: colors.backgroundSecondary }]}
    >
      <View style={styles.content}>
        <Text style={[styles.title, { color: colors.textPrimary }]}>Profile</Text>

        <Card style={styles.profileCard}>
          <View style={styles.avatarContainer}>
            <View style={[styles.avatar, { backgroundColor: colors.primary }]}>
              <Text style={styles.avatarText}>
                {user?.email?.charAt(0).toUpperCase() || 'U'}
              </Text>
            </View>
          </View>
          <Text style={[styles.email, { color: colors.textPrimary }]}>
            {user?.email || 'User'}
          </Text>
        </Card>

        {/* Theme Settings */}
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>
            Theme
          </Text>
          <Card style={styles.themeCard}>
            {themeOptions.map((option) => (
              <TouchableOpacity
                key={option.key}
                style={[
                  styles.themeOption,
                  themeMode === option.key && {
                    backgroundColor: colors.primary + '15',
                  },
                ]}
                onPress={() => handleThemeChange(option.key)}
              >
                <View style={styles.themeOptionLeft}>
                  <Ionicons
                    name={option.icon as any}
                    size={22}
                    color={themeMode === option.key ? colors.primary : colors.textSecondary}
                  />
                  <Text
                    style={[
                      styles.themeOptionText,
                      {
                        color:
                          themeMode === option.key
                            ? colors.primary
                            : colors.textPrimary,
                      },
                    ]}
                  >
                    {option.label}
                  </Text>
                </View>
                {themeMode === option.key && (
                  <Ionicons name="checkmark" size={22} color={colors.primary} />
                )}
              </TouchableOpacity>
            ))}
          </Card>
        </View>

        {/* Account */}
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>
            Account
          </Text>
          <Button
            title="Logout"
            variant="outline"
            onPress={handleLogout}
            fullWidth
          />
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    flex: 1,
    padding: layout.screenPadding,
  },
  title: {
    ...typography.h2,
    marginBottom: spacing[6],
  },
  profileCard: {
    alignItems: 'center',
    padding: spacing[6],
    marginBottom: spacing[6],
  },
  avatarContainer: {
    marginBottom: spacing[4],
  },
  avatar: {
    width: 80,
    height: 80,
    borderRadius: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarText: {
    ...typography.h1,
    color: '#FFFFFF',
  },
  email: {
    ...typography.body,
  },
  section: {
    marginBottom: spacing[6],
  },
  sectionTitle: {
    ...typography.label,
    marginBottom: spacing[3],
    textTransform: 'uppercase',
  },
  themeCard: {
    padding: 0,
    overflow: 'hidden',
  },
  themeOption: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: spacing[4],
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(0,0,0,0.05)',
  },
  themeOptionLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[3],
  },
  themeOptionText: {
    ...typography.body,
  },
});
