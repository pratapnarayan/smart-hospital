// Global ApexCharts theme to match Ant Design palette
export const chartColors = {
  primary: '#1677ff',
  success: '#52c41a',
  warning: '#faad14',
  danger: '#ff4d4f',
  purple: '#722ed1',
  cyan: '#13c2c2',
  orange: '#fa8c16',
  geekblue: '#2f54eb',
}

export const chartPalette = [
  '#1677ff', '#52c41a', '#faad14', '#ff4d4f',
  '#722ed1', '#13c2c2', '#fa8c16', '#2f54eb',
]

export const baseChartOptions: ApexCharts.ApexOptions = {
  chart: {
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
    toolbar: { show: false },
    animations: {
      enabled: true,
      easing: 'easeinout',
      speed: 600,
    },
  },
  colors: chartPalette,
  grid: {
    borderColor: '#f0f2f5',
    strokeDashArray: 3,
  },
  tooltip: {
    theme: 'light',
  },
  dataLabels: { enabled: false },
}
