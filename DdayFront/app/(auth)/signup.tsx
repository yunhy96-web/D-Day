import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  TouchableOpacity,
  Modal,
  FlatList,
} from 'react-native';
import { Link, router } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Button, Input } from '@/components';
import { useAuth, useTheme } from '@/contexts';
import { showAlert } from '@/utils/alert';
import { spacing, layout, borderRadius } from '@/styles';

const TIMEZONE_OPTIONS = [
  { value: 'Asia/Seoul', label: '한국 (Korea)', flag: '🇰🇷' },
  { value: 'Asia/Bangkok', label: 'ประเทศไทย (Thailand)', flag: '🇹🇭' },
];

export default function SignUpScreen() {
  const { colors, isDark } = useTheme();
  const { signUp } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [nickname, setNickname] = useState('');
  const [timezone, setTimezone] = useState('Asia/Seoul');
  const [showTimezoneModal, setShowTimezoneModal] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState<{
    email?: string;
    password?: string;
    confirmPassword?: string;
    nickname?: string;
  }>({});

  const selectedTimezone = TIMEZONE_OPTIONS.find(t => t.value === timezone);

  const validate = () => {
    const newErrors: typeof errors = {};

    if (!email) {
      newErrors.email = 'Please enter your email';
    } else if (!/\S+@\S+\.\S+/.test(email)) {
      newErrors.email = 'Please enter a valid email';
    }

    if (!password) {
      newErrors.password = 'Please enter a password';
    } else if (password.length < 6) {
      newErrors.password = 'Password must be at least 6 characters';
    }

    if (!confirmPassword) {
      newErrors.confirmPassword = 'Please confirm your password';
    } else if (password !== confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }

    if (!nickname) {
      newErrors.nickname = 'Please enter a nickname';
    } else if (nickname.length < 2) {
      newErrors.nickname = 'Nickname must be at least 2 characters';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSignUp = async () => {
    if (!validate()) return;

    setIsLoading(true);
    try {
      await signUp({ email, password, nickname, timezone });
      router.replace('/(tabs)');
    } catch (error: any) {
      showAlert(
        'Sign Up Failed',
        error.response?.data?.message || 'An error occurred during sign up'
      );
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={[styles.container, { backgroundColor: colors.background }]}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <ScrollView
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        {/* Header */}
        <View style={styles.header}>
          <View style={[styles.logoContainer, { backgroundColor: colors.primary + '15' }]}>
            <Ionicons name="person-add" size={36} color={colors.primary} />
          </View>
          <Text style={[styles.title, { color: colors.textPrimary }]}>Create Account</Text>
          <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
            Join the Community today!
          </Text>
        </View>

        <View style={styles.form}>
          <Input
            label="Email"
            placeholder="example@email.com"
            value={email}
            onChangeText={setEmail}
            keyboardType="email-address"
            autoCapitalize="none"
            autoCorrect={false}
            leftIcon="mail-outline"
            error={errors.email}
          />

          <Input
            label="Nickname"
            placeholder="How should we call you?"
            value={nickname}
            onChangeText={setNickname}
            autoCapitalize="none"
            leftIcon="person-outline"
            error={errors.nickname}
          />

          <Input
            label="Password"
            placeholder="At least 6 characters"
            value={password}
            onChangeText={setPassword}
            secureTextEntry
            leftIcon="lock-closed-outline"
            error={errors.password}
          />

          <Input
            label="Confirm Password"
            placeholder="Re-enter your password"
            value={confirmPassword}
            onChangeText={setConfirmPassword}
            secureTextEntry
            leftIcon="lock-closed-outline"
            error={errors.confirmPassword}
          />

          <View style={styles.timezoneContainer}>
            <Text style={[styles.label, { color: colors.textPrimary }]}>Country</Text>
            <TouchableOpacity
              style={[styles.timezoneButton, { borderColor: colors.border, backgroundColor: colors.background }]}
              onPress={() => setShowTimezoneModal(true)}
            >
              <View style={styles.timezoneLeft}>
                <Text style={styles.timezoneFlag}>{selectedTimezone?.flag}</Text>
                <Text style={[styles.timezoneText, { color: colors.textPrimary }]}>
                  {selectedTimezone?.label}
                </Text>
              </View>
              <Ionicons name="chevron-down" size={20} color={colors.textSecondary} />
            </TouchableOpacity>
          </View>

          <Button
            title="Create Account"
            onPress={handleSignUp}
            loading={isLoading}
            fullWidth
            style={styles.signupButton}
          />
        </View>

        {/* Timezone Modal */}
        <Modal
          visible={showTimezoneModal}
          transparent
          animationType="slide"
          onRequestClose={() => setShowTimezoneModal(false)}
        >
          <TouchableOpacity
            style={styles.modalOverlay}
            activeOpacity={1}
            onPress={() => setShowTimezoneModal(false)}
          >
            <View style={[styles.modalContent, { backgroundColor: isDark ? colors.gray100 : colors.background }]}>
              <View style={[styles.modalHandle, { backgroundColor: colors.gray300 }]} />
              <View style={styles.modalHeader}>
                <Text style={[styles.modalTitle, { color: colors.textPrimary }]}>
                  Select Your Country
                </Text>
              </View>
              <FlatList
                data={TIMEZONE_OPTIONS}
                keyExtractor={(item) => item.value}
                renderItem={({ item }) => (
                  <TouchableOpacity
                    style={[
                      styles.timezoneOption,
                      timezone === item.value && { backgroundColor: colors.primary + '12' },
                    ]}
                    onPress={() => {
                      setTimezone(item.value);
                      setShowTimezoneModal(false);
                    }}
                  >
                    <Text style={styles.timezoneFlag}>{item.flag}</Text>
                    <Text style={[styles.timezoneOptionText, { color: colors.textPrimary }]}>
                      {item.label}
                    </Text>
                    {timezone === item.value && (
                      <Ionicons name="checkmark-circle" size={22} color={colors.primary} />
                    )}
                  </TouchableOpacity>
                )}
              />
            </View>
          </TouchableOpacity>
        </Modal>

        <View style={styles.footer}>
          <Text style={[styles.footerText, { color: colors.textSecondary }]}>
            Already have an account?
          </Text>
          <Link href="/(auth)/login" asChild>
            <TouchableOpacity>
              <Text style={[styles.loginLink, { color: colors.primary }]}>Sign In</Text>
            </TouchableOpacity>
          </Link>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
    padding: layout.screenPadding,
    paddingTop: spacing[10],
    paddingBottom: spacing[8],
  },
  header: {
    alignItems: 'center',
    marginBottom: spacing[8],
  },
  logoContainer: {
    width: 72,
    height: 72,
    borderRadius: 36,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing[4],
  },
  title: {
    fontSize: 26,
    fontWeight: '800',
    marginBottom: spacing[2],
  },
  subtitle: {
    fontSize: 15,
    textAlign: 'center',
  },
  form: {
    marginBottom: spacing[6],
  },
  signupButton: {
    marginTop: spacing[4],
    borderRadius: borderRadius.xl,
  },
  timezoneContainer: {
    marginBottom: spacing[4],
  },
  label: {
    fontSize: 14,
    fontWeight: '500',
    marginBottom: spacing[2],
  },
  timezoneButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderWidth: 1,
    borderRadius: borderRadius.xl,
    padding: spacing[4],
  },
  timezoneLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[3],
  },
  timezoneFlag: {
    fontSize: 24,
  },
  timezoneText: {
    fontSize: 15,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'flex-end',
  },
  modalContent: {
    borderTopLeftRadius: borderRadius.xl,
    borderTopRightRadius: borderRadius.xl,
    paddingBottom: spacing[8],
  },
  modalHandle: {
    width: 36,
    height: 4,
    borderRadius: 2,
    alignSelf: 'center',
    marginTop: spacing[3],
    marginBottom: spacing[2],
  },
  modalHeader: {
    alignItems: 'center',
    padding: spacing[4],
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '700',
  },
  timezoneOption: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: spacing[4],
    marginHorizontal: spacing[3],
    borderRadius: borderRadius.lg,
    gap: spacing[3],
  },
  timezoneOptionText: {
    fontSize: 15,
    flex: 1,
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    gap: spacing[1],
  },
  footerText: {
    fontSize: 14,
  },
  loginLink: {
    fontSize: 14,
    fontWeight: '700',
  },
});
