import ReactApexChart from 'react-apexcharts'
import { Row, Col, Card } from 'antd'
import { PageHeader } from '@/components/common'
import { KpiCard, EmptyChart, AnalyticsFilter, ExportToolbar, baseChartOptions, chartPalette } from '@/components/analytics'
import { usePharmacyAnalytics } from '@/hooks/useAnalytics'
import { withDemoFallback, DEMO_PHARMACY } from '@/hooks/useDemoData'
import type { PharmacyAnalytics } from '@/types'

const fmt = (v: number) => `₹${Number(v ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`

export function PharmacyAnalyticsPage() {
  const { data: raw, isLoading } = usePharmacyAnalytics()
  const { data, isDemo } = withDemoFallback<PharmacyAnalytics>(raw, DEMO_PHARMACY, isLoading)

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
        <PageHeader title="Pharmacy Analytics" subtitle="Medicine sales, stock health and category performance" />
        <ExportToolbar section="pharmacy" isDemoData={isDemo} />
      </div>
      <AnalyticsFilter />

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {[
          { title: 'Total Revenue', value: fmt(data.totalMedicineRevenue) },
          { title: 'Bills Issued', value: data.totalBillsIssued?.toLocaleString() ?? '0' },
          { title: 'Low Stock Alerts', value: data.lowStockAlerts?.toString() ?? '0' },
          { title: 'Expiry Alerts', value: data.expiryAlerts?.toString() ?? '0' },
        ].map(k => <Col xs={24} sm={12} md={6} key={k.title}><KpiCard {...k} loading={isLoading} /></Col>)}
      </Row>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} lg={16}>
          <Card title="Revenue Trend">
            {data.revenueTrend?.length ? (
              <ReactApexChart type="area" height={260}
                series={[{ name: 'Revenue (₹)', data: data.revenueTrend.map(p => p.value) }]}
                options={{ ...baseChartOptions, xaxis: { categories: data.revenueTrend.map(p => p.label), labels: { rotate: -45, style: { fontSize: '10px' } } }, colors: ['#fa8c16'], fill: { type: 'gradient', gradient: { opacityFrom: 0.4, opacityTo: 0.05 } }, stroke: { curve: 'smooth', width: 2 } }} />
            ) : <EmptyChart height={260} />}
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="Stock Health">
            {data.stockHealthDistribution?.length ? (
              <ReactApexChart type="donut" height={260}
                series={data.stockHealthDistribution.map(p => p.value)}
                options={{ ...baseChartOptions, labels: data.stockHealthDistribution.map(p => p.name), colors: ['#52c41a', '#faad14', '#ff4d4f'], legend: { position: 'bottom' } }} />
            ) : <EmptyChart height={260} />}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="Top 10 Medicines by Revenue">
            {data.topMedicinesByRevenue?.length ? (
              <ReactApexChart type="bar" height={240}
                series={[{ name: 'Revenue (₹)', data: data.topMedicinesByRevenue.map(p => p.value) }]}
                options={{ ...baseChartOptions, plotOptions: { bar: { horizontal: true, borderRadius: 4 } }, xaxis: { categories: data.topMedicinesByRevenue.map(p => p.name) }, colors: ['#fa8c16'] }} />
            ) : <EmptyChart height={240} />}
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Revenue by Category">
            {data.revenueByCategory?.length ? (
              <ReactApexChart type="bar" height={240}
                series={[{ name: 'Revenue (₹)', data: data.revenueByCategory.map(p => p.value) }]}
                options={{ ...baseChartOptions, xaxis: { categories: data.revenueByCategory.map(p => p.name) }, colors: ['#13c2c2'], plotOptions: { bar: { borderRadius: 4 } } }} />
            ) : <EmptyChart height={240} />}
          </Card>
        </Col>
      </Row>
    </div>
  )
}
