import { useState } from 'react'
import { Table, Button, Tag, Select, DatePicker, Space, Card, Modal, Form, Input, InputNumber } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined } from '@ant-design/icons'
import dayjs, { type Dayjs } from 'dayjs'
import { PageHeader, PatientSearchSelect, StaffSearchSelect } from '@/components/common'
import { useIncomeEntries, useCreateIncome } from '@/hooks/useFinance'
import { useAuthStore } from '@/store/authStore'
import type { IncomeEntry, IncomeSourceType, PaymentMode, CreateIncomePayload } from '@/types'

const SOURCE_COLOR: Record<IncomeSourceType, string> = {
  OPD: 'blue', IPD: 'purple', PHARMACY: 'cyan',
  PATHOLOGY: 'orange', RADIOLOGY: 'geekblue', OTHER: 'default',
}

const PAYMENT_COLOR: Record<PaymentMode, string> = {
  CASH: 'green', CARD: 'blue', UPI: 'purple',
  CHEQUE: 'orange', NEFT: 'cyan', OTHER: 'default',
}

const fmt = (v: number) =>
  `₹${Number(v ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`

export function IncomeListPage() {
  const { hasPermission } = useAuthStore()
  const [range, setRange]         = useState<[Dayjs, Dayjs]>([dayjs().startOf('month'), dayjs()])
  const [source, setSource]       = useState<IncomeSourceType | undefined>()
  const [page, setPage]           = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [form] = Form.useForm<CreateIncomePayload>()

  const params = {
    from:       range[0].format('YYYY-MM-DD'),
    to:         range[1].format('YYYY-MM-DD'),
    sourceType: source,
    page,
  }

  const { data, isLoading }                  = useIncomeEntries(params)
  const { mutate: createIncome, isPending }  = useCreateIncome()

  const columns: ColumnsType<IncomeEntry> = [
    { title: 'Entry No.',    dataIndex: 'entryNumber', width: 140 },
    { title: 'Date',         dataIndex: 'entryDate',   width: 110 },
    {
      title: 'Source', dataIndex: 'sourceType', width: 110,
      render: (v: IncomeSourceType) => <Tag color={SOURCE_COLOR[v]}>{v}</Tag>,
    },
    { title: 'Patient',      dataIndex: 'patientName', render: (v?: string) => v ?? '—' },
    { title: 'Description',  dataIndex: 'description', ellipsis: true },
    {
      title: 'Mode', dataIndex: 'paymentMode', width: 90,
      render: (v: PaymentMode) => <Tag color={PAYMENT_COLOR[v]}>{v}</Tag>,
    },
    { title: 'Ref No.',      dataIndex: 'referenceNo', render: (v?: string) => v ?? '—', width: 110 },
    { title: 'Received By',  dataIndex: 'receivedBy',  render: (v?: string) => v ?? '—' },
    {
      title: 'Amount', dataIndex: 'amount', align: 'right', width: 120,
      render: fmt,
    },
  ]

  const onFinish = (values: CreateIncomePayload) => {
    createIncome(
      { ...values, entryDate: values.entryDate ? dayjs(values.entryDate as unknown as Dayjs).format('YYYY-MM-DD') : undefined },
      { onSuccess: () => { form.resetFields(); setModalOpen(false) } }
    )
  }

  return (
    <>
      <PageHeader
        title="Income Entries"
        subtitle="All revenue received by the hospital"
        extra={
          hasPermission('FINANCE.CREATE') && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
              New Income
            </Button>
          )
        }
      />

      <Card
        title={
          <Space wrap>
            <DatePicker.RangePicker
              value={range}
              onChange={v => { if (v?.[0] && v?.[1]) { setRange([v[0], v[1]]); setPage(0) } }}
            />
            <Select
              allowClear
              placeholder="All sources"
              style={{ width: 150 }}
              onChange={v => { setSource(v as IncomeSourceType | undefined); setPage(0) }}
              options={['OPD','IPD','PHARMACY','PATHOLOGY','RADIOLOGY','OTHER'].map(v => ({ value: v, label: v }))}
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
            current: page + 1, pageSize: 20, total: data?.total ?? 0,
            onChange: p => setPage(p - 1),
          }}
        />
      </Card>

      <Modal
        title="New Income Entry"
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => form.submit()}
        okText="Save"
        confirmLoading={isPending}
        width={560}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" onFinish={onFinish}>
          <Space style={{ width: '100%' }} size={12}>
            <Form.Item name="entryDate" label="Date" style={{ width: 160 }}>
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="sourceType" label="Source" style={{ width: 160 }}
              rules={[{ required: true }]} initialValue="OTHER">
              <Select options={['OPD','IPD','PHARMACY','PATHOLOGY','RADIOLOGY','OTHER'].map(v => ({ value: v, label: v }))} />
            </Form.Item>
            <Form.Item name="paymentMode" label="Payment Mode" style={{ width: 160 }}
              rules={[{ required: true }]} initialValue="CASH">
              <Select options={['CASH','CARD','UPI','CHEQUE','NEFT','OTHER'].map(v => ({ value: v, label: v }))} />
            </Form.Item>
          </Space>

          <Form.Item name="amount" label="Amount (₹)" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={0.01} precision={2} placeholder="0.00" />
          </Form.Item>

          <Form.Item name="description" label="Description" rules={[{ required: true }]}>
            <Input.TextArea rows={2} placeholder="Brief description of income" />
          </Form.Item>

          <Space style={{ width: '100%' }} size={12}>
            <Form.Item name="patientName" label="Patient Name" style={{ flex: 1 }}>
              <PatientSearchSelect placeholder="Search patient…" />
            </Form.Item>
            <Form.Item name="referenceNo" label="Ref / Receipt No." style={{ flex: 1 }}>
              <Input placeholder="Optional reference" />
            </Form.Item>
          </Space>

          <Form.Item name="receivedBy" label="Received By">
            <StaffSearchSelect placeholder="Search staff…" />
          </Form.Item>

          <Form.Item name="notes" label="Notes">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
