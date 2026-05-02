import { useEffect, useState } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, ActivityIndicator, ScrollView, Alert, Linking, Switch } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import * as Clipboard from 'expo-clipboard';
import WebView from 'react-native-webview';
import { activateKeepAwakeAsync, deactivateKeepAwake } from 'expo-keep-awake';
import Sidebar from '@/components/Sidebar';
import { colors, layout, shadows, spacing } from '@/styles';
import { useStockChecker, type CrawlMode } from '@/hooks/useStockChecker';
import { useSites } from '@/contexts';

const KEEP_AWAKE_TAG = 'site-dashboard-app-mode';

function formatTime(date: Date | null): string {
  if (!date) return '-';
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

export default function SiteDashboard() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { sites, updateSite } = useSites();
  const router = useRouter();
  const [sidebarVisible, setSidebarVisible] = useState(false);
  const [mode, setMode] = useState<CrawlMode>('backend');

  const site = sites.find((s) => s.id === id) ?? null;

  const {
    totalCount,
    matchedCount,
    products,
    lastChecked,
    isLoading,
    error,
    refresh,
    countdown,
    refreshInterval,
    webViewRef,
    injectedJs,
    handleMessage,
    handleLoadStart,
    handleError,
  } = useStockChecker(site, mode);

  const [copiedIndex, setCopiedIndex] = useState<number | null>(null);

  // app 모드일 때 화면 자동 잠금 방지
  useEffect(() => {
    if (mode === 'app') {
      activateKeepAwakeAsync(KEEP_AWAKE_TAG).catch(() => {});
      return () => {
        try {
          deactivateKeepAwake(KEEP_AWAKE_TAG);
        } catch {}
      };
    }
    return;
  }, [mode]);

  const handleProductPress = async (url: string) => {
    try {
      const supported = await Linking.canOpenURL(url);
      if (supported) {
        await Linking.openURL(url);
      } else {
        await Clipboard.setStringAsync(url);
        Alert.alert('클립보드 복사 완료', '링크가 복사되었습니다. 사파리에서 붙여넣기 해주세요.');
      }
    } catch {
      await Clipboard.setStringAsync(url);
      Alert.alert('클립보드 복사 완료', '링크가 복사되었습니다. 사파리에서 붙여넣기 해주세요.');
    }
  };

  const handleCopy = async (url: string, index: number) => {
    await Clipboard.setStringAsync(url);
    setCopiedIndex(index);
    setTimeout(() => setCopiedIndex(null), 2000);
  };

  const handleIntervalChange = async (seconds: number) => {
    if (!site) return;
    await updateSite(site.id, { refreshInterval: seconds });
  };

  if (!site) {
    return (
      <SafeAreaView style={styles.container}>
        <Text style={styles.errorText}>사이트를 찾을 수 없습니다</Text>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      {/* app 모드 전용 숨겨진 WebView */}
      {mode === 'app' && (
        <View style={styles.hiddenWebView}>
          <WebView
            ref={webViewRef}
            source={{ uri: site.url }}
            injectedJavaScript={injectedJs}
            onMessage={handleMessage}
            onLoadStart={handleLoadStart}
            onError={handleError}
            javaScriptEnabled
            domStorageEnabled
            sharedCookiesEnabled
          />
        </View>
      )}

      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {/* 헤더 */}
        <View style={styles.header}>
          <TouchableOpacity
            onPress={() => setSidebarVisible(true)}
            style={styles.menuButton}
          >
            <Ionicons name="menu-outline" size={26} color={colors.textPrimary} />
          </TouchableOpacity>
          <View style={styles.headerCenter}>
            <Text style={styles.title} numberOfLines={1}>{site.name}</Text>
          </View>
          <TouchableOpacity
            onPress={() => router.push(`/history/${site.id}` as any)}
            style={styles.historyButton}
          >
            <Ionicons name="time-outline" size={24} color={colors.textSecondary} />
          </TouchableOpacity>
        </View>

        {/* 상품 수 카드 */}
        <View style={styles.card}>
          {error ? (
            <View style={styles.errorContainer}>
              <Ionicons name="warning-outline" size={48} color={colors.error} />
              <Text style={styles.errorText}>{error}</Text>
              <TouchableOpacity
                style={styles.retryButton}
                onPress={refresh}
                disabled={isLoading}
              >
                <Text style={styles.retryButtonText}>다시 시도</Text>
              </TouchableOpacity>
            </View>
          ) : (
            <>
              <Text style={styles.cardLabel}>매칭 상품 수</Text>
              <View style={styles.countContainer}>
                {isLoading && matchedCount === null ? (
                  <ActivityIndicator size="large" color={colors.primary} />
                ) : (
                  <Text style={styles.countText}>{matchedCount === null ? '-' : matchedCount}</Text>
                )}
              </View>
              <Text style={styles.countUnit}>
                {totalCount !== null ? `전체 ${totalCount}개 중` : '개'}
              </Text>
            </>
          )}
        </View>

        {/* 정보 영역 */}
        <View style={styles.infoSection}>
          <View style={styles.infoRow}>
            <Ionicons name="time-outline" size={18} color={colors.textTertiary} />
            <Text style={styles.infoText}>
              마지막 확인: {formatTime(lastChecked)}
            </Text>
          </View>
          <View style={styles.infoRow}>
            <Ionicons name="refresh-circle-outline" size={18} color={colors.textTertiary} />
            <Text style={styles.infoText}>
              다음 새로고침까지 <Text style={styles.countdownText}>{countdown}초</Text>
            </Text>
          </View>
        </View>

        {/* 크롤링 모드 토글 */}
        <View style={styles.modeSection}>
          <View style={styles.modeRow}>
            <View style={styles.modeLabelGroup}>
              <Text style={styles.modeLabel}>
                {mode === 'app' ? '앱 크롤링 (수면 모드)' : '백엔드 크롤링'}
              </Text>
              <Text style={styles.modeHint}>
                {mode === 'app'
                  ? '화면 켠 상태 유지 · 캡차 우회 · 알림 수신용'
                  : '앱 꺼져도 동작 · Python 크롤러 사용'}
              </Text>
            </View>
            <Switch
              value={mode === 'app'}
              onValueChange={(v) => setMode(v ? 'app' : 'backend')}
              trackColor={{ false: colors.border, true: colors.primary }}
              thumbColor={colors.white}
            />
          </View>
        </View>

        {/* 새로고침 주기 설정 */}
        <View style={styles.intervalSection}>
          <Text style={styles.intervalLabel}>새로고침 주기</Text>
          <View style={styles.intervalButtons}>
            {[15, 30, 60, 120, 300].map((sec) => (
              <TouchableOpacity
                key={sec}
                style={[
                  styles.intervalButton,
                  refreshInterval === sec && styles.intervalButtonActive,
                ]}
                onPress={() => handleIntervalChange(sec)}
                activeOpacity={0.7}
              >
                <Text
                  style={[
                    styles.intervalButtonText,
                    refreshInterval === sec && styles.intervalButtonTextActive,
                  ]}
                >
                  {sec >= 60 ? `${sec / 60}분` : `${sec}초`}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* 상품 목록 */}
        {products.length > 0 && (
          <View style={styles.productList}>
            <Text style={styles.productListTitle}>상품 목록</Text>
            {products.map((product, index) => (
              <View key={index} style={styles.productItem}>
                <Text style={styles.productIndex}>{index + 1}</Text>
                <TouchableOpacity
                  style={styles.productNameButton}
                  onPress={() => handleProductPress(product.url)}
                  activeOpacity={0.6}
                >
                  <Text style={styles.productName}>{product.name}</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={styles.copyButton}
                  onPress={() => handleCopy(product.url, index)}
                  activeOpacity={0.6}
                >
                  <Ionicons
                    name={copiedIndex === index ? 'checkmark-outline' : 'copy-outline'}
                    size={18}
                    color={copiedIndex === index ? colors.success : colors.textTertiary}
                  />
                </TouchableOpacity>
              </View>
            ))}
          </View>
        )}
      </ScrollView>

      {/* 새로고침 버튼 */}
      <TouchableOpacity
        style={[styles.refreshButton, isLoading && styles.refreshButtonDisabled]}
        onPress={refresh}
        disabled={isLoading}
        activeOpacity={0.7}
      >
        {isLoading ? (
          <ActivityIndicator size="small" color={colors.white} />
        ) : (
          <Ionicons name="refresh-outline" size={24} color={colors.white} />
        )}
        <Text style={styles.refreshButtonText}>
          {isLoading ? '확인 중...' : '지금 확인하기'}
        </Text>
      </TouchableOpacity>
      <Sidebar
        visible={sidebarVisible}
        onClose={() => setSidebarVisible(false)}
        activeSiteId={id}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
    paddingHorizontal: layout.screenPadding,
  },
  hiddenWebView: {
    height: 0,
    overflow: 'hidden',
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingBottom: spacing[4],
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: spacing[4],
    marginBottom: spacing[5],
  },
  menuButton: {
    padding: spacing[1],
  },
  headerCenter: {
    flex: 1,
    alignItems: 'center',
    paddingHorizontal: spacing[3],
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  historyButton: {
    padding: spacing[1],
  },
  card: {
    backgroundColor: colors.cardBackground,
    borderRadius: layout.cardBorderRadius,
    padding: spacing[8],
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 200,
    borderWidth: 1,
    borderColor: colors.border,
    ...shadows.lg,
  },
  cardLabel: {
    fontSize: 16,
    color: colors.textSecondary,
    marginBottom: spacing[3],
  },
  countContainer: {
    justifyContent: 'center',
    alignItems: 'center',
    minHeight: 80,
  },
  countText: {
    fontSize: 72,
    fontWeight: '800',
    color: colors.primary,
  },
  countUnit: {
    fontSize: 18,
    color: colors.textTertiary,
    marginTop: spacing[1],
  },
  errorContainer: {
    alignItems: 'center',
    gap: spacing[3],
  },
  errorText: {
    fontSize: 14,
    color: colors.error,
    textAlign: 'center',
  },
  retryButton: {
    paddingHorizontal: spacing[5],
    paddingVertical: spacing[2],
    backgroundColor: colors.cardBackground,
    borderRadius: layout.buttonBorderRadius,
    borderWidth: 1,
    borderColor: colors.error,
  },
  retryButtonText: {
    color: colors.error,
    fontSize: 14,
    fontWeight: '600',
  },
  infoSection: {
    marginTop: spacing[6],
    gap: spacing[2],
  },
  infoRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
  },
  infoText: {
    fontSize: 13,
    color: colors.textTertiary,
  },
  countdownText: {
    color: colors.primary,
    fontWeight: '700',
  },
  modeSection: {
    marginTop: spacing[5],
    backgroundColor: colors.cardBackground,
    borderRadius: layout.cardBorderRadius,
    padding: spacing[4],
    borderWidth: 1,
    borderColor: colors.border,
  },
  modeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[3],
  },
  modeLabelGroup: {
    flex: 1,
  },
  modeLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  modeHint: {
    fontSize: 11,
    color: colors.textTertiary,
    marginTop: 2,
  },
  intervalSection: {
    marginTop: spacing[5],
  },
  intervalLabel: {
    fontSize: 13,
    color: colors.textTertiary,
    marginBottom: spacing[2],
  },
  intervalButtons: {
    flexDirection: 'row',
    gap: spacing[2],
  },
  intervalButton: {
    flex: 1,
    paddingVertical: spacing[2],
    borderRadius: spacing[2],
    borderWidth: 1,
    borderColor: colors.border,
    alignItems: 'center',
  },
  intervalButtonActive: {
    borderColor: colors.primary,
    backgroundColor: colors.primary + '20',
  },
  intervalButtonText: {
    fontSize: 13,
    color: colors.textTertiary,
    fontWeight: '500',
  },
  intervalButtonTextActive: {
    color: colors.primary,
    fontWeight: '700',
  },
  productList: {
    marginTop: spacing[6],
  },
  productListTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: colors.textPrimary,
    marginBottom: spacing[3],
  },
  productItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.cardBackground,
    borderRadius: spacing[2],
    paddingVertical: spacing[3],
    paddingHorizontal: spacing[4],
    marginBottom: spacing[2],
    borderWidth: 1,
    borderColor: colors.border,
  },
  productIndex: {
    fontSize: 13,
    fontWeight: '700',
    color: colors.primary,
    width: 28,
  },
  productNameButton: {
    flex: 1,
  },
  productName: {
    fontSize: 14,
    color: colors.iconBlue,
    textDecorationLine: 'underline',
  },
  copyButton: {
    padding: spacing[2],
    marginLeft: spacing[2],
  },
  refreshButton: {
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
  refreshButtonDisabled: {
    backgroundColor: colors.primaryDark,
    opacity: 0.7,
  },
  refreshButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: colors.white,
  },
});
