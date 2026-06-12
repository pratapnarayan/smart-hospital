import { useState } from 'react'
import {
  Table, Button, Tag, Select, Space, Card, Modal, Form, Input, InputNumber,
  Badge, Descriptions,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { PageHeader, PatientSearchSelect, StaffSearchSelect } from '@/components/common'
import {
  useBloodRequests, useCreateBloodRequest, useCancelRequest,
  useAvailableUnits, useIssueBlood, useRequestIssues,
} from '@/hooks/useBloodBank'
import { useAuthStore } from '@/store/authStore'
import type {
  BloodRequest, BloodGroup, ComponentType, RequestStatus,
  CreateBloodRequestPayload, BloodUnit,
} from '@/types'
import {
  BLOOD_GROUP_OPTIONS, COMPONENT_OPTIONS, BLOOD_GROUP_LABELS, COMPONENT_LABELS,
} from '@/types'

const URGENCY_COLOR = { ROUTINE: 'default', URGENT: 'orange', EMERGENCY: 'red' } as const
const STATUS_COLOR: Record<RequestStatus, string> = {
  PENDING: 'default', PARTIALLY_FULFILLED: 'processing',
  FULFILLED: 'success', CANCELLED: 'error',
}

export function BloodRequestListPage() {
  const { hasPermission } = useAuthStore()
  const [status, setStatus]           = useState<RequestStatus | undefined>()
  const [page, setPage]               = useState(0)
  const [newOpen, setNewOpen]         = useState(false)
  const [issueOpen, setIssueOpen]     = useState(false)
  const [detailOpen, setDetailOpen]   = useState(false)
  const [activeRequest, setActiveRequest] = useState<BloodRequest | null>(null)
  const [selectedUnit, setSelectedUnit]   = useState<string | undefined>()
  const [issuedBy, setIssuedBy]           = useState('')

  const [form] = Form.useForm<CreateBloodRequestPayload>()

  const { data, isLoading }          = useBloodRequests({ status, page })
  const { mutate: createReq, isPending: creating }  = useCreateBloodRequest()
  const { mutate: cancelReq, isPending: cancelling } = useCancelRequest(activeRequest?.id ?? '')
  const { mutate: issueBlood, isPending: issuing }   = useIssueBlood()

  const { data: availableUnits = [] } = useAvailableUnits(
    activeRequest?.bloodGroup, activeRequest?.componentType)
  const { data: reqIssues = [] } = useRequestIssues(activeRequest?.id ?? '')

  const canEdit = hasPermission('BLOODBANK.EDIT')

  const openIssue = (r: BloodRequest) => {
    setActiveRequest(r); setSelectedUnit(undefined); setIssueOpen(true)
  }
  const openDetail = (r: BloodRequest) => {
    setActiveRequest(r); setDetailOpen(true)
  }

  const columns: ColumnsType<BloodRequest> = [
    {
      title: 'Request No.', dataIndex: 'requestNumber', width: 150,
      render: (v: string, r: BloodRequest) => (
        <Button type="link" style={{ padding: 0 }} onClick={() => openDetail(r)}>{v}</Button>
      ),
    },
    { title: 'Date',    dataIndex: 'requestDate', width: 110 },
    { title: 'Patient', dataIndex: 'patientName' },
    {
      title: 'Blood Group', dataIndex: 'bloodGroupDisplay', width: 100,
      render: (v: string) => <Tag style={{ fontWeight: 700 }}>{v}</Tag>,
    },
    {
      title: 'Component', dataIndex: 'componentType', width: 140,
      render: (v: ComponentType) => COMPONENT_LABELS[v],
    },
    {
      title: 'Units', key: 'units', width: 80, align: 'center' as const,
      render: (_: unknown, r: BloodRequest) => (
        <Badge
          count={`${r.unitsIssued}/${r.unitsRequired}`}
          color={r.unitsIssued >= r.unitsRequired ? '#52c41a' : '#1677ff'}
          style={{ fontSize: 12 }}
        />
      ),
    },
    {
      title: 'Urgency', dataIndex: 'urgency', width: 100,
      render: (v: keyof typeof URGENCY_COLOR) => <Tag color={URGENCY_COLOR[v]}>{v}</Tag>,
    },
    {
      title: 'Status', dataIndex: 'status', width: 150,
      render: (v: RequestStatus) => (
        <Tag color={STATUS_COLOR[v]}>{v.replace('_', ' ')}</Tag>
      ),
    },
    { title: 'Requested By', dataIndex: 'requestedBy', render: (v?: string) => v ?? '—' },
    canEdit ? {
      title: 'Actions', key: 'actions', width: 140,
      render: (_: unknown, r: BloodRequest) => (
        <Space size={4}>
          {(r.status === 'PENDING' || r.status === 'PARTIALLY_FULFILLED') && (
            <Button size="small" type="primary" onClick={() => openIssue(r)}>
              Issue Unit
            </Button>
          )}
          {r.status === 'PENDING' && (
            <Button size="small" danger onClick={() => {
              setActiveRequest(r)
              Modal.confirm({
                title: 'Cancel Request',
                content: `Cancel request ${r.requestNumber}?`,
                okType: 'danger', okText: 'Cancel Request',
                onOk: () => cancelReq(undefined, { onSuccess: () => setActiveRequest(null) }),
              })
            }}>
              Cancel
            </Button>
          )}
        </Space>
      ),
    } : {},
  ].filter(c => Object.keys(c).length > 0)

  const onNewFinish = (values: CreateBloodRequestPayload) => {
    createReq(values, { onSuccess: () => { form.resetFields(); setNewOpen(false) } })
  }

  return (
    <>
      <PageHeader
        title="Blood Requests"
        subtitle="Patient blood requests and issue management"
        extra={
          hasPermission('BLOODBANK.CREATE') && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setNewOpen(true)}>
              New Request
            </Button>
          )
        }
      />

      <Card
        title={
          <Select allowClear placeholder="All statuses" style={{ width: 200 }}
            onChange={v => { setStatus(v); setPage(0) }}
            options={(['PENDING','PARTIALLY_FULFILLED','FULFILLED','CANCELLED'] as RequestStatus[])
              .map(v => ({ value: v, label: v.replace('_', ' ') }))} />
        }
      >
        <Table rowKey="id" size="small" columns={columns}
          dataSource={data?.content ?? []} loading={isLoading}
          pagination={{ current: page + 1, pageSize: 20, total: data?.total ?? 0,
            onChange: p => setPage(p - 1) }} />
      </Card>

      {/* New Request Modal */}
      <Modal title="New Blood Request" open={newOpen} onCancel={() => setNewOpen(false)}
        onOk={() => form.submit()} okText="Create Request" confirmLoading={creating}
        width={540} destroyOnHidden>
        <Form form={form} layout="vertical" onFinish={onNewFinish}>
          <Form.Item name="patientName" label="Patient Name" rules={[{ required: true }]}>
            <PatientSearchSelect placeholder="Search patient…" />
          </Form.Item>
          <Space style={{ width: '100%' }} size={12}>
            <Form.Item name="bloodGroup" label="Blood Group" style={{ width: 130 }}
              rules={[{ required: true }]}>
              <Select options={BLOOD_GROUP_OPTIONS} />
            </Form.Item>
            <Form.Item name="componentType" label="Component" style={{ flex: 1 }}
              rules={[{ required: true }]}>
              <Select options={COMPONENT_OPTIONS} />
            </Form.Item>
            <Form.Item name="unitsRequired" label="Units" style={{ width: 80 }}
              initialValue={1} rules={[{ required: true }]}>
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Space style={{ width: '100%' }} size={12}>
            <Form.Item name="urgency" label="Urgency" style={{ width: 140 }} initialValue="ROUTINE">
              <Select options={[
                { value: 'ROUTINE',   label: 'Routine' },
                { value: 'URGENT',    label: '⚡ Urgent' },
                { value: 'EMERGENCY', label: '🚨 Emergency' },
              ]} />
            </Form.Item>
            <Form.Item name="requestedBy" label="Requested By (Doctor)" style={{ flex: 1 }}>
              <StaffSearchSelect isDoctor placeholder="Search doctor…" />
            </Form.Item>
          </Space>
          <Form.Item name="notes" label="Clinical Notes">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Issue Blood Modal */}
      <Modal
        title={`Issue Blood — ${activeRequest?.requestNumber}`}
        open={issueOpen}
        onCancel={() => setIssueOpen(false)}
        onOk={() => {
          if (!selectedUnit || !activeRequest) return
          issueBlood(
            { requestId: activeRequest.id, unitId: selectedUnit, issuedBy },
            { onSuccess: () => { setIssueOpen(false); setSelectedUnit(undefined) } }
          )
        }}
        okText="Confirm Issue"
        okButtonProps={{ disabled: !selectedUnit }}
        confirmLoading={issuing}
        width={560}
        destroyOnHidden
      >
        {activeRequest && (
          <>
            <Descriptions size="small" column={3} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Patient">{activeRequest.patientName}</Descriptions.Item>
              <Descriptions.Item label="Group">
                <strong>{activeRequest.bloodGroupDisplay}</strong>
              </Descriptions.Item>
              <Descriptions.Item label="Component">
                {COMPONENT_LABELS[activeRequest.componentType]}
              </Descriptions.Item>
              <Descriptions.Item label="Still needed">
                {activeRequest.unitsRequired - activeRequest.unitsIssued} unit(s)
              </Descriptions.Item>
            </Descriptions>

            <Select
              placeholder="Select available unit (FEFO order)"
              style={{ width: '100%', marginBottom: 12 }}
              value={selectedUnit}
              onChange={setSelectedUnit}
              options={availableUnits.map((u: BloodUnit) => ({
                value: u.id,
                label: `${u.unitNumber} — ${u.bloodGroupDisplay} ${COMPONENT_LABELS[u.componentType]} — expires ${u.expiryDate} (${u.volumeMl}ml)`,
              }))}
              notFoundContent="No available units for this blood group and component"
            />

            <StaffSearchSelect
              placeholder="Search staff…"
              value={issuedBy}
              onChange={v => setIssuedBy(v)}
            />
          </>
        )}
      </Modal>

      {/* Request Detail Modal */}
      <Modal
        title={`Request — ${activeRequest?.requestNumber}`}
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        footer={null}
        width={600}
      >
        {activeRequest && (
          <>
            <Descriptions bordered size="small" column={2} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Patient"    span={2}>{activeRequest.patientName}</Descriptions.Item>
              <Descriptions.Item label="Blood Group"><strong>{activeRequest.bloodGroupDisplay}</strong></Descriptions.Item>
              <Descriptions.Item label="Component">{COMPONENT_LABELS[activeRequest.componentType]}</Descriptions.Item>
              <Descriptions.Item label="Units Required">{activeRequest.unitsRequired}</Descriptions.Item>
              <Descriptions.Item label="Units Issued">{activeRequest.unitsIssued}</Descriptions.Item>
              <Descriptions.Item label="Urgency">
                <Tag color={URGENCY_COLOR[activeRequest.urgency]}>{activeRequest.urgency}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={STATUS_COLOR[activeRequest.status]}>{activeRequest.status.replace('_', ' ')}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Requested By" span={2}>{activeRequest.requestedBy ?? '—'}</Descriptions.Item>
              {activeRequest.requiredBy && (
                <Descriptions.Item label="Required By" span={2}>
                  {dayjs(activeRequest.requiredBy).format('DD MMM YYYY HH:mm')}
                </Descriptions.Item>
              )}
              {activeRequest.notes && (
                <Descriptions.Item label="Notes" span={2}>{activeRequest.notes}</Descriptions.Item>
              )}
            </Descriptions>

            {reqIssues.length > 0 && (
              <>
                <strong>Issued Units:</strong>
                <Table
                  rowKey="id" size="small" style={{ marginTop: 8 }} pagination={false}
                  dataSource={reqIssues}
                  columns={[
                    { title: 'Issue No.',  dataIndex: 'issueNumber', width: 140 },
                    { title: 'Unit No.',   dataIndex: 'unitNumber',  width: 140 },
                    { title: 'Blood Group', dataIndex: 'bloodGroup',
                      render: (v: string) => BLOOD_GROUP_LABELS[v as BloodGroup] ?? v },
                    { title: 'Issued By',  dataIndex: 'issuedBy', render: (v?: string) => v ?? '—' },
                    { title: 'Date',       dataIndex: 'issueDate',
                      render: (v: string) => dayjs(v).format('DD MMM, HH:mm') },
                  ]}
                />
              </>
            )}
          </>
        )}
      </Modal>
    </>
  )
}
