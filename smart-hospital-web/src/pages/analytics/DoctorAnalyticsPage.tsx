import ReactApexChart from 'react-apexcharts'
import { Row, Col, Card, Table, Tag } from 'antd'
import { TrophyOutlined } from '@ant-design/icons'
import { PageHeader } from '@/components/common'
import { KpiCard, EmptyChart, AnalyticsFilter, ExportToolbar, baseChartOptions, chartPalette } from '@/components/analytics'
import { useDoctorAnalytics } from '@/hooks/useAnalytics'
import { withDemoFallback, DEMO_DOCTORS } from '@/hooks/useDemoData'
import type { DoctorStatEntry, DoctorAnalytics } from '@/types'

const fmt = (v: number) => `₹${Number(v ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`

export function DoctorAnalyticsPage() {
  const { data: raw, isLoading } = useDoctorAnalytics()
  const { data, isDemo } = withDemoFallback<DoctorAnalytics>(raw, DEMO_DOCTORS, isLoading)

  const columns = [
    { title: '#', key: 'rank', render: (_: any, __: any, i: number) => <Tag color={i === 0 ? 'gold' : i === 1 ? 'silver' : i === 2 ? 'orange' : 'default'}>{i + 1}</Tag>, width: 50 },
    { title: 'Doctor', dataIndex: 'doctorName', key: 'doctorName' },
    { title: 'Specialization', dataIndex: 'specialization', key: 'specialization', render: (v: string) => <Tag color="blue">{v}</Tag> },
    { title: 'Appointments', dataIndex: 'appointmentsCompleted', key: 'appts' },
    { title: 'Revenue', dataIndex: 'revenueGenerated', key: 'rev', render: (v: number) => fmt(v) },
    { title: 'Utilization', dataIndex: 'utilizationPct', key: 'util', render: (v: number) => `${v?.toFixed(1)}%` },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
        <PageHeader title="Doctor Analytics" subtitle="Performance leaderboard and revenue breakdown" />
        <ExportToolbar section="doctors" isDemoData={isDemo} />
      </div>
      <AnalyticsFilter />

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {[
          { title: 'Total Doctors', value: data.totalDoctors?.toString() ?? '0' },
          { title: 'Active Doctors', value: data.activeDoctors?.toString() ?? '0' },
        ].map(k => <Col xs={24} sm={12} md={6} key={k.title}><KpiCard {...k} loading={isLoading} /></Col>)}
      </Row>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24}>
          <Card title={<span><TrophyOutlined style={{ color: '#faad14', marginRight: 8 }} />Doctor Leaderboard</span>}>
            <Table
              dataSource={data.leaderboard ?? []}
              columns={columns}
              rowKey="doctorName"
              size="small"
              pagination={false}
              loading={isLoading}
              locale={{ emptyText: 'No doctor data for selected period' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="Revenue by Doctor">
            {data.revenueByDoctor?.length ? (
              <ReactApexChart type="bar" height={240}
                series={[{ name: 'Revenue (₹)', data: data.revenueByDoctor.map(p => p.value) }]}
                options={{ ...baseChartOptions, plotOptions: { bar: { horizontal: true, borderRadius: 4 } }, xaxis: { categories: data.revenueByDoctor.map(p => p.name) }, colors: ['#722ed1'] }} />
            ) : <EmptyChart height={240} />}
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Appointments by Doctor">
            {data.appointmentsByDoctor?.length ? (
              <ReactApexChart type="bar" height={240}
                series={[{ name: 'Appointments', data: data.appointmentsByDoctor.map(p => p.value) }]}
                options={{ ...baseChartOptions, plotOptions: { bar: { horizontal: true, borderRadius: 4 } }, xaxis: { categories: data.appointmentsByDoctor.map(p => p.name) }, colors: ['#1677ff'] }} />
            ) : <EmptyChart height={240} />}
          </Card>
        </Col>
      </Row>
    </div>
  )
}
