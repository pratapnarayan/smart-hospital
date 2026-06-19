import ReactApexChart from 'react-apexcharts'
import { Card } from 'antd'
import { DollarCircleOutlined, CreditCardOutlined, RiseOutlined, PercentageOutlined } from '@ant-design/icons'
import { PageHeader } from '@/components/common'
import { KpiCard, EmptyChart, AnalyticsFilter, ExportToolbar } from '@/components/analytics'
import { chartConfigs, formatCurrency } from '@/theme/chartTheme'
import { useFinanceAnalytics } from '@/hooks/useAnalytics'
import { withDemoFallback, DEMO_FINANCE } from '@/hooks/useDemoData'
import type { FinanceAnalytics } from '@/types'

export function FinancialAnalyticsPage() {
  const { data: raw, isLoading } = useFinanceAnalytics()
  const { data, isDemo } = withDemoFallback<FinanceAnalytics>(raw, DEMO_FINANCE, isLoading)

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4">
        <PageHeader title="Financial Analytics" subtitle="Revenue, expenses, and profitability insights" />
        <ExportToolbar section="finance" isDemoData={isDemo} />
      </div>
      <AnalyticsFilter />

      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
        <KpiCard title="Total Revenue" value={formatCurrency(data.totalRevenue)} loading={isLoading} icon={<DollarCircleOutlined />} color="success" />
        <KpiCard title="Total Expenses" value={formatCurrency(data.totalExpenses)} loading={isLoading} icon={<CreditCardOutlined />} color="warning" />
        <KpiCard title="Net Profit" value={formatCurrency(data.netProfit)} loading={isLoading} icon={<RiseOutlined />} color="primary" />
        <KpiCard title="Collection Efficiency" value={`${(data.collectionEfficiencyPct ?? 0).toFixed(1)}%`} loading={isLoading} icon={<PercentageOutlined />} color="cyan" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <Card title="Daily Revenue Trend" className="medical-card" styles={{ body: { padding: '24px' } }}>
            {data.dailyRevenue?.length ? (
              <ReactApexChart type="area" height={260}
                series={[{ name: 'Revenue (₹)', data: data.dailyRevenue.map(p => p.value) }]}
                options={{ ...chartConfigs.area('#1677ff'), xaxis: { categories: data.dailyRevenue.map(p => p.label), labels: { rotate: -45, style: { fontSize: '10px' } } }, yaxis: { labels: { formatter: (v: number) => formatCurrency(v) } } }} />
            ) : <EmptyChart height={260} />}
          </Card>
        </div>
        <div>
          <Card title="Revenue by Source" className="medical-card" styles={{ body: { padding: '24px' } }}>
            {data.revenueBySource?.length ? (
              <ReactApexChart type="donut" height={260}
                series={data.revenueBySource.map(p => p.value)}
                options={{ ...chartConfigs.donut(), labels: data.revenueBySource.map(p => p.name) }} />
            ) : <EmptyChart height={260} />}
          </Card>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card title="Revenue by Doctor (Top 10)" className="medical-card" styles={{ body: { padding: '24px' } }}>
          {data.revenueByDoctor?.length ? (
            <ReactApexChart type="bar" height={240}
              series={[{ name: 'Revenue (₹)', data: data.revenueByDoctor.map(p => p.value) }]}
              options={{ ...chartConfigs.horizontalBar('#722ed1'), xaxis: { categories: data.revenueByDoctor.map(p => p.name) } }} />
          ) : <EmptyChart height={240} />}
        </Card>
        <Card title="Monthly Revenue vs Expenses" className="medical-card" styles={{ body: { padding: '24px' } }}>
          {data.monthlyComparison?.length ? (
            <ReactApexChart type="bar" height={240}
              series={[{ name: 'Revenue', data: data.monthlyComparison.map(p => p.value) }]}
              options={{ ...chartConfigs.bar('#52c41a'), xaxis: { categories: data.monthlyComparison.map(p => p.name) } }} />
          ) : <EmptyChart height={240} />}
        </Card>
      </div>
    </div>
  )
}
