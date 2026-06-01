import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Table, Tag, Button, Select, Card, Space } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { FileSearchOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { PageHeader } from '@/components/common'
import { useRadiologyOrders } from '@/hooks/useRadiology'
import type { RadiologyOrder, RadiologyOrderStatus } from '@/types'

const STATUS_COLOR: Record<RadiologyOrderStatus, string> = {
  PENDING:     'default',
  IN_PROGRESS: 'processing',
  COMPLETED:   'success',
  CANCELLED:   'error',
}

const PRIORITY_COLOR = { ROUTINE: 'default', URGENT: 'orange', STAT: 'red' } as const

export function RadiologyOrdersPage() {
  const navigate = useNavigate()
  const [page, setPage]     = useState(0)
  const [status, setStatus] = useState<RadiologyOrderStatus | undefined>(undefined)

  const { data, isLoading } = useRadiologyOrders(status, page)

  const columns: ColumnsType<RadiologyOrder> = [
    {
      title: 'Order No.',
      dataIndex: 'orderNumber',
      width: 150,
      render: (v: string, r: RadiologyOrder) => (
        <Button type="link" style={{ padding: 0 }} onClick={() => navigate(`/radiology/orders/${r.id}`)}>
          {v}
        </Button>
      ),
    },
    { title: 'Patient',     dataIndex: 'patientName' },
    {
      title: 'Studies',
      dataIndex: 'items',
      render: (items: RadiologyOrder['items']) =>
        items.map(i => (
          <Tag key={i.studyCode} style={{ margin: 2 }}>
            {i.studyCode} <span style={{ color: '#888', fontSize: 11 }}>({i.modalityName})</span>
          </Tag>
        )),
    },
    {
      title: 'Priority', dataIndex: 'priority', width: 100,
      render: (v: keyof typeof PRIORITY_COLOR) => <Tag color={PRIORITY_COLOR[v]}>{v}</Tag>,
    },
    {
      title: 'Status', dataIndex: 'status', width: 120,
      render: (v: RadiologyOrderStatus) => (
        <Tag color={STATUS_COLOR[v]}>{v.replace('_', ' ')}</Tag>
      ),
    },
    {
      title: 'Payment', dataIndex: 'paymentStatus', width: 90,
      render: (v: string) => (
        <Tag color={v === 'PAID' ? 'success' : v === 'PENDING' ? 'warning' : 'default'}>
          {v}
        </Tag>
      ),
    },
    {
      title: 'Net Amount', dataIndex: 'netAmount', width: 110, align: 'right',
      render: (v: number) => `₹${Number(v).toLocaleString('en-IN')}`,
    },
    {
      title: 'Created', dataIndex: 'createdAt', width: 130,
      render: (v: string) => dayjs(v).format('DD/MM/YY HH:mm'),
    },
  ]

  return (
    <>
      <PageHeader
        title="Radiology — Orders"
        subtitle="All imaging orders and reports"
        extra={
          <Button type="primary" icon={<FileSearchOutlined />}
            onClick={() => navigate('/radiology/dashboard')}>
            Dashboard
          </Button>
        }
      />

      <Card
        title={
          <Space>
            <FileSearchOutlined />
            <span>Orders</span>
            <Select
              allowClear
              placeholder="Filter by status"
              style={{ width: 200 }}
              onChange={v => { setStatus(v as RadiologyOrderStatus | undefined); setPage(0) }}
              options={[
                { value: 'PENDING',     label: 'Pending' },
                { value: 'IN_PROGRESS', label: 'In Progress' },
                { value: 'COMPLETED',   label: 'Completed' },
                { value: 'CANCELLED',   label: 'Cancelled' },
              ]}
            />
          </Space>
        }
      >
        <Table
          rowKey="id"
          size="small"
          columns={columns}
          dataSource={data?.content ?? []}
          loading={isLoading}
          pagination={{
            current: page + 1,
            pageSize: 20,
            total: data?.total ?? 0,
            onChange: p => setPage(p - 1),
          }}
          onRow={r => ({ onClick: () => navigate(`/radiology/orders/${r.id}`) })}
        />
      </Card>
    </>
  )
}
