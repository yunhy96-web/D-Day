import React, { useState, useMemo } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, Dimensions } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { BlurView } from 'expo-blur';
import { Ionicons } from '@expo/vector-icons';
import Svg, { Circle, Path, Line, Text as SvgText } from 'react-native-svg';
import { colors, spacing, layout } from '@/styles';
import { useCar, useRecord, MaintenanceRecord } from '@/contexts';

const { width: SCREEN_WIDTH } = Dimensions.get('window');

// 기간 옵션
type PeriodType = 'all' | '6months' | '1year' | 'thisMonth';
const periodOptions: { value: PeriodType; label: string }[] = [
  { value: 'all', label: '전체' },
  { value: '1year', label: '최근 1년' },
  { value: '6months', label: '최근 6개월' },
  { value: 'thisMonth', label: '이번 달' },
];

// 파이 차트 카테고리 옵션
type ChartCategoryType = 'all' | '정비' | '주유' | '기타';
const chartCategoryOptions: { value: ChartCategoryType; label: string }[] = [
  { value: 'all', label: '전체' },
  { value: '정비', label: '정비' },
  { value: '주유', label: '주유' },
  { value: '기타', label: '기타' },
];

// 차트 색상
const CHART_COLORS = [
  colors.primary,    // 골드
  '#1E3A5F',         // 네이비
  colors.success,    // 그린
  colors.iconBlue,   // 블루
  '#E57373',         // 레드
  '#9575CD',         // 퍼플
  '#4DB6AC',         // 틸
  '#FFB74D',         // 오렌지
];

