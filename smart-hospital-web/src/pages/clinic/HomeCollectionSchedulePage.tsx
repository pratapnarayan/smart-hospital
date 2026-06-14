import { useState } from 'react'
import { Row, Col, Card, DatePicker, Button, Table, Tag, Space, Statistic } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import type { TableProps } from 'antd'
import { useHomeCollectionsByDate } from '@/hooks/useClinic'
import { ScheduleHomeCollectionModal } from './ScheduleHomeCollectionModal'
import { HomeCollectionStatusModal } from './HomeCollectionStatusModal'
import type { HomeCollection, CollectionStatus } from '@/types/clinic.types'

const STATUS_COLOR: Record<CollectionStatus, string> = {
  SCHEDULED: 'blue',
  EN_ROUTE: 'orange',
  COLLECTED: 'green',
  CANCELLED: 'default',
  FAILED: 'red',
}

export function HomeCollectionSchedulePage() {
  const [date, setDate] = useState(dayjs().format('YYYY-MM-DD'))
  const [scheduleOpen, setScheduleOpen] = useState(false)
  const [selectedCollection, setSelectedCollection] = useState<HomeCollection | null>(null)

  const { data, isLoading } = useHomeCollectionsByDate(date)

  const columns: TableProps<HomeCollection>['columns'] = [
    { title: 'Patient', dataIndex: 'patientName', key: 'patientName' },
    { title: 'Phone', dataIndex: 'patientPhone', key: 'patientPhone' },
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
    {
      title: 'Action', key: 'action',
      render: (_: unknown, record: HomeCollection) => (
        <Button size="small" onClick={() => setSelectedCollection(record)}>Update Status</Button>
      ),
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <Row gutter={[16, 16]} align="middle" style={{ marginBottom: 16 }}>
        <Col flex="auto"><h2 style={{ margin: 0 }}>Home Collections</h2></Col>
        <Col>
          <Space>
            <DatePicker value={dayjs(date)} onChange={d => d && setDate(d.format('YYYY-MM-DD'))} />
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setScheduleOpen(true)}>
              Schedule Collection
            </Button>
          </Space>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        {(Object.keys(STATUS_COLOR) as CollectionStatus[]).map(status => (
          <Col key={status} span={4}>
            <Card size="small">
              <Statistic
                title={status.replace('_', ' ')}
                value={data?.byStatus[status] ?? 0}
                valueStyle={{ color: STATUS_COLOR[status] === 'default' ? undefined : STATUS_COLOR[status] }}
              />
            </Card>
          </Col>
        ))}
      </Row>

      <Card>
        <Table
          columns={columns}
          dataSource={data?.collections ?? []}
          rowKey="id"
          loading={isLoading}
          pagination={{ pageSize: 20 }}
        />
      </Card>

      <ScheduleHomeCollectionModal open={scheduleOpen} onClose={() => setScheduleOpen(false)} />
      <HomeCollectionStatusModal collection={selectedCollection} onClose={() => setSelectedCollection(null)} />
    </div>
  )
}
