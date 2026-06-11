import ReactApexChart from 'react-apexcharts'
import { Row, Col, Card } from 'antd'
import { PageHeader } from '@/components/common'
import { KpiCard, EmptyChart, AnalyticsFilter, ExportToolbar, baseChartOptions, chartPalette } from '@/components/analytics'
import { useLaboratoryAnalytics } from '@/hooks/useAnalytics'
import { withDemoFallback, DEMO_LABORATORY } from '@/hooks/useDemoData'
import type { LaboratoryAnalytics } from '@/types'

const fmt = (v: number) => `₹${Number(v ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`

export function LaboratoryAnalyticsPage() {
  const { data: raw, isLoading } = useLaboratoryAnalytics()
  const { data, isDemo } = withDemoFallback<LaboratoryAnalytics>(raw, DEMO_LABORATORY)

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
        <PageHeader title="Laboratory Analytics" subtitle="Test volumes, revenue and departmental referrals" />
        <ExportToolbar section="laboratory" isDemoData={isDemo} />
      </div>
      <AnalyticsFilter />

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {[
          { title: 'Tests Performed', value: data.totalTestsPerformed?.toLocaleString() ?? '0' },
          { title: 'Total Revenue', value: fmt(data.totalRevenue) },
          { title: 'Pending Reports', value: data.pendingReports?.toString() ?? '0' },
        ].map(k => <Col xs={24} sm={12} md={8} key={k.title}><KpiCard {...k} loading={isLoading} /></Col>)}
      </Row>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} lg={16}>
          <Card title="Daily Tests Trend">
            {data.dailyTestsTrend?.length ? (
              <ReactApexChart type="area" height={260}
                series={[{ name: 'Tests', data: data.dailyTestsTrend.map(p => p.value) }]}
                options={{ ...baseChartOptions, xaxis: { categories: data.dailyTestsTrend.map(p => p.label), labels: { rotate: -45, style: { fontSize: '10px' } } }, colors: ['#722ed1'], fill: { type: 'gradient', gradient: { opacityFrom: 0.4, opacityTo: 0.05 } }, stroke: { curve: 'smooth', width: 2 } }} />
            ) : <EmptyChart height={260} />}
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="Status Distribution">
            {data.statusDistribution?.length ? (
              <ReactApexChart type="donut" height={260}
                series={data.statusDistribution.map(p => p.value)}
                options={{ ...baseChartOptions, labels: data.statusDistribution.map(p => p.name), colors: ['#52c41a', '#faad14'], legend: { position: 'bottom' } }} />
            ) : <EmptyChart height={260} />}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="Top 10 Tests by Volume">
            {data.topTests?.length ? (
              <ReactApexChart type="bar" height={240}
                series={[{ name: 'Tests', data: data.topTests.map(p => p.value) }]}
                options={{ ...baseChartOptions, plotOptions: { bar: { horizontal: true, borderRadius: 4 } }, xaxis: { categories: data.topTests.map(p => p.name) }, colors: ['#722ed1'] }} />
            ) : <EmptyChart height={240} />}
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Referrals by Department">
            {data.byDepartmentReferral?.length ? (
              <ReactApexChart type="bar" height={240}
                series={[{ name: 'Referrals', data: data.byDepartmentReferral.map(p => p.value) }]}
                options={{ ...baseChartOptions, xaxis: { categories: data.byDepartmentReferral.map(p => p.name) }, colors: ['#13c2c2'], plotOptions: { bar: { borderRadius: 4 } } }} />
            ) : <EmptyChart height={240} />}
          </Card>
        </Col>
      </Row>
    </div>
  )
}
