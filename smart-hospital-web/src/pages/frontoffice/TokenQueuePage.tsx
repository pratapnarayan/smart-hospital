import { useState } from 'react'
import {
  Table, Tag, Button, Space, Card, Select, Modal, Form, Input, Row, Col,
  Badge, Statistic, Tooltip,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined, ThunderboltOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { PageHeader, PatientSearchSelect } from '@/components/common'
import { useOpdTokens, useIssueToken, useUpdateTokenStatus } from '@/hooks/useFrontOffice'
import { useAuthStore } from '@/store/authStore'
import type { OpdToken, TokenStatus, IssueTokenPayload } from '@/types'

const STATUS_COLOR: Record<TokenStatus, string> = {
  WAITING:     'default',
  IN_PROGRESS: 'processing',
  COMPLETED:   'success',
  SKIPPED:     'warning',
}

const DEPARTMENTS = ['General Medicine', 'Pediatrics', 'Orthopedics', 'Gynecology',
                     'Cardiology', 'ENT', 'Ophthalmology', 'Dermatology', 'Surgery']

export function TokenQueuePage() {
  const { hasPermission } = useAuthStore()
  const [department, setDepartment] = useState<string | undefined>(undefined)
  const [issueOpen, setIssueOpen] = useState(false)
  const [form] = Form.useForm<IssueTokenPayload>()

  const today = dayjs().format('YYYY-MM-DD')
  const { data: tokens = [], isLoading } = useOpdTokens(today, department)
  const { mutate: issueToken, isPending } = useIssueToken()
  const { mutate: updateStatus } = useUpdateTokenStatus()

  const canEdit = hasPermission('FRONTOFFICE.EDIT')

  const waiting     = tokens.filter(t => t.status === 'WAITING').length
  const inProgress  = tokens.filter(t => t.status === 'IN_PROGRESS').length
  const completed   = tokens.filter(t => t.status === 'COMPLETED').length

  const columns: ColumnsType<OpdToken> = [
    {
      title: 'Token',
      dataIndex: 'tokenNumber',
      width: 90,
      render: (v: string, r: OpdToken) => (
        <Space>
          {r.priority === 'URGENT' && <Tooltip title="Urgent"><ThunderboltOutlined style={{ color: '#ff4d4f' }} /></Tooltip>}
          <strong>{v}</strong>
        </Space>
      ),
    },
    { title: 'Patient',    dataIndex: 'patientName' },
    { title: 'Mobile',     dataIndex: 'patientMobile', render: (v?: string) => v ?? '—' },
    { title: 'Department', dataIndex: 'department' },
    { title: 'Doctor',     dataIndex: 'doctorName', render: (v?: string) => v ?? '—' },
    {
      title: 'Status',
      dataIndex: 'status',
      render: (v: TokenStatus) => <Badge status={STATUS_COLOR[v] as any} text={v.replace('_', ' ')} />,
    },
    canEdit ? {
      title: 'Actions',
      key: 'actions',
      render: (_: unknown, r: OpdToken) => (
        <Space size="small">
          {r.status === 'WAITING' && (
            <Button size="small" type="primary"
              onClick={() => updateStatus({ id: r.id, status: 'IN_PROGRESS' })}>
              Call In
            </Button>
          )}
          {r.status === 'IN_PROGRESS' && (
            <Button size="small" type="primary" ghost
              onClick={() => updateStatus({ id: r.id, status: 'COMPLETED' })}>
              Complete
            </Button>
          )}
          {(r.status === 'WAITING' || r.status === 'IN_PROGRESS') && (
            <Button size="small"
              onClick={() => updateStatus({ id: r.id, status: 'SKIPPED' })}>
              Skip
            </Button>
          )}
        </Space>
      ),
    } : {},
  ].filter(c => Object.keys(c).length > 0)

  const onIssue = (values: IssueTokenPayload) => {
    issueToken(values, { onSuccess: () => { form.resetFields(); setIssueOpen(false) } })
  }

  return (
    <>
      <PageHeader
        title="OPD Token Queue"
        subtitle={`Today — ${dayjs().format('DD MMM YYYY')}`}
        extra={
          hasPermission('FRONTOFFICE.CREATE') && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setIssueOpen(true)}>
              Issue Token
            </Button>
          )
        }
      />

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}><Card><Statistic title="Total Today"  value={tokens.length} /></Card></Col>
        <Col span={6}><Card><Statistic title="Waiting"      value={waiting}     valueStyle={{ color: '#8c8c8c' }} /></Card></Col>
        <Col span={6}><Card><Statistic title="In Progress"  value={inProgress}  valueStyle={{ color: '#1677ff' }} /></Card></Col>
        <Col span={6}><Card><Statistic title="Completed"    value={completed}   valueStyle={{ color: '#52c41a' }} /></Card></Col>
      </Row>

      <Card
        title={
          <Space>
            <span>Queue</span>
            <Select
              allowClear
              placeholder="Filter by department"
              style={{ width: 200 }}
              onChange={setDepartment}
              options={DEPARTMENTS.map(d => ({ value: d, label: d }))}
            />
          </Space>
        }
      >
        <Table
          rowKey="id"
          size="small"
          columns={columns}
          dataSource={tokens}
          loading={isLoading}
          pagination={false}
          rowClassName={(r) => r.priority === 'URGENT' ? 'ant-table-row-selected' : ''}
        />
      </Card>

      <Modal
        title="Issue OPD Token"
        open={issueOpen}
        onCancel={() => setIssueOpen(false)}
        onOk={() => form.submit()}
        okText="Issue Token"
        confirmLoading={isPending}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" onFinish={onIssue}>
          <Form.Item name="patientId" label="Patient" rules={[{ required: true, message: 'Select a patient' }]}>
            <PatientSearchSelect valueMode="id" placeholder="Search patient by name or mobile…" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={14}>
              <Form.Item name="department" label="Department" rules={[{ required: true }]}>
                <Select options={DEPARTMENTS.map(d => ({ value: d, label: d }))} placeholder="Select department" />
              </Form.Item>
            </Col>
            <Col span={10}>
              <Form.Item name="priority" label="Priority" initialValue="NORMAL">
                <Select options={[
                  { value: 'NORMAL', label: 'Normal' },
                  { value: 'URGENT', label: '⚡ Urgent' },
                ]} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="doctorName" label="Doctor (optional)">
            <Input placeholder="Doctor name" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
