import { Alert, Platform } from 'react-native';

interface ConfirmOptions {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
}

/**
 * 크로스 플랫폼 확인 다이얼로그
 * 웹에서는 window.confirm, 모바일에서는 Alert.alert 사용
 */
export function confirm(options: ConfirmOptions): Promise<boolean> {
  const { title, message, confirmText = 'OK', cancelText = 'Cancel' } = options;

  return new Promise((resolve) => {
    if (Platform.OS === 'web') {
      const result = window.confirm(`${title}\n\n${message}`);
      resolve(result);
    } else {
      Alert.alert(title, message, [
        { text: cancelText, style: 'cancel', onPress: () => resolve(false) },
        { text: confirmText, style: 'destructive', onPress: () => resolve(true) },
      ]);
    }
  });
}

/**
 * 크로스 플랫폼 알림 다이얼로그
 */
export function showAlert(title: string, message?: string): void {
  if (Platform.OS === 'web') {
    window.alert(message ? `${title}\n\n${message}` : title);
  } else {
    Alert.alert(title, message);
  }
}
