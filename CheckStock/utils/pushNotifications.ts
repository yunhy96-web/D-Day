import * as Notifications from 'expo-notifications';
import * as Device from 'expo-device';

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true,
    shouldShowList: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
  }),
});

export async function fireLocalAlert(
  title: string,
  body: string,
  data: Record<string, unknown> = {}
): Promise<void> {
  try {
    await Notifications.scheduleNotificationAsync({
      content: {
        title,
        body,
        sound: 'default',
        data,
      },
      trigger: null,
    });
  } catch (err) {
    console.warn('[push] 로컬 알림 실패:', err);
  }
}

export async function ensureNotificationPermission(): Promise<boolean> {
  if (!Device.isDevice) {
    console.log('[push] 실기기가 아니라 알림 권한 스킵');
    return false;
  }
  const { status: existingStatus } = await Notifications.getPermissionsAsync();
  if (existingStatus === 'granted') return true;
  const { status } = await Notifications.requestPermissionsAsync();
  return status === 'granted';
}
