import ReactApexChart from 'react-apexcharts'
import { Card, Table, Tag } from 'antd'
import { TrophyOutlined, SolutionOutlined, CheckCircleOutlined } from '@ant-design/icons'
import { PageHeader } from '@/components/common'
import { KpiCard, EmptyChart, AnalyticsFilter, ExportToolbar } from '@/components/analytics'
import { chartConfigs, formatCurrency } from '@/theme/chartTheme'
import { useDoctorAnalytics } from '@/hooks/useAnalytics'
import { withDemoFallback, DEMO_DOCTORS } from '@/hooks/useDemoData'
import type { DoctorStatEntry, DoctorAnalytics } from '@/types'

export function DoctorAnalyticsPage() {
  const { data: raw, isLoading } = useDoctorAnalytics()
  const { data, isDemo } = withDemoFallback<DoctorAnalytics>(raw, DEMO_DOCTORS, isLoading)

  const columns = [
    { title: '#', key: 'rank', render: (_: any, __: any, i: number) => <Tag color={i === 0 ? 'gold' : i === 1 ? 'silver' : i === 2 ? 'orange' : 'default'}>{i + 1}</Tag>, width: 50 },
    { title: 'Doctor', dataIndex: 'doctorName', key: 'doctorName' },
    { title: 'Specialization', dataIndex: 'specialization', key: 'specialization', render: (v: string) => <Tag color="blue">{v}</Tag> },
    { title: 'Appointments', dataIndex: 'appointmentsCompleted', key: 'appts' },
    { title: 'Revenue', dataIndex: 'revenueGenerated', key: 'rev', render: (v: number) => formatCurrency(v) },
    { title: 'Utilization', dataIndex: 'utilizationPct', key: 'util', render: (v: number) => `${(v ?? 0).toFixed(1)}%` },
  ]

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4">
        <PageHeader title="Doctor Analytics" subtitle="Performance leaderboard and revenue breakdown" />
        <ExportToolbar section="doctors" isDemoData={isDemo} />
      </div>
      <AnalyticsFilter />

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <KpiCard title="Total Doctors" value={data.totalDoctors?.toString() ?? '0'} loading={isLoading} icon={<SolutionOutlined />} color="primary" />
        <KpiCard title="Active Doctors" value={data.activeDoctors?.toString() ?? '0'} loading={isLoading} icon={<CheckCircleOutlined />} color="success" />
      </div>

      <Card title={<span><TrophyOutlined style={{ color: '#faad14', marginRight: 8 }} />Doctor Leaderboard</span>} className="medical-card" styles={{ body: { padding: '24px' } }}>
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

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card title="Revenue by Doctor" className="medical-card" styles={{ body: { padding: '24px' } }}>
          {data.revenueByDoctor?.length ? (
            <ReactApexChart type="bar" height={240}
              series={[{ name: 'Revenue (₹)', data: data.revenueByDoctor.map(p => p.value) }]}
              options={{ ...chartConfigs.horizontalBar('#722ed1'), xaxis: { categories: data.revenueByDoctor.map(p => p.name) } }} />
          ) : <EmptyChart height={240} />}
        </Card>
        <Card title="Appointments by Doctor" className="medical-card" styles={{ body: { padding: '24px' } }}>
          {data.appointmentsByDoctor?.length ? (
            <ReactApexChart type="bar" height={240}
              series={[{ name: 'Appointments', data: data.appointmentsByDoctor.map(p => p.value) }]}
              options={{ ...chartConfigs.horizontalBar('#1677ff'), xaxis: { categories: data.appointmentsByDoctor.map(p => p.name) } }} />
          ) : <EmptyChart height={240} />}
        </Card>
      </div>
    </div>
  )
}
