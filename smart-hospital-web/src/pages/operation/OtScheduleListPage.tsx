import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Table, Button, Tag, Select, DatePicker, Space, Card, Modal, Form,
  Input, InputNumber, TimePicker,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined } from '@ant-design/icons'
import dayjs, { type Dayjs } from 'dayjs'
import { PageHeader, PatientSearchSelect, StaffSearchSelect } from '@/components/common'
import { useOtSchedules, useOtTheatres, useScheduleOperation } from '@/hooks/useOperation'
import { useAuthStore } from '@/store/authStore'
import type { OtSchedule, OtStatus, ScheduleOperationPayload, OperationType, OtPriority } from '@/types'

const STATUS_COLOR: Record<OtStatus, string> = {
  SCHEDULED: 'blue', IN_PROGRESS: 'orange', COMPLETED: 'success',
  POSTPONED: 'warning', CANCELLED: 'error',
}

const PRIORITY_COLOR = { ROUTINE: 'default', URGENT: 'orange', EMERGENCY: 'red' } as const

export function OtScheduleListPage() {
  const navigate = useNavigate()
  const { hasPermission } = useAuthStore()
  const [date, setDate]         = useState<Dayjs>(dayjs())
  const [status, setStatus]     = useState<OtStatus | undefined>()
  const [theatreId, setTheatreId] = useState<string | undefined>()
  const [page, setPage]         = useState(0)
  const [newOpen, setNewOpen]   = useState(false)
  const [form] = Form.useForm()

  const params = {
    date: date.format('YYYY-MM-DD'),
    status,
    theatreId,
    page,
  }

  const { data, isLoading }               = useOtSchedules(params)
  const { data: theatres = [] }           = useOtTheatres()
  const { mutate: scheduleOp, isPending } = useScheduleOperation()

  const columns: ColumnsType<OtSchedule> = [
    {
      title: 'Schedule No.', dataIndex: 'scheduleNumber', width: 150,
      render: (v: string, r: OtSchedule) => (
        <Button type="link" style={{ padding: 0 }} onClick={() => navigate(`/operation/schedules/${r.id}`)}>
          {v}
        </Button>
      ),
    },
    { title: 'Time', dataIndex: 'scheduledStart', width: 80,
      render: (v: string) => dayjs(v).format('HH:mm') },
    { title: 'Theatre',   dataIndex: 'theatreName',   width: 100 },
    { title: 'Patient',   dataIndex: 'patientName' },
    { title: 'Procedure', dataIndex: 'procedureName', ellipsis: true },
    { title: 'Surgeon',   dataIndex: 'surgeonName',   render: (v?: string) => v ?? '—' },
    { title: 'Est. (min)', dataIndex: 'estimatedDurationMins', width: 95, align: 'right' },
    {
      title: 'Type', dataIndex: 'operationType', width: 100,
      render: (v: OperationType) => (
        <Tag color={v === 'EMERGENCY' ? 'red' : v === 'DIAGNOSTIC' ? 'cyan' : 'default'}>{v}</Tag>
      ),
    },
    {
      title: 'Priority', dataIndex: 'priority', width: 90,
      render: (v: keyof typeof PRIORITY_COLOR) => <Tag color={PRIORITY_COLOR[v]}>{v}</Tag>,
    },
    {
      title: 'Status', dataIndex: 'status', width: 120,
      render: (v: OtStatus) => <Tag color={STATUS_COLOR[v]}>{v.replace('_', ' ')}</Tag>,
    },
  ]

  const onFinish = (values: ScheduleOperationPayload & {
    scheduledDatePicker: Dayjs; scheduledTimePicker: Dayjs
  }) => {
    const scheduledDate  = values.scheduledDatePicker.format('YYYY-MM-DD')
    const scheduledStart = `${scheduledDate}T${values.scheduledTimePicker.format('HH:mm')}:00`
    scheduleOp(
      { ...values, scheduledDate, scheduledStart },
      { onSuccess: () => { form.resetFields(); setNewOpen(false) } }
    )
  }

  return (
    <>
      <PageHeader
        title="OT Schedules"
        subtitle="Operation theatre scheduling and management"
        extra={
          hasPermission('OPERATION.CREATE') && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setNewOpen(true)}>
              Schedule Operation
            </Button>
          )
        }
      />

      <Card
        title={
          <Space wrap>
            <DatePicker value={date} onChange={d => { if (d) { setDate(d); setPage(0) } }} />
            <Select allowClear placeholder="All statuses" style={{ width: 160 }}
              onChange={v => { setStatus(v); setPage(0) }}
              options={(['SCHEDULED','IN_PROGRESS','COMPLETED','POSTPONED','CANCELLED'] as OtStatus[])
                .map(v => ({ value: v, label: v.replace('_', ' ') }))} />
            <Select allowClear placeholder="All theatres" style={{ width: 160 }}
              onChange={v => { setTheatreId(v); setPage(0) }}
              options={theatres.map(t => ({ value: t.id, label: t.name }))} />
          </Space>
        }
      >
        <Table
          rowKey="id" size="small" columns={columns}
          dataSource={data?.content ?? []} loading={isLoading}
          pagination={{ current: page + 1, pageSize: 20, total: data?.total ?? 0,
            onChange: p => setPage(p - 1) }}
          onRow={r => ({ onClick: () => navigate(`/operation/schedules/${r.id}`) })}
        />
      </Card>

      {/* Schedule Operation Modal */}
      <Modal
        title="Schedule Operation"
        open={newOpen}
        onCancel={() => setNewOpen(false)}
        onOk={() => form.submit()}
        okText="Schedule"
        confirmLoading={isPending}
        width={640}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" onFinish={onFinish}>
          <Form.Item name="patientName" label="Patient Name" rules={[{ required: true }]}>
            <PatientSearchSelect placeholder="Search patient…" />
          </Form.Item>

          <Space style={{ width: '100%' }} size={12}>
            <Form.Item name="theatreId" label="Theatre" style={{ flex: 1 }} rules={[{ required: true }]}>
              <Select options={theatres.map(t => ({ value: t.id, label: `${t.theatreNumber} — ${t.name}` }))} />
            </Form.Item>
            <Form.Item name="scheduledDatePicker" label="Date" style={{ width: 160 }}
              rules={[{ required: true }]}>
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="scheduledTimePicker" label="Start Time" style={{ width: 130 }}
              rules={[{ required: true }]}>
              <TimePicker format="HH:mm" minuteStep={15} style={{ width: '100%' }} />
            </Form.Item>
          </Space>

          <Space style={{ width: '100%' }} size={12}>
            <Form.Item name="estimatedDurationMins" label="Est. Duration (min)"
              style={{ width: 160 }} initialValue={60}>
              <InputNumber min={15} step={15} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="operationType" label="Type" style={{ width: 130 }} initialValue="ELECTIVE">
              <Select options={[
                { value: 'ELECTIVE',    label: 'Elective' },
                { value: 'EMERGENCY',   label: 'Emergency' },
                { value: 'DIAGNOSTIC',  label: 'Diagnostic' },
              ]} />
            </Form.Item>
            <Form.Item name="priority" label="Priority" style={{ width: 120 }} initialValue="ROUTINE">
              <Select options={[
                { value: 'ROUTINE',   label: 'Routine' },
                { value: 'URGENT',    label: '⚡ Urgent' },
                { value: 'EMERGENCY', label: '🚨 Emergency' },
              ]} />
            </Form.Item>
          </Space>

          <Form.Item name="procedureName" label="Procedure Name" rules={[{ required: true }]}>
            <Input placeholder="e.g. Appendectomy, CABG, Total Knee Replacement" />
          </Form.Item>

          <Space style={{ width: '100%' }} size={12}>
            <Form.Item name="surgeonName" label="Lead Surgeon" style={{ flex: 1 }}>
              <StaffSearchSelect isDoctor placeholder="Search surgeon…" />
            </Form.Item>
            <Form.Item name="anesthetistName" label="Anesthetist" style={{ flex: 1 }}>
              <StaffSearchSelect isDoctor placeholder="Search anesthetist…" />
            </Form.Item>
          </Space>

          <Form.Item name="assistantNames" label="Assistants">
            <Input placeholder="Comma-separated assistant names" />
          </Form.Item>

          <Form.Item name="preOpDiagnosis" label="Pre-op Diagnosis">
            <Input.TextArea rows={2} />
          </Form.Item>

          <Space style={{ width: '100%' }} size={12}>
            <Form.Item name="bloodRequestNumber" label="Blood Request No. (if needed)" style={{ flex: 1 }}>
              <Input placeholder="BRQ-2026-00001" />
            </Form.Item>
          </Space>

          <Form.Item name="notes" label="Notes">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