export default function StatsScreen() {
  const router = useRouter();
  const { selectedCar, cars, selectCar } = useCar();
  const { records, getRecordsByCarId } = useRecord();

  const [selectedPeriod, setSelectedPeriod] = useState<PeriodType>('all');
  const [showPeriodPicker, setShowPeriodPicker] = useState(false);
  const [showCarPicker, setShowCarPicker] = useState(false);
  const [selectedChartCategory, setSelectedChartCategory] = useState<ChartCategoryType>('all');
  const [selectedMonthlyCategory, setSelectedMonthlyCategory] = useState<ChartCategoryType>('all');

  // 선택된 차량의 기록
  const carRecords = useMemo(() => {
    if (!selectedCar) return [];
    return getRecordsByCarId(selectedCar.id);
  }, [selectedCar, getRecordsByCarId]);

  // 기간 필터링
  const filteredRecords = useMemo(() => {
    const now = new Date();
    let startDate: Date;

    switch (selectedPeriod) {
      case '6months':
        startDate = new Date(now.getFullYear(), now.getMonth() - 6, 1);
        break;
      case '1year':
        startDate = new Date(now.getFullYear() - 1, now.getMonth(), 1);
        break;
      case 'thisMonth':
        startDate = new Date(now.getFullYear(), now.getMonth(), 1);
        break;
      default:
        return carRecords;
    }

    return carRecords.filter(record => new Date(record.date) >= startDate);
  }, [carRecords, selectedPeriod]);

  // 통계 계산
  const stats = useMemo(() => {
    const totalCount = filteredRecords.length;
    const totalCost = filteredRecords.reduce((sum, r) => sum + (r.cost || 0), 0);
    const avgCost = totalCount > 0 ? Math.round(totalCost / totalCount) : 0;

    // 이번 달 정비 횟수
    const now = new Date();
    const thisMonthStart = new Date(now.getFullYear(), now.getMonth(), 1);
    const thisMonthCount = filteredRecords.filter(
      r => new Date(r.date) >= thisMonthStart
    ).length;

    return { totalCount, totalCost, avgCost, thisMonthCount };
  }, [filteredRecords]);

  // 유형별 비용 (파이 차트 데이터)
  const typeData = useMemo(() => {
    // 카테고리별 필터링
    const categoryFilteredRecords = selectedChartCategory === 'all'
      ? filteredRecords
      : filteredRecords.filter(r => r.category === selectedChartCategory);

    const typeMap: { [key: string]: number } = {};
    categoryFilteredRecords.forEach(record => {
      if (record.cost) {
        typeMap[record.type] = (typeMap[record.type] || 0) + record.cost;
      }
    });

    return Object.entries(typeMap)
      .map(([type, cost], index) => ({
        type,
        cost,
        color: CHART_COLORS[index % CHART_COLORS.length],
      }))
      .sort((a, b) => b.cost - a.cost);
  }, [filteredRecords, selectedChartCategory]);

  // 선택된 카테고리의 총 비용
  const categoryTotalCost = useMemo(() => {
    return typeData.reduce((sum, item) => sum + item.cost, 0);
  }, [typeData]);

  // 월별 비용 추이 (라인 차트 데이터)
  const monthlyData = useMemo(() => {
    const now = new Date();
    const months: { month: string; cost: number }[] = [];

    // 카테고리별 필터링
    const categoryFilteredRecords = selectedMonthlyCategory === 'all'
      ? carRecords
      : carRecords.filter(r => r.category === selectedMonthlyCategory);

    for (let i = 5; i >= 0; i--) {
      const date = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const monthLabel = `${date.getMonth() + 1}월`;
      const monthStart = new Date(date.getFullYear(), date.getMonth(), 1);
      const monthEnd = new Date(date.getFullYear(), date.getMonth() + 1, 0);

      const monthCost = categoryFilteredRecords
        .filter(r => {
          const recordDate = new Date(r.date);
          return recordDate >= monthStart && recordDate <= monthEnd;
        })
        .reduce((sum, r) => sum + (r.cost || 0), 0);

      months.push({ month: monthLabel, cost: monthCost });
    }

    return months;
  }, [carRecords, selectedMonthlyCategory]);

  // 월별 누적 주행거리 추이 (라인 차트 데이터)
  const monthlyMileageData = useMemo(() => {
    const now = new Date();
    const months: { month: string; mileage: number }[] = [];

    for (let i = 5; i >= 0; i--) {
      const date = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const monthLabel = `${date.getMonth() + 1}월`;
      const monthEnd = new Date(date.getFullYear(), date.getMonth() + 1, 0);

      // 해당 월 말까지의 기록 중 가장 높은 주행거리
      const maxMileage = carRecords
        .filter(r => {
          const recordDate = new Date(r.date);
          return recordDate <= monthEnd && r.mileage;
        })
        .reduce((max, r) => Math.max(max, r.mileage || 0), 0);

      months.push({ month: monthLabel, mileage: maxMileage });
    }

    return months;
  }, [carRecords]);

  // 차량별 통계
  const carStats = useMemo(() => {
    return cars.map(car => {
      const carRecords = getRecordsByCarId(car.id);
      const totalCount = carRecords.length;
      const totalCost = carRecords.reduce((sum, r) => sum + (r.cost || 0), 0);
      const avgCost = totalCount > 0 ? Math.round(totalCost / totalCount) : 0;

      return { car, totalCount, totalCost, avgCost };
    });
  }, [cars, getRecordsByCarId]);

  const currentPeriodLabel = periodOptions.find(p => p.value === selectedPeriod)?.label || '전체';

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.push('/')} style={styles.backButton}>
          <Ionicons name="chevron-back" size={24} color={colors.textPrimary} />
        </TouchableOpacity>
        <Text style={styles.title}>통계</Text>
        <View style={styles.headerSpacer} />
      </View>

      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {/* 필터 영역 */}
        <View style={styles.filterRow}>
          {/* 차량 선택 */}
          <TouchableOpacity
            style={styles.carSelector}
            onPress={() => {
              setShowCarPicker(!showCarPicker);
              setShowPeriodPicker(false);
            }}
            activeOpacity={0.7}
          >
            <Ionicons name="car-sport" size={16} color={colors.primary} />
            <Text style={styles.carSelectorText} numberOfLines={1}>
              {selectedCar?.name || '차량 선택'}
            </Text>
            <Ionicons
              name={showCarPicker ? 'chevron-up' : 'chevron-down'}
              size={16}
              color="rgba(0,0,0,0.4)"
            />
          </TouchableOpacity>

          {/* 기간 선택 */}
          <TouchableOpacity
            style={styles.periodSelector}
            onPress={() => {
              setShowPeriodPicker(!showPeriodPicker);
              setShowCarPicker(false);
            }}
            activeOpacity={0.7}
          >
            <Text style={styles.periodText}>{currentPeriodLabel}</Text>
            <Ionicons
              name={showPeriodPicker ? 'chevron-up' : 'chevron-down'}
              size={18}
              color={colors.textSecondary}
            />
          </TouchableOpacity>
        </View>

        {/* 차량 선택 드롭다운 */}
        {showCarPicker && cars.length > 0 && (
          <View style={styles.dropdown}>
            {cars.map(car => (
              <TouchableOpacity
                key={car.id}
                style={[
                  styles.dropdownOption,
                  selectedCar?.id === car.id && styles.dropdownOptionActive,
                ]}
                onPress={() => {
                  selectCar(car.id);
                  setShowCarPicker(false);
                }}
              >
                <Ionicons
                  name="car-sport"
                  size={18}
                  color={selectedCar?.id === car.id ? colors.primary : 'rgba(0,0,0,0.5)'}
                />
                <Text
                  style={[
                    styles.dropdownOptionText,
                    selectedCar?.id === car.id && styles.dropdownOptionTextActive,
                  ]}
                >
                  {car.name}
                </Text>
                {selectedCar?.id === car.id && (
                  <Ionicons name="checkmark" size={18} color={colors.primary} />
                )}
              </TouchableOpacity>
            ))}
          </View>
        )}

        {/* 기간 선택 드롭다운 */}
        {showPeriodPicker && (
          <View style={styles.dropdown}>
            {periodOptions.map(option => (
              <TouchableOpacity
                key={option.value}
                style={[
                  styles.dropdownOption,
                  selectedPeriod === option.value && styles.dropdownOptionActive,
                ]}
                onPress={() => {
                  setSelectedPeriod(option.value);
                  setShowPeriodPicker(false);
                }}
              >
                <Text
                  style={[
                    styles.dropdownOptionText,
                    selectedPeriod === option.value && styles.dropdownOptionTextActive,
                  ]}
                >
                  {option.label}
                </Text>
                {selectedPeriod === option.value && (
                  <Ionicons name="checkmark" size={18} color={colors.primary} />
                )}
              </TouchableOpacity>
            ))}
          </View>
        )}

        {!selectedCar ? (
          <View style={styles.emptyState}>
            <Ionicons name="car-outline" size={48} color={colors.textTertiary} />
            <Text style={styles.emptyText}>차량을 먼저 추가해주세요</Text>
            <TouchableOpacity
              style={styles.emptyButton}
              onPress={() => router.push('/settings')}
            >
              <Text style={styles.emptyButtonText}>차량 추가하기</Text>
            </TouchableOpacity>
          </View>
        ) : (
          <>
            {/* 요약 카드 그리드 */}
            <View style={styles.statsGrid}>
              <StatCard
                icon="construct-outline"
                iconColor={colors.textSecondary}
                label="총 정비 횟수"
                value={`${stats.totalCount}회`}
              />
              <StatCard
                icon="calendar-outline"
                iconColor={colors.iconBlue}
                label="이번 달 정비 횟수"
                value={`${stats.thisMonthCount}회`}
              />
              <StatCard
                icon="cash-outline"
                iconColor={colors.iconGold}
                label="총 지출 비용"
                value={`${stats.totalCost.toLocaleString()}원`}
              />
              <StatCard
                icon="trending-up-outline"
                iconColor={colors.success}
                label="평균 비용"
                value={`${stats.avgCost.toLocaleString()}원`}
              />
            </View>

            {/* 유형별 비용 파이 차트 */}
            <GlassCard>
              <View style={styles.chartHeader}>
                <Ionicons name="pie-chart-outline" size={22} color={colors.primary} />
                <Text style={styles.chartTitle}>유형별 비용</Text>
              </View>

              {/* 카테고리 토글 */}
              <View style={styles.categoryToggle}>
                {chartCategoryOptions.map(option => (
                  <TouchableOpacity
                    key={option.value}
                    style={[
                      styles.categoryToggleButton,
                      selectedChartCategory === option.value && styles.categoryToggleButtonActive,
                    ]}
                    onPress={() => setSelectedChartCategory(option.value)}
                  >
                    <Text
                      style={[
                        styles.categoryToggleText,
                        selectedChartCategory === option.value && styles.categoryToggleTextActive,
                      ]}
                    >
                      {option.label}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>

              {typeData.length > 0 ? (
                <>
                  <PieChart data={typeData} totalCost={categoryTotalCost} />
                  <View style={styles.legendContainer}>
                    {typeData.map((item, index) => {
                      const percentage = categoryTotalCost > 0
                        ? ((item.cost / categoryTotalCost) * 100).toFixed(1)
                        : '0.0';
                      return (
                        <View key={index} style={styles.legendItem}>
                          <View style={[styles.legendDot, { backgroundColor: item.color }]} />
                          <Text style={styles.legendLabel} numberOfLines={1}>{item.type}</Text>
                          <Text style={styles.legendValue}>
                            {item.cost.toLocaleString()}원 ({percentage}%)
                          </Text>
                        </View>
                      );
                    })}
                  </View>
                </>
              ) : (
                <View style={styles.noDataContainer}>
                  <Text style={styles.noDataText}>해당 카테고리의 기록이 없습니다</Text>
                </View>
              )}
            </GlassCard>

            {/* 월별 비용 추이 라인 차트 */}
            <GlassCard>
              <View style={styles.chartHeader}>
                <Ionicons name="analytics-outline" size={22} color={colors.primary} />
                <Text style={styles.chartTitle}>월별 유형별 비용</Text>
              </View>

              {/* 카테고리 토글 */}
              <View style={styles.categoryToggle}>
                {chartCategoryOptions.map(option => (
                  <TouchableOpacity
                    key={option.value}
                    style={[
                      styles.categoryToggleButton,
                      selectedMonthlyCategory === option.value && styles.categoryToggleButtonActive,
                    ]}
                    onPress={() => setSelectedMonthlyCategory(option.value)}
                  >
                    <Text
                      style={[
                        styles.categoryToggleText,
                        selectedMonthlyCategory === option.value && styles.categoryToggleTextActive,
                      ]}
                    >
                      {option.label}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>

              <LineChart data={monthlyData} />
            </GlassCard>

            {/* 월별 누적 주행거리 추이 라인 차트 */}
            {monthlyMileageData.some(d => d.mileage > 0) && (
              <GlassCard>
                <View style={styles.chartHeader}>
                  <Ionicons name="speedometer-outline" size={22} color={colors.success} />
                  <Text style={styles.chartTitle}>월별 누적 주행거리</Text>
                </View>
                <MileageLineChart data={monthlyMileageData} />
              </GlassCard>
            )}

            {/* 차량별 상세 비교 */}
            {cars.length > 1 && (
              <GlassCard>
                <View style={styles.chartHeader}>
                  <Ionicons name="car-sport-outline" size={22} color={colors.primary} />
                  <Text style={styles.chartTitle}>차량별 상세 비교</Text>
                </View>
                {carStats.map((stat, index) => (
                  <View
                    key={stat.car.id}
                    style={[
                      styles.carStatItem,
                      index < carStats.length - 1 && styles.carStatItemBorder,
                    ]}
                  >
                    <Text style={styles.carStatName}>{stat.car.name}</Text>
                    <View style={styles.carStatRow}>
                      <View style={styles.carStatCol}>
                        <Text style={styles.carStatLabel}>정비 횟수</Text>
                        <Text style={styles.carStatValue}>{stat.totalCount}회</Text>
                      </View>
                      <View style={styles.carStatDivider} />
                      <View style={styles.carStatCol}>
                        <Text style={styles.carStatLabel}>총 지출</Text>
                        <Text style={[styles.carStatValue, styles.carStatCost]}>
                          {stat.totalCost.toLocaleString()}원
                        </Text>
                      </View>
                      <View style={styles.carStatDivider} />
                      <View style={styles.carStatCol}>
                        <Text style={styles.carStatLabel}>평균 비용</Text>
                        <Text style={styles.carStatValue}>{stat.avgCost.toLocaleString()}원</Text>
                      </View>
                    </View>
                  </View>
                ))}
              </GlassCard>
            )}
          </>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

// 요약 카드 컴포넌트
function StatCard({
  icon,
  iconColor,
  label,
  value,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  iconColor: string;
  label: string;
  value: string;
}) {
  return (
    <View style={styles.statCard}>
      <BlurView intensity={40} tint="light" style={styles.blurView}>
        <View style={styles.glassOverlay} />
      </BlurView>
      <View style={styles.statCardContent}>
        <Ionicons name={icon} size={24} color={iconColor} />
        <Text style={styles.statLabel}>{label}</Text>
        <Text style={styles.statValue}>{value}</Text>
      </View>
    </View>
  );
}

// 글래스 카드 컴포넌트
function GlassCard({ children }: { children: React.ReactNode }) {
  return (
    <View style={styles.glassCard}>
      <BlurView intensity={40} tint="light" style={styles.blurView}>
        <View style={styles.glassOverlay} />
      </BlurView>
      <View style={styles.glassCardContent}>{children}</View>
    </View>
  );
}

// 파이 차트 컴포넌트
function PieChart({ data, totalCost }: { data: { type: string; cost: number; color: string }[]; totalCost: number }) {
  const size = SCREEN_WIDTH - layout.screenPadding * 2 - spacing[8];
  const radius = size / 2 - 20;
  const innerRadius = radius * 0.6;
  const center = size / 2;

  // 데이터가 하나뿐이거나 하나가 100%인 경우 도넛 전체를 그림
  if (data.length === 1 || (data.length > 0 && data[0].cost === totalCost)) {
    const color = data[0]?.color || colors.primary;
    // 두 개의 반원으로 전체 도넛을 그림
    const d = `
      M ${center} ${center - radius}
      A ${radius} ${radius} 0 1 1 ${center} ${center + radius}
      A ${radius} ${radius} 0 1 1 ${center} ${center - radius}
      M ${center} ${center - innerRadius}
      A ${innerRadius} ${innerRadius} 0 1 0 ${center} ${center + innerRadius}
      A ${innerRadius} ${innerRadius} 0 1 0 ${center} ${center - innerRadius}
      Z
    `;
    return (
      <View style={styles.pieChartContainer}>
        <Svg width={size} height={size}>
          <Path d={d} fill={color} fillRule="evenodd" />
        </Svg>
      </View>
    );
  }

  let currentAngle = -90; // 12시 방향부터 시작

  const paths = data.map((item, index) => {
    const percentage = totalCost > 0 ? item.cost / totalCost : 0;
    const angle = percentage * 360;

    // 각도가 너무 작으면 스킵
    if (angle < 0.1) return null;

    const startAngle = currentAngle;
    const endAngle = currentAngle + angle;
    currentAngle = endAngle;

    const startRad = (startAngle * Math.PI) / 180;
    const endRad = (endAngle * Math.PI) / 180;

    const x1 = center + radius * Math.cos(startRad);
    const y1 = center + radius * Math.sin(startRad);
    const x2 = center + radius * Math.cos(endRad);
    const y2 = center + radius * Math.sin(endRad);

    const ix1 = center + innerRadius * Math.cos(startRad);
    const iy1 = center + innerRadius * Math.sin(startRad);
    const ix2 = center + innerRadius * Math.cos(endRad);
    const iy2 = center + innerRadius * Math.sin(endRad);

    const largeArc = angle > 180 ? 1 : 0;

    const d = `
      M ${ix1} ${iy1}
      L ${x1} ${y1}
      A ${radius} ${radius} 0 ${largeArc} 1 ${x2} ${y2}
      L ${ix2} ${iy2}
      A ${innerRadius} ${innerRadius} 0 ${largeArc} 0 ${ix1} ${iy1}
      Z
    `;

    return <Path key={index} d={d} fill={item.color} />;
  });

  return (
    <View style={styles.pieChartContainer}>
      <Svg width={size} height={size}>
        {paths}
      </Svg>
    </View>
  );
}

// 금액 포맷 함수 (만원 단위)
const formatCostLabel = (val: number): string => {
  if (val === 0) return '0';
  if (val >= 10000) {
    const man = val / 10000;
    return man % 1 === 0 ? `${man.toFixed(0)}만` : `${man.toFixed(1)}만`;
  }
  return val.toLocaleString();
};

// 주행거리 포맷 함수 (km 단위)
const formatMileageLabel = (val: number): string => {
  if (val === 0) return '0';
  if (val >= 10000) {
    const man = val / 10000;
    return man % 1 === 0 ? `${man.toFixed(0)}만km` : `${man.toFixed(1)}만km`;
  }
  if (val >= 1000) {
    return `${(val / 1000).toFixed(0)}천km`;
  }
  return `${val}km`;
};

// 라인 차트 컴포넌트
function LineChart({ data }: { data: { month: string; cost: number }[] }) {
  const chartWidth = SCREEN_WIDTH - layout.screenPadding * 2 - spacing[8];
  const chartHeight = 200;
  const paddingLeft = 60;
  const paddingRight = 20;
  const paddingTop = 20;
  const paddingBottom = 40;

  const graphWidth = chartWidth - paddingLeft - paddingRight;
  const graphHeight = chartHeight - paddingTop - paddingBottom;

  const maxCost = Math.max(...data.map(d => d.cost), 1);
  // 적절한 단위로 반올림 (1만, 5만, 10만, 50만, 100만 등)
  let roundedMax: number;
  if (maxCost <= 50000) {
    roundedMax = Math.ceil(maxCost / 10000) * 10000 || 10000;
  } else if (maxCost <= 500000) {
    roundedMax = Math.ceil(maxCost / 50000) * 50000;
  } else {
    roundedMax = Math.ceil(maxCost / 100000) * 100000;
  }

  const points = data.map((d, index) => {
    const x = paddingLeft + (index / (data.length - 1)) * graphWidth;
    const y = paddingTop + graphHeight - (d.cost / roundedMax) * graphHeight;
    return { x, y, ...d };
  });

  // 라인 패스
  const linePath = points
    .map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`)
    .join(' ');

  // Y축 라벨
  const yLabels = [0, roundedMax / 2, roundedMax];

  return (
    <View style={styles.lineChartContainer}>
      <Svg width={chartWidth} height={chartHeight}>
        {/* 그리드 라인 */}
        {yLabels.map((val, i) => {
          const y = paddingTop + graphHeight - (val / roundedMax) * graphHeight;
          return (
            <Line
              key={i}
              x1={paddingLeft}
              y1={y}
              x2={chartWidth - paddingRight}
              y2={y}
              stroke="rgba(0,0,0,0.1)"
              strokeWidth={1}
            />
          );
        })}

        {/* Y축 라벨 */}
        {yLabels.map((val, i) => {
          const y = paddingTop + graphHeight - (val / roundedMax) * graphHeight;
          return (
            <SvgText
              key={i}
              x={paddingLeft - 8}
              y={y + 4}
              fontSize={11}
              fill="rgba(0,0,0,0.5)"
              textAnchor="end"
            >
              {formatCostLabel(val)}
            </SvgText>
          );
        })}

        {/* 라인 */}
        <Path
          d={linePath}
          stroke={colors.primary}
          strokeWidth={2}
          fill="none"
        />

        {/* 포인트 */}
        {points.map((p, i) => (
          <Circle
            key={i}
            cx={p.x}
            cy={p.y}
            r={5}
            fill={colors.primary}
          />
        ))}

        {/* X축 라벨 */}
        {points.map((p, i) => (
          <SvgText
            key={i}
            x={p.x}
            y={chartHeight - 10}
            fontSize={11}
            fill="rgba(0,0,0,0.5)"
            textAnchor="middle"
          >
            {p.month}
          </SvgText>
        ))}
      </Svg>
    </View>
  );
}

// 주행거리 라인 차트 컴포넌트
function MileageLineChart({ data }: { data: { month: string; mileage: number }[] }) {
  const chartWidth = SCREEN_WIDTH - layout.screenPadding * 2 - spacing[8];
  const chartHeight = 200;
  const paddingLeft = 70;
  const paddingRight = 20;
  const paddingTop = 20;
  const paddingBottom = 40;

  const graphWidth = chartWidth - paddingLeft - paddingRight;
  const graphHeight = chartHeight - paddingTop - paddingBottom;

  const maxMileage = Math.max(...data.map(d => d.mileage), 1);
  const minMileage = Math.min(...data.filter(d => d.mileage > 0).map(d => d.mileage), maxMileage);

  // 범위 계산 (최소값과 최대값 사이)
  const range = maxMileage - minMileage;
  const padding = range * 0.1 || 1000; // 10% 여유 또는 최소 1000
  const chartMin = Math.max(0, Math.floor((minMileage - padding) / 1000) * 1000);
  const chartMax = Math.ceil((maxMileage + padding) / 1000) * 1000;
  const chartRange = chartMax - chartMin || 1;

  const points = data.map((d, index) => {
    const x = paddingLeft + (index / (data.length - 1)) * graphWidth;
    const y = d.mileage > 0
      ? paddingTop + graphHeight - ((d.mileage - chartMin) / chartRange) * graphHeight
      : paddingTop + graphHeight;
    return { x, y, ...d };
  });

  // 라인 패스 (주행거리가 있는 포인트만 연결)
  const validPoints = points.filter(p => p.mileage > 0);
  const linePath = validPoints
    .map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`)
    .join(' ');

  // Y축 라벨
  const yLabels = [chartMin, (chartMin + chartMax) / 2, chartMax];

  return (
    <View style={styles.lineChartContainer}>
      <Svg width={chartWidth} height={chartHeight}>
        {/* 그리드 라인 */}
        {yLabels.map((val, i) => {
          const y = paddingTop + graphHeight - ((val - chartMin) / chartRange) * graphHeight;
          return (
            <Line
              key={i}
              x1={paddingLeft}
              y1={y}
              x2={chartWidth - paddingRight}
              y2={y}
              stroke="rgba(0,0,0,0.1)"
              strokeWidth={1}
            />
          );
        })}

        {/* Y축 라벨 */}
        {yLabels.map((val, i) => {
          const y = paddingTop + graphHeight - ((val - chartMin) / chartRange) * graphHeight;
          return (
            <SvgText
              key={i}
              x={paddingLeft - 8}
              y={y + 4}
              fontSize={10}
              fill="rgba(0,0,0,0.5)"
              textAnchor="end"
            >
              {formatMileageLabel(val)}
            </SvgText>
          );
        })}

        {/* 라인 */}
        {validPoints.length > 1 && (
          <Path
            d={linePath}
            stroke={colors.success}
            strokeWidth={2}
            fill="none"
          />
        )}

        {/* 포인트 */}
        {points.map((p, i) => (
          p.mileage > 0 && (
            <Circle
              key={i}
              cx={p.x}
              cy={p.y}
              r={5}
              fill={colors.success}
            />
          )
        ))}

        {/* X축 라벨 */}
        {points.map((p, i) => (
          <SvgText
            key={i}
            x={p.x}
            y={chartHeight - 10}
            fontSize={11}
            fill="rgba(0,0,0,0.5)"
            textAnchor="middle"
          >
            {p.month}
          </SvgText>
        ))}
      </Svg>
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
    justifyContent: 'space-between',
    paddingHorizontal: spacing[2],
    paddingVertical: spacing[3],
  },
  backButton: {
    width: 44,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerSpacer: {
    width: 44,
  },
  title: {
    fontSize: 18,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: layout.screenPadding,
    paddingBottom: 120,
  },
  // 필터 영역
  filterRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing[4],
    gap: spacing[3],
  },
  carSelector: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(212, 168, 75, 0.1)',
    paddingHorizontal: spacing[3],
    paddingVertical: spacing[2],
    borderRadius: 12,
    gap: spacing[2],
  },
  carSelectorText: {
    fontSize: 14,
    fontWeight: '600',
    color: colors.primary,
    maxWidth: 100,
  },
  periodSelector: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(255,255,255,0.9)',
    paddingHorizontal: spacing[4],
    paddingVertical: spacing[2],
    borderRadius: 12,
    gap: spacing[2],
  },
  periodText: {
    fontSize: 14,
    fontWeight: '600',
    color: 'rgba(0,0,0,0.8)',
  },
  dropdown: {
    backgroundColor: 'rgba(255,255,255,0.95)',
    borderRadius: 12,
    marginBottom: spacing[4],
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 12,
    elevation: 5,
    overflow: 'hidden',
  },
  dropdownOption: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing[4],
    paddingVertical: spacing[3],
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(0,0,0,0.05)',
    gap: spacing[3],
  },
  dropdownOptionActive: {
    backgroundColor: 'rgba(212, 168, 75, 0.1)',
  },
  dropdownOptionText: {
    flex: 1,
    fontSize: 15,
    color: 'rgba(0,0,0,0.7)',
  },
  dropdownOptionTextActive: {
    color: colors.primary,
    fontWeight: '600',
  },
  // 요약 카드 그리드
  statsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing[3],
    marginBottom: spacing[4],
  },
  statCard: {
    width: '48%',
    borderRadius: 16,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 8,
    elevation: 3,
  },
  blurView: {
    ...StyleSheet.absoluteFillObject,
  },
  glassOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(255, 255, 255, 0.75)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.9)',
    borderRadius: 16,
  },
  statCardContent: {
    padding: spacing[4],
    gap: spacing[2],
  },
  statLabel: {
    fontSize: 13,
    color: 'rgba(0,0,0,0.5)',
    marginTop: spacing[2],
  },
  statValue: {
    fontSize: 20,
    fontWeight: '700',
    color: 'rgba(0,0,0,0.8)',
  },
  // 글래스 카드
  glassCard: {
    borderRadius: 20,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 12,
    elevation: 5,
    marginBottom: spacing[4],
  },
  glassCardContent: {
    padding: spacing[4],
  },
  chartHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
    marginBottom: spacing[4],
  },
  chartTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: 'rgba(0,0,0,0.8)',
  },
  // 카테고리 토글
  categoryToggle: {
    flexDirection: 'row',
    backgroundColor: 'rgba(0,0,0,0.05)',
    borderRadius: 10,
    padding: 4,
    marginBottom: spacing[4],
  },
  categoryToggleButton: {
    flex: 1,
    paddingVertical: spacing[2],
    borderRadius: 8,
    alignItems: 'center',
  },
  categoryToggleButtonActive: {
    backgroundColor: 'rgba(255,255,255,0.9)',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  categoryToggleText: {
    fontSize: 13,
    fontWeight: '500',
    color: 'rgba(0,0,0,0.5)',
  },
  categoryToggleTextActive: {
    color: colors.primary,
    fontWeight: '600',
  },
  noDataContainer: {
    alignItems: 'center',
    paddingVertical: spacing[8],
  },
  noDataText: {
    fontSize: 14,
    color: 'rgba(0,0,0,0.4)',
  },
  // 파이 차트
  pieChartContainer: {
    alignItems: 'center',
    marginBottom: spacing[4],
  },
  legendContainer: {
    gap: spacing[2],
  },
  legendItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
  },
  legendDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
  },
  legendLabel: {
    flex: 1,
    fontSize: 14,
    color: 'rgba(0,0,0,0.7)',
  },
  legendValue: {
    fontSize: 14,
    fontWeight: '600',
    color: 'rgba(0,0,0,0.8)',
  },
  // 라인 차트
  lineChartContainer: {
    alignItems: 'center',
  },
  // 차량별 통계
  carStatItem: {
    paddingVertical: spacing[3],
  },
  carStatItemBorder: {
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(0,0,0,0.08)',
  },
  carStatName: {
    fontSize: 15,
    fontWeight: '600',
    color: 'rgba(0,0,0,0.8)',
    marginBottom: spacing[2],
  },
  carStatRow: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(0,0,0,0.03)',
    borderRadius: 12,
    padding: spacing[3],
  },
  carStatCol: {
    flex: 1,
    alignItems: 'center',
  },
  carStatDivider: {
    width: 1,
    height: 30,
    backgroundColor: 'rgba(0,0,0,0.1)',
  },
  carStatLabel: {
    fontSize: 12,
    color: 'rgba(0,0,0,0.5)',
    marginBottom: spacing[1],
  },
  carStatValue: {
    fontSize: 14,
    fontWeight: '600',
    color: 'rgba(0,0,0,0.8)',
  },
  carStatCost: {
    color: colors.primary,
  },
  // Empty state
  emptyState: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: spacing[16],
    gap: spacing[3],
  },
  emptyText: {
    fontSize: 16,
    color: colors.textSecondary,
    marginTop: spacing[2],
  },
  emptyButton: {
    marginTop: spacing[2],
    paddingHorizontal: spacing[6],
    paddingVertical: spacing[3],
    backgroundColor: colors.primary,
    borderRadius: 12,
  },
  emptyButtonText: {
    fontSize: 15,
    fontWeight: '600',
    color: colors.background,
  },
});
