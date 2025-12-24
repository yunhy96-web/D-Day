import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView } from 'react-native';
import { router } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useAuth, useTheme } from '@/contexts';
import { confirm } from '@/utils/alert';
import { spacing, layout, borderRadius, shadows } from '@/styles';
import { SafeAreaView } from 'react-native-safe-area-context';

export default function ProfileScreen() {
  const { colors, isDark, themeMode, setThemeMode } = useTheme();
  const { user, logout } = useAuth();

  const handleLogout = async () => {
    const confirmed = await confirm({
      title: 'Logout',
      message: 'Are you sure you want to logout?',
      confirmText: 'Logout',
    });

    if (confirmed) {
      await logout();
      router.replace('/(auth)/login');
    }
  };

  const handleThemeChange = (mode: 'light' | 'dark' | 'system') => {
    setThemeMode(mode);
  };

  const themeOptions = [
    { key: 'light', label: 'Light', icon: 'sunny-outline' },
    { key: 'dark', label: 'Dark', icon: 'moon-outline' },
    { key: 'system', label: 'System', icon: 'phone-portrait-outline' },
  ] as const;

  const getInitial = () => {
    if (user?.nickname) return user.nickname.charAt(0).toUpperCase();
    if (user?.email) return user.email.charAt(0).toUpperCase();
    return 'U';
  };

  return (
    <SafeAreaView
      style={[styles.container, { backgroundColor: colors.background }]}
    >
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity
          onPress={() => router.back()}
          style={[styles.backButton, { backgroundColor: isDark ? colors.gray200 : colors.gray100 }]}
        >
          <Ionicons name="arrow-back" size={20} color={colors.textPrimary} />
        </TouchableOpacity>
        <Text style={[styles.headerTitle, { color: colors.textPrimary }]}>Profile</Text>
        <View style={styles.headerSpacer} />
      </View>

      <ScrollView style={styles.scrollView} showsVerticalScrollIndicator={false}>
        <View style={styles.content}>
          {/* Profile Card */}
          <View style={[styles.profileCard, { backgroundColor: isDark ? colors.gray100 : colors.background }, !isDark && shadows.md]}>
            <View style={[styles.avatar, { backgroundColor: colors.primary }]}>
              <Text style={styles.avatarText}>{getInitial()}</Text>
            </View>
            <View style={styles.profileInfo}>
              <Text style={[styles.nickname, { color: colors.textPrimary }]}>
                {user?.nickname || 'User'}
              </Text>
              <Text style={[styles.email, { color: colors.textSecondary }]}>
                {user?.email || ''}
              </Text>
            </View>
          </View>

          {/* Theme Settings */}
          <View style={styles.section}>
            <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>
              Appearance
            </Text>
            <View style={[styles.themeCard, { backgroundColor: isDark ? colors.gray100 : colors.background }, !isDark && shadows.sm]}>
              {themeOptions.map((option, index) => (
                <TouchableOpacity
                  key={option.key}
                  style={[
                    styles.themeOption,
                    themeMode === option.key && {
                      backgroundColor: colors.primary + '12',
                    },
                    index < themeOptions.length - 1 && {
                      borderBottomWidth: 1,
                      borderBottomColor: isDark ? colors.gray200 : colors.gray100,
                    },
                  ]}
                  onPress={() => handleThemeChange(option.key)}
                >
                  <View style={styles.themeOptionLeft}>
                    <View style={[
                      styles.iconContainer,
                      { backgroundColor: themeMode === option.key ? colors.primary + '20' : (isDark ? colors.gray200 : colors.gray100) }
                    ]}>
                      <Ionicons
                        name={option.icon as any}
                        size={18}
                        color={themeMode === option.key ? colors.primary : colors.textSecondary}
                      />
                    </View>
                    <Text
                      style={[
                        styles.themeOptionText,
                        {
                          color: themeMode === option.key ? colors.primary : colors.textPrimary,
                          fontWeight: themeMode === option.key ? '600' : '400',
                        },
                      ]}
                    >
                      {option.label}
                    </Text>
                  </View>
                  {themeMode === option.key && (
                    <Ionicons name="checkmark-circle" size={22} color={colors.primary} />
                  )}
                </TouchableOpacity>
              ))}
            </View>
          </View>

          {/* Account */}
          <View style={styles.section}>
            <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>
              Account
            </Text>
            <TouchableOpacity
              style={[styles.logoutButton, { backgroundColor: colors.error + '12' }]}
              onPress={handleLogout}
            >
              <Ionicons name="log-out-outline" size={20} color={colors.error} />
              <Text style={[styles.logoutText, { color: colors.error }]}>Logout</Text>
            </TouchableOpacity>
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: layout.screenPadding,
    paddingVertical: spacing[3],
  },
  backButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: '700',
  },
  headerSpacer: {
    width: 40,
  },
  scrollView: {
    flex: 1,
  },
  content: {
    padding: layout.screenPadding,
  },
  profileCard: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: spacing[5],
    borderRadius: borderRadius.xl,
    marginBottom: spacing[6],
    gap: spacing[4],
  },
  avatar: {
    width: 72,
    height: 72,
    borderRadius: 36,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarText: {
    fontSize: 28,
    fontWeight: '700',
    color: '#FFFFFF',
  },
  profileInfo: {
    flex: 1,
    gap: 4,
  },
  nickname: {
    fontSize: 22,
    fontWeight: '700',
  },
  email: {
    fontSize: 14,
  },
  section: {
    marginBottom: spacing[6],
  },
  sectionTitle: {
    fontSize: 13,
    fontWeight: '600',
    marginBottom: spacing[3],
    marginLeft: spacing[1],
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  themeCard: {
    borderRadius: borderRadius.xl,
    overflow: 'hidden',
  },
  themeOption: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: spacing[4],
  },
  themeOptionLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[3],
  },
  iconContainer: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
  },
  themeOptionText: {
    fontSize: 15,
  },
  logoutButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    padding: spacing[4],
    borderRadius: borderRadius.xl,
    gap: spacing[2],
  },
  logoutText: {
    fontSize: 15,
    fontWeight: '600',
  },
});
