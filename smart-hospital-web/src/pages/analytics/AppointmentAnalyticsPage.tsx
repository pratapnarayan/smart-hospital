import ReactApexChart from 'react-apexcharts'
import { Row, Col, Card } from 'antd'
import { PageHeader } from '@/components/common'
import { KpiCard, EmptyChart, AnalyticsFilter, ExportToolbar, baseChartOptions, chartPalette } from '@/components/analytics'
import { useAppointmentAnalytics } from '@/hooks/useAnalytics'
import { withDemoFallback, DEMO_APPOINTMENTS } from '@/hooks/useDemoData'
import type { AppointmentAnalytics } from '@/types'

export function AppointmentAnalyticsPage() {
  const { data: raw, isLoading } = useAppointmentAnalytics()
  const { data, isDemo } = withDemoFallback<AppointmentAnalytics>(raw, DEMO_APPOINTMENTS, isLoading)

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
        <PageHeader title="Appointment Analytics" subtitle="Booking patterns, status distribution and peak hours" />
        <ExportToolbar section="appointments" isDemoData={isDemo} />
      </div>
      <AnalyticsFilter />

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {[
          { title: 'Total Appointments', value: data.totalAppointments?.toLocaleString() ?? '0' },
          { title: 'Completed', value: data.completed?.toLocaleString() ?? '0' },
          { title: 'Cancelled', value: data.cancelled?.toLocaleString() ?? '0' },
          { title: 'No Show', value: data.noShow?.toLocaleString() ?? '0' },
        ].map(k => <Col xs={24} sm={12} md={6} key={k.title}><KpiCard {...k} loading={isLoading} /></Col>)}
      </Row>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} lg={16}>
          <Card title="Daily Appointment Trend">
            {data.dailyTrend?.length ? (
              <ReactApexChart type="line" height={260}
                series={[{ name: 'Appointments', data: data.dailyTrend.map(p => p.value) }]}
                options={{ ...baseChartOptions, xaxis: { categories: data.dailyTrend.map(p => p.label), labels: { rotate: -45, style: { fontSize: '10px' } } }, colors: ['#1677ff'], stroke: { curve: 'smooth', width: 2 } }} />
            ) : <EmptyChart height={260} />}
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="Status Distribution">
            {data.statusDistribution?.length ? (
              <ReactApexChart type="donut" height={260}
                series={data.statusDistribution.map(p => p.value)}
                options={{ ...baseChartOptions, labels: data.statusDistribution.map(p => p.name), colors: ['#52c41a', '#ff4d4f', '#faad14', '#1677ff'], legend: { position: 'bottom' } }} />
            ) : <EmptyChart height={260} />}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="Appointments by Doctor (Top 10)">
            {data.byDoctor?.length ? (
              <ReactApexChart type="bar" height={240}
                series={[{ name: 'Appointments', data: data.byDoctor.map(p => p.value) }]}
                options={{ ...baseChartOptions, plotOptions: { bar: { horizontal: true, borderRadius: 4 } }, xaxis: { categories: data.byDoctor.map(p => p.name) }, colors: ['#722ed1'] }} />
            ) : <EmptyChart height={240} />}
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Appointments by Department">
            {data.byDepartment?.length ? (
              <ReactApexChart type="bar" height={240}
                series={[{ name: 'Appointments', data: data.byDepartment.map(p => p.value) }]}
                options={{ ...baseChartOptions, xaxis: { categories: data.byDepartment.map(p => p.name) }, colors: ['#13c2c2'], plotOptions: { bar: { borderRadius: 4 } } }} />
            ) : <EmptyChart height={240} />}
          </Card>
        </Col>
      </Row>
    </div>
  )
}
