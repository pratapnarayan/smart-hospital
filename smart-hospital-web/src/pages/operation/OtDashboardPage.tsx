import { useNavigate } from 'react-router-dom'
import { Card, Row, Col, Statistic, Table, Tag, Button, Progress } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { CalendarOutlined, PlayCircleOutlined, CheckCircleOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { PageHeader } from '@/components/common'
import { useOtDashboard } from '@/hooks/useOperation'
import type { OtSchedule, OtStatus, OtTheatreUtilization } from '@/types'

const STATUS_COLOR: Record<OtStatus, string> = {
  SCHEDULED: 'blue', IN_PROGRESS: 'orange', COMPLETED: 'success',
  POSTPONED: 'warning', CANCELLED: 'error',
}

const PRIORITY_COLOR = { ROUTINE: 'default', URGENT: 'orange', EMERGENCY: 'red' } as const

export function OtDashboardPage() {
  const navigate = useNavigate()
  const { data: dash, isLoading } = useOtDashboard()

  const totalToday = (dash?.todayScheduled ?? 0) + (dash?.todayInProgress ?? 0) + (dash?.todayCompleted ?? 0)
  const completedPct = totalToday > 0 ? Math.round(((dash?.todayCompleted ?? 0) / totalToday) * 100) : 0

  const scheduleColumns: ColumnsType<OtSchedule> = [
    {
      title: 'Time', dataIndex: 'scheduledStart', width: 80,
      render: (v: string) => dayjs(v).format('HH:mm'),
    },
    { title: 'Theatre', dataIndex: 'theatreName', width: 100 },
    {
      title: 'Patient', dataIndex: 'patientName',
      render: (v: string, r: OtSchedule) => (
        <Button type="link" style={{ padding: 0 }} onClick={() => navigate(`/operation/schedules/${r.id}`)}>
          {v}
        </Button>
      ),
    },
    { title: 'Procedure', dataIndex: 'procedureName', ellipsis: true },
    { title: 'Surgeon',   dataIndex: 'surgeonName', render: (v?: string) => v ?? '—' },
    {
      title: 'Priority', dataIndex: 'priority', width: 90,
      render: (v: keyof typeof PRIORITY_COLOR) => <Tag color={PRIORITY_COLOR[v]}>{v}</Tag>,
    },
    {
      title: 'Status', dataIndex: 'status', width: 120,
      render: (v: OtStatus) => <Tag color={STATUS_COLOR[v]}>{v.replace('_', ' ')}</Tag>,
    },
    {
      title: 'Est.', dataIndex: 'estimatedDurationMins', width: 70, align: 'right',
      render: (v: number) => `${v}m`,
    },
  ]

  const utilisationColumns: ColumnsType<OtTheatreUtilization> = [
    { title: 'Theatre', dataIndex: 'theatreName' },
    {
      title: 'Operations (this month)', dataIndex: 'operationsThisMonth', align: 'right',
      render: (v: number) => <strong>{v}</strong>,
    },
  ]

  return (
    <>
      <PageHeader title="Operation Theatre" subtitle="Today's OT schedule and utilization" />

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={12} md={5}>
          <Card loading={isLoading}>
            <Statistic title="Today — Scheduled" value={dash?.todayScheduled ?? 0}
              prefix={<CalendarOutlined />} valueStyle={{ color: '#1677ff' }} />
          </Card>
        </Col>
        <Col xs={12} md={5}>
          <Card loading={isLoading}>
            <Statistic title="Today — In Progress" value={dash?.todayInProgress ?? 0}
              prefix={<PlayCircleOutlined />} valueStyle={{ color: '#fa8c16' }} />
          </Card>
        </Col>
        <Col xs={12} md={5}>
          <Card loading={isLoading}>
            <Statistic title="Today — Completed" value={dash?.todayCompleted ?? 0}
              prefix={<CheckCircleOutlined />} valueStyle={{ color: '#52c41a' }} />
          </Card>
        </Col>
        <Col xs={12} md={5}>
          <Card loading={isLoading}>
            <Statistic title="Last 30 Days — Total" value={dash?.monthTotal ?? 0} />
          </Card>
        </Col>
        <Col xs={24} md={4}>
          <Card loading={isLoading} style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 12, color: '#888', marginBottom: 8 }}>Today's Progress</div>
            <Progress type="circle" percent={completedPct} size={64} />
          </Card>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col xs={24} xl={17}>
          <Card title={`Today's Schedule (${dayjs().format('DD MMM YYYY')})`} loading={isLoading}>
            <Table
              rowKey="id"
              size="small"
              columns={scheduleColumns}
              dataSource={dash?.todaySchedules ?? []}
              pagination={false}
              locale={{ emptyText: 'No operations scheduled today' }}
              onRow={r => ({ onClick: () => navigate(`/operation/schedules/${r.id}`) })}
            />
          </Card>
        </Col>
        <Col xs={24} xl={7}>
          <Card title="Theatre Utilization — Last 30 Days" loading={isLoading}>
            <Table
              rowKey="theatreName"
              size="small"
              columns={utilisationColumns}
              dataSource={dash?.theatreUtilization ?? []}
              pagination={false}
              locale={{ emptyText: 'No data yet' }}
            />
          </Card>
        </Col>
      </Row>
    </>
  )
}
