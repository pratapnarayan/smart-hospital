import ReactApexChart from 'react-apexcharts'
import { Row, Col, Card, Typography } from 'antd'
import { PageHeader } from '@/components/common'
import {
  KpiCard, EmptyChart, AnalyticsFilter, ExportToolbar,
  baseChartOptions, chartPalette,
} from '@/components/analytics'
import { useExecutiveDashboard } from '@/hooks/useAnalytics'
import { withDemoFallback, DEMO_EXECUTIVE } from '@/hooks/useDemoData'
import type { ExecutiveDashboard } from '@/types'

const fmt = (v: number) =>
  `₹${Number(v ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`

export function ExecutiveDashboardPage() {
  const { data: raw, isLoading } = useExecutiveDashboard()
  const { data, isDemo } = withDemoFallback<ExecutiveDashboard>(raw, DEMO_EXECUTIVE)

  return (
    <div>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
        <PageHeader title="Executive Dashboard" subtitle="Real-time hospital performance overview" />
        <ExportToolbar section="executive" isDemoData={isDemo} />
      </div>

      <AnalyticsFilter />

      {/* Section 1: KPI Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {[
          { title: "Today's Revenue", value: fmt(data.todayRevenue), trend: data.todayRevenueTrend, subtitle: 'vs yesterday' },
          { title: 'Month Revenue', value: fmt(data.monthRevenue), trend: data.monthRevenueTrend, subtitle: 'vs last month' },
          { title: 'Total Patients', value: data.totalPatients.toLocaleString(), trend: data.totalPatientsTrend, subtitle: 'all time' },
          { title: "Today's Appointments", value: data.todayAppointments.toString(), trend: data.todayAppointmentsTrend, subtitle: 'vs yesterday' },
          { title: 'Pending Payments', value: fmt(data.pendingPayments), trend: null, subtitle: 'outstanding' },
          { title: 'Doctors Available', value: data.doctorsAvailableToday.toString(), trend: null, subtitle: 'today' },
          { title: 'Current Admissions', value: data.currentAdmissions.toString(), trend: null, subtitle: 'IPD active' },
          { title: 'Lab Tests Today', value: data.labTestsToday.toString(), trend: null, subtitle: 'pathology' },
          { title: 'Medicine Sales', value: fmt(data.medicineSalesToday), trend: null, subtitle: 'today' },
          { title: 'Inventory Alerts', value: data.inventoryAlerts.toString(), trend: data.inventoryAlerts > 0 ? -1 : 0, subtitle: 'items low/out' },
        ].map((kpi) => (
          <Col xs={24} sm={12} md={8} lg={6} xl={4} key={kpi.title} style={{ minWidth: 180 }}>
            <KpiCard {...kpi} loading={isLoading} />
          </Col>
        ))}
      </Row>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        {/* Section 2: Revenue Trend */}
        <Col xs={24} lg={16}>
          <Card title="Revenue Trend — Last 30 Days"
            extra={<Typography.Text type="secondary">Daily</Typography.Text>}>
            {data.revenueTrend?.length ? (
              <ReactApexChart
                type="area"
                height={260}
                series={[{ name: 'Revenue (₹)', data: data.revenueTrend.map(p => p.value) }]}
                options={{
                  ...baseChartOptions,
                  xaxis: { categories: data.revenueTrend.map(p => p.label), labels: { rotate: -45, style: { fontSize: '10px' } } },
                  yaxis: { labels: { formatter: (v) => `₹${(v/1000).toFixed(0)}k` } },
                  fill: { type: 'gradient', gradient: { shadeIntensity: 1, opacityFrom: 0.4, opacityTo: 0.05 } },
                  stroke: { curve: 'smooth', width: 2 },
                  colors: ['#1677ff'],
                }}
              />
            ) : <EmptyChart height={260} />}
          </Card>
        </Col>

        {/* Section 4: Revenue Distribution Donut */}
        <Col xs={24} lg={8}>
          <Card title="Revenue by Source">
            {data.revenueBySource?.length ? (
              <ReactApexChart
                type="donut"
                height={260}
                series={data.revenueBySource.map(p => p.value)}
                options={{
                  ...baseChartOptions,
                  labels: data.revenueBySource.map(p => p.name),
                  colors: chartPalette,
                  legend: { position: 'bottom' },
                  plotOptions: { pie: { donut: { size: '65%' } } },
                }}
              />
            ) : <EmptyChart height={260} />}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        {/* Section 3: Patient Growth */}
        <Col xs={24} lg={12}>
          <Card title="Patient Growth Trend">
            {data.patientGrowth?.length ? (
              <ReactApexChart
                type="area"
                height={240}
                series={[{ name: 'New Patients', data: data.patientGrowth.map(p => p.value) }]}
                options={{
                  ...baseChartOptions,
                  xaxis: { categories: data.patientGrowth.map(p => p.label) },
                  colors: ['#52c41a'],
                  fill: { type: 'gradient', gradient: { shadeIntensity: 1, opacityFrom: 0.4, opacityTo: 0.05 } },
                  stroke: { curve: 'smooth', width: 2 },
                }}
              />
            ) : <EmptyChart height={240} />}
          </Card>
        </Col>

        {/* Section 5: Top Doctors */}
        <Col xs={24} lg={12}>
          <Card title="Top Performing Doctors">
            {data.topDoctors?.length ? (
              <ReactApexChart
                type="bar"
                height={240}
                series={[{ name: 'Revenue (₹)', data: data.topDoctors.map(p => p.value) }]}
                options={{
                  ...baseChartOptions,
                  plotOptions: { bar: { horizontal: true, borderRadius: 4 } },
                  xaxis: { categories: data.topDoctors.map(p => p.name), labels: { formatter: (v) => `₹${(Number(v)/1000).toFixed(0)}k` } },
                  colors: ['#722ed1'],
                }}
              />
            ) : <EmptyChart height={240} />}
          </Card>
        </Col>
      </Row>

      {/* Section 6: Department Performance */}
      <Row gutter={[16, 16]}>
        <Col xs={24}>
          <Card title="Department Performance — Revenue">
            {data.departmentRevenue?.length ? (
              <ReactApexChart
                type="bar"
                height={220}
                series={[{ name: 'Revenue (₹)', data: data.departmentRevenue.map(p => p.value) }]}
                options={{
                  ...baseChartOptions,
                  xaxis: { categories: data.departmentRevenue.map(p => p.name) },
                  colors: ['#13c2c2'],
                  plotOptions: { bar: { borderRadius: 4, columnWidth: '50%' } },
                  yaxis: { labels: { formatter: (v) => `₹${(v/1000).toFixed(0)}k` } },
                }}
              />
            ) : <EmptyChart height={220} />}
          </Card>
        </Col>
      </Row>
    </div>
  )
}
