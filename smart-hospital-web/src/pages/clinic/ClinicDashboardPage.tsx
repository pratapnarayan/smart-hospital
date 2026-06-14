import { Card, Row, Col, Statistic, Table, Tag } from 'antd'
import { HomeOutlined, TeamOutlined, ExperimentOutlined, CalendarOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import type { TableProps } from 'antd'
import { useHomeCollectionsByDate } from '@/hooks/useClinic'
import type { HomeCollection, CollectionStatus } from '@/types/clinic.types'

const today = dayjs().format('YYYY-MM-DD')

const STATUS_COLOR: Record<CollectionStatus, string> = {
  SCHEDULED: 'blue',
  EN_ROUTE: 'orange',
  COLLECTED: 'green',
  CANCELLED: 'default',
  FAILED: 'red',
}

export function ClinicDashboardPage() {
  const { data: homeCollData, isLoading: hcLoading } = useHomeCollectionsByDate(today)

  const pendingCollections = homeCollData?.collections.filter(
    c => c.status === 'SCHEDULED' || c.status === 'EN_ROUTE'
  ) ?? []

  const collectionColumns: TableProps<HomeCollection>['columns'] = [
    { title: 'Patient', dataIndex: 'patientName', key: 'patientName' },
    {
      title: 'Scheduled', dataIndex: 'scheduledAt', key: 'scheduledAt',
      render: (v: string) => dayjs(v).format('HH:mm'),
    },
    {
      title: 'Technician', dataIndex: 'technicianName', key: 'technicianName',
      render: (v: string | undefined) => v ?? '—',
    },
    {
      title: 'Status', dataIndex: 'status', key: 'status',
      render: (s: CollectionStatus) => <Tag color={STATUS_COLOR[s]}>{s.replace('_', ' ')}</Tag>,
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 24 }}>SmartClinic Dashboard</h2>

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="Home Collections Today"
              value={homeCollData?.total ?? 0}
              prefix={<HomeOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Pending Collections"
              value={pendingCollections.length}
              prefix={<TeamOutlined />}
              valueStyle={{ color: pendingCollections.length > 0 ? '#faad14' : undefined }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Collected Today"
              value={homeCollData?.byStatus['COLLECTED'] ?? 0}
              prefix={<ExperimentOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Today's Date"
              value={dayjs().format('DD MMM YYYY')}
              prefix={<CalendarOutlined />}
            />
          </Card>
        </Col>
      </Row>

      <Card title="Pending Home Collections">
        <Table
          columns={collectionColumns}
          dataSource={pendingCollections}
          rowKey="id"
          loading={hcLoading}
          pagination={false}
          size="small"
        />
      </Card>
    </div>
  )
}
