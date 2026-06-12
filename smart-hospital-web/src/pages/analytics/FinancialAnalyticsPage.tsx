import ReactApexChart from 'react-apexcharts'
import { Row, Col, Card } from 'antd'
import { PageHeader } from '@/components/common'
import { KpiCard, EmptyChart, AnalyticsFilter, ExportToolbar, baseChartOptions, chartPalette } from '@/components/analytics'
import { useFinanceAnalytics } from '@/hooks/useAnalytics'
import { withDemoFallback, DEMO_FINANCE } from '@/hooks/useDemoData'
import type { FinanceAnalytics } from '@/types'

const fmt = (v: number) => `₹${Number(v ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`

export function FinancialAnalyticsPage() {
  const { data: raw, isLoading } = useFinanceAnalytics()
  const { data, isDemo } = withDemoFallback<FinanceAnalytics>(raw, DEMO_FINANCE, isLoading)

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
        <PageHeader title="Financial Analytics" subtitle="Revenue, expenses, and profitability insights" />
        <ExportToolbar section="finance" isDemoData={isDemo} />
      </div>
      <AnalyticsFilter />

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {[
          { title: 'Total Revenue', value: fmt(data.totalRevenue) },
          { title: 'Total Expenses', value: fmt(data.totalExpenses) },
          { title: 'Net Profit', value: fmt(data.netProfit) },
          { title: 'Collection Efficiency', value: `${data.collectionEfficiencyPct?.toFixed(1)}%` },
        ].map(k => <Col xs={24} sm={12} md={6} key={k.title}><KpiCard {...k} loading={isLoading} /></Col>)}
      </Row>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} lg={16}>
          <Card title="Daily Revenue Trend">
            {data.dailyRevenue?.length ? (
              <ReactApexChart type="area" height={260}
                series={[{ name: 'Revenue (₹)', data: data.dailyRevenue.map(p => p.value) }]}
                options={{ ...baseChartOptions, xaxis: { categories: data.dailyRevenue.map(p => p.label), labels: { rotate: -45, style: { fontSize: '10px' } } }, colors: ['#1677ff'], fill: { type: 'gradient', gradient: { opacityFrom: 0.4, opacityTo: 0.05 } }, stroke: { curve: 'smooth', width: 2 } }} />
            ) : <EmptyChart height={260} />}
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="Revenue by Source">
            {data.revenueBySource?.length ? (
              <ReactApexChart type="donut" height={260}
                series={data.revenueBySource.map(p => p.value)}
                options={{ ...baseChartOptions, labels: data.revenueBySource.map(p => p.name), colors: chartPalette, legend: { position: 'bottom' } }} />
            ) : <EmptyChart height={260} />}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="Revenue by Doctor (Top 10)">
            {data.revenueByDoctor?.length ? (
              <ReactApexChart type="bar" height={240}
                series={[{ name: 'Revenue (₹)', data: data.revenueByDoctor.map(p => p.value) }]}
                options={{ ...baseChartOptions, plotOptions: { bar: { horizontal: true, borderRadius: 4 } }, xaxis: { categories: data.revenueByDoctor.map(p => p.name) }, colors: ['#722ed1'] }} />
            ) : <EmptyChart height={240} />}
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Monthly Revenue vs Expenses">
            {data.monthlyComparison?.length ? (
              <ReactApexChart type="bar" height={240}
                series={[{ name: 'Revenue', data: data.monthlyComparison.map(p => p.value) }]}
                options={{ ...baseChartOptions, xaxis: { categories: data.monthlyComparison.map(p => p.name) }, colors: ['#52c41a'], plotOptions: { bar: { borderRadius: 4 } } }} />
            ) : <EmptyChart height={240} />}
          </Card>
        </Col>
      </Row>
    </div>
  )
}
