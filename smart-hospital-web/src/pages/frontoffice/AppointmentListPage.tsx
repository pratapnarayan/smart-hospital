import { useState } from 'react'
import { Table, Tag, Button, Space, Card, DatePicker, Statistic, Row, Col, Popconfirm, Tabs, Badge } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { PageHeader } from '@/components/common'
import {
  useAppointments, useUpcomingAppointments, useFrontOfficeDashboard,
  useUpdateAppointment, useCancelAppointment,
} from '@/hooks/useFrontOffice'
import { useAuthStore } from '@/store/authStore'
import { formatDate } from '@/utils'
import type { Appointment, AppointmentStatus } from '@/types'
import { AppointmentFormModal } from './AppointmentFormModal'

const STATUS_COLOR: Record<AppointmentStatus, string> = {
  SCHEDULED:  'default',
  CONFIRMED:  'processing',
  CHECKED_IN: 'warning',
  COMPLETED:  'success',
  CANCELLED:  'error',
  NO_SHOW:    'default',
}

const NEXT_STATUS: Partial<Record<AppointmentStatus, AppointmentStatus>> = {
  SCHEDULED:  'CONFIRMED',
  CONFIRMED:  'CHECKED_IN',
  CHECKED_IN: 'COMPLETED',
}

// Per-row action buttons — each instance owns its own mutate with the correct ID.
function RowActions({ record }: { record: Appointment }) {
  const { mutate: updateApt, isPending: updating } = useUpdateAppointment(record.id)
  const { mutate: cancelApt, isPending: cancelling } = useCancelAppointment()
  const next = NEXT_STATUS[record.status]
  const closed = record.status === 'CANCELLED' || record.status === 'COMPLETED'
  return (
    <Space size="small">
      {next && (
        <Button size="small" type="primary" ghost loading={updating}
          onClick={() => updateApt({ status: next })}>
          → {next.replace('_', ' ')}
        </Button>
      )}
      {!closed && (
        <Popconfirm title="Cancel this appointment?" onConfirm={() => cancelApt(record.id)}>
          <Button size="small" danger loading={cancelling}>Cancel</Button>
        </Popconfirm>
      )}
    </Space>
  )
}

function buildColumns(withDate: boolean, canEdit: boolean): ColumnsType<Appointment> {
  const cols: ColumnsType<Appointment> = [
    ...(withDate ? [{ title: 'Date', dataIndex: 'appointmentDate', width: 110, render: formatDate } as ColumnsType<Appointment>[number]] : []),
    { title: 'Appointment No.', dataIndex: 'appointmentNumber', width: 160 },
    { title: 'Patient',         dataIndex: 'patientName' },
    { title: 'Mobile',          dataIndex: 'patientMobile',   render: (v?: string) => v ?? '—' },
    { title: 'Doctor',          dataIndex: 'doctorName',      render: (v?: string) => v ?? '—' },
    { title: 'Department',      dataIndex: 'department',      render: (v?: string) => v ?? '—' },
    { title: 'Time Slot',       dataIndex: 'timeSlot',        render: (v?: string) => v ?? '—' },
    { title: 'Type',            dataIndex: 'appointmentType',
      render: (v: string) => <Tag>{v.replace('_', ' ')}</Tag> },
    { title: 'Status',          dataIndex: 'status',
      render: (v: AppointmentStatus) => <Tag color={STATUS_COLOR[v]}>{v.replace('_', ' ')}</Tag> },
  ]
  if (canEdit) cols.push({ title: 'Actions', key: 'actions', render: (_, r) => <RowActions record={r} /> })
  return cols
}

export function AppointmentListPage() {
  const { hasPermission } = useAuthStore()
  const [selectedDate, setSelectedDate] = useState<string>(dayjs().format('YYYY-MM-DD'))
  const [bookOpen, setBookOpen] = useState(false)

  const { data: byDate,   isLoading: loadingDate }     = useAppointments(selectedDate)
  const { data: upcoming, isLoading: loadingUpcoming } = useUpcomingAppointments()
  const { data: dashboard } = useFrontOfficeDashboard()

  const canEdit    = hasPermission('FRONTOFFICE.EDIT')
  const upcomingCols = buildColumns(true,  canEdit)
  const byDateCols   = buildColumns(false, canEdit)

  return (
    <>
      <PageHeader
        title="Front Office — Appointments"
        subtitle="Schedule and manage patient appointments"
        extra={
          hasPermission('FRONTOFFICE.CREATE') && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setBookOpen(true)}>
              Book Appointment
            </Button>
          )
        }
      />

      {dashboard && (
        <Row gutter={16} style={{ marginBottom: 24 }}>
          <Col span={6}><Card><Statistic title="Today's Appointments" value={dashboard.todayAppointments} /></Card></Col>
          <Col span={6}><Card><Statistic title="Confirmed"  value={dashboard.confirmedAppointments}  valueStyle={{ color: '#1677ff' }} /></Card></Col>
          <Col span={6}><Card><Statistic title="Checked In" value={dashboard.checkedInAppointments}  valueStyle={{ color: '#faad14' }} /></Card></Col>
          <Col span={6}><Card><Statistic title="Upcoming (all)" value={upcoming?.total ?? 0}        valueStyle={{ color: '#52c41a' }} /></Card></Col>
        </Row>
      )}

      <Tabs
        defaultActiveKey="upcoming"
        items={[
          {
            key: 'upcoming',
            label: (
              <Badge count={upcoming?.total ?? 0} size="small" color="#52c41a" offset={[8, -2]}>
                <span style={{ paddingRight: 6 }}>Upcoming</span>
              </Badge>
            ),
            children: (
              <Card>
                <Table
                  rowKey="id"
                  size="small"
                  columns={upcomingCols}
                  dataSource={upcoming?.content ?? []}
                  loading={loadingUpcoming}
                  pagination={{ pageSize: 20, total: upcoming?.total ?? 0 }}
                />
              </Card>
            ),
          },
          {
            key: 'bydate',
            label: 'By Date',
            children: (
              <Card
                title={
                  <Space>
                    <span>Appointments for</span>
                    <DatePicker
                      defaultValue={dayjs()}
                      format="DD MMM YYYY"
                      onChange={(d) => setSelectedDate(d ? d.format('YYYY-MM-DD') : dayjs().format('YYYY-MM-DD'))}
                    />
                  </Space>
                }
              >
                <Table
                  rowKey="id"
                  size="small"
                  columns={byDateCols}
                  dataSource={byDate?.content ?? []}
                  loading={loadingDate}
                  pagination={{ pageSize: 20, total: byDate?.total ?? 0 }}
                />
              </Card>
            ),
          },
        ]}
      />

      <AppointmentFormModal open={bookOpen} onClose={() => setBookOpen(false)} />
    </>
  )
}
