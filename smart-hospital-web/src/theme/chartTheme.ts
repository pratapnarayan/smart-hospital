// src/theme/chartTheme.ts
import { designTokens } from './index'

export const chartPalette = [
  designTokens.colors.chart.primary,
  designTokens.colors.chart.secondary,
  designTokens.colors.chart.success,
  designTokens.colors.chart.warning,
  designTokens.colors.chart.danger,
  designTokens.colors.chart.purple,
  designTokens.colors.chart.magenta,
  designTokens.colors.chart.orange,
  designTokens.colors.chart.cyan,
  designTokens.colors.chart.lime,
]

export const baseChartOptions: ApexCharts.ApexOptions = {
  chart: {
    fontFamily: designTokens.typography.fontFamily.sans,
    toolbar: { show: false },
    zoom: { enabled: false },
    animations: {
      enabled: true,
      easing: 'easeinout',
      speed: 800,
      animateGradually: { enabled: true, delay: 150 },
      dynamicAnimation: { enabled: true, speed: 350 },
    },
    background: 'transparent',
  },
  theme: { mode: 'light' },
  dataLabels: { enabled: false },
  stroke: { curve: 'smooth', width: 2 },
  grid: {
    borderColor: designTokens.colors.neutral[200],
    strokeDashArray: 4,
    padding: { top: 0, right: 0, bottom: 0, left: 10 },
  },
  xaxis: {
    axisBorder: { show: false },
    axisTicks: { show: false },
    labels: {
      style: {
        colors: designTokens.colors.text.tertiary,
        fontSize: '11px',
        fontFamily: designTokens.typography.fontFamily.sans,
      },
    },
  },
  yaxis: {
    labels: {
      style: {
        colors: designTokens.colors.text.tertiary,
        fontSize: '11px',
        fontFamily: designTokens.typography.fontFamily.sans,
      },
    },
  },
  legend: {
    fontSize: '12px',
    fontFamily: designTokens.typography.fontFamily.sans,
    labels: { colors: designTokens.colors.text.secondary },
    markers: { size: 4 },
  },
  tooltip: {
    theme: 'light',
    style: {
      fontSize: '12px',
      fontFamily: designTokens.typography.fontFamily.sans,
    },
    fillSeriesColor: false,
    marker: { show: true },
  },
}

export const chartConfigs = {
  area: (color: string = designTokens.colors.chart.primary): ApexCharts.ApexOptions => ({
    ...baseChartOptions,
    colors: [color],
    fill: {
      type: 'gradient',
      gradient: {
        shadeIntensity: 1,
        opacityFrom: 0.4,
        opacityTo: 0.05,
        stops: [0, 100],
      },
    },
    stroke: { curve: 'smooth', width: 2 },
  }),

  line: (color: string = designTokens.colors.chart.primary): ApexCharts.ApexOptions => ({
    ...baseChartOptions,
    colors: [color],
    stroke: { curve: 'straight', width: 2 },
    fill: { type: 'none' },
  }),

  bar: (color: string = designTokens.colors.chart.primary): ApexCharts.ApexOptions => ({
    ...baseChartOptions,
    colors: [color],
    plotOptions: {
      bar: {
        borderRadius: 4,
        columnWidth: '50%',
      },
    },
  }),

  horizontalBar: (color: string = designTokens.colors.chart.primary): ApexCharts.ApexOptions => ({
    ...baseChartOptions,
    colors: [color],
    plotOptions: {
      bar: {
        horizontal: true,
        borderRadius: 4,
        barHeight: '60%',
      },
    },
  }),

  donut: (): ApexCharts.ApexOptions => ({
    ...baseChartOptions,
    colors: chartPalette,
    plotOptions: {
      pie: {
        donut: {
          size: '65%',
          labels: {
            show: true,
            name: {
              fontSize: '14px',
              fontFamily: designTokens.typography.fontFamily.sans,
              color: designTokens.colors.text.secondary,
            },
            value: {
              fontSize: '24px',
              fontFamily: designTokens.typography.fontFamily.sans,
              fontWeight: '600',
              color: designTokens.colors.text.primary,
            },
            total: {
              show: true,
              fontSize: '14px',
              fontFamily: designTokens.typography.fontFamily.sans,
              fontWeight: '500',
              color: designTokens.colors.text.tertiary,
            },
          },
        },
      },
    },
    legend: { position: 'bottom' },
  }),

  sparkline: (color: string = designTokens.colors.chart.primary): ApexCharts.ApexOptions => ({
    chart: {
      ...baseChartOptions.chart,
      sparkline: { enabled: true },
    },
    colors: [color],
    stroke: { curve: 'smooth', width: 2 },
    tooltip: {
      ...baseChartOptions.tooltip,
      x: { show: false },
    },
  }),
}

export const formatCurrency = (value: number): string =>
  `₹${Number(value ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`

export const formatCompactCurrency = (value: number): string =>
  `₹${Math.floor(value / 1000)}k`

export const formatNumber = (value: number): string =>
  value?.toLocaleString('en-IN') ?? '0'

export const formatPercentage = (value: number): string =>
  `${value >= 0 ? '+' : ''}${value.toFixed(1)}%`

// Legacy colour map kept for other analytics pages that import from here
export const chartColors = {
  primary: designTokens.colors.chart.primary,
  success: designTokens.colors.chart.success,
  warning: designTokens.colors.chart.warning,
  danger: designTokens.colors.chart.danger,
  purple: designTokens.colors.chart.purple,
  cyan: designTokens.colors.chart.cyan,
  orange: designTokens.colors.chart.orange,
  geekblue: '#2f54eb',
}
