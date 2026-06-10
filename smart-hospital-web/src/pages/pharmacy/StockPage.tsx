import { useState } from 'react'
import {
  Table, Input, Tag, Button, Alert, Tabs, Badge, Modal, Form,
  InputNumber, DatePicker, Space, type TableProps,
} from 'antd'
import { PlusOutlined, SearchOutlined, WarningOutlined, MedicineBoxOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { useMedicines, useLowStockMedicines, useExpiringBatches, useAddBatch } from '@/hooks/usePharmacy'
import { useAuthStore } from '@/store/authStore'
import { PageHeader } from '@/components/common/PageHeader'
import { formatDate, formatCurrency } from '@/utils'
import type { Medicine, MedicineBatch } from '@/types'

// ── Add Batch Modal ───────────────────────────────────────────────────────────

function AddBatchModal({ medicine, onClose }: { medicine: Medicine | null; onClose: () => void }) {
  const [form] = Form.useForm()
  const { mutate: addBatch, isPending } = useAddBatch()

  function handleOk() {
    form.validateFields().then((values) => {
      addBatch(
        {
          medicineId: medicine!.id,
          batchNumber: values.batchNumber,
          expiryDate: (values.expiryDate as dayjs.Dayjs).format('YYYY-MM-DD'),
          quantity: values.quantity,
          purchasePrice: values.purchasePrice,
          salePrice: values.salePrice,
        },
        {
          onSuccess: () => {
            form.resetFields()
            onClose()
          },
        },
      )
    })
  }

  return (
    <Modal
      open={!!medicine}
      title={
        <Space>
          <MedicineBoxOutlined style={{ color: '#1677ff' }} />
          {`Add Stock Batch — ${medicine?.name ?? ''}`}
        </Space>
      }
      onCancel={onClose}
      onOk={handleOk}
      okText="Add Batch"
      confirmLoading={isPending}
      destroyOnClose
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item
          name="batchNumber"
          label="Batch Number"
          rules={[{ required: true, message: 'Enter a batch number' }]}
        >
          <Input placeholder="e.g. BATCH-2025-001" />
        </Form.Item>

        <Form.Item
          name="expiryDate"
          label="Expiry Date"
          rules={[
            { required: true, message: 'Select expiry date' },
            {
              validator: (_, v) =>
                v && !(v as dayjs.Dayjs).isBefore(dayjs(), 'day')
                  ? Promise.resolve()
                  : Promise.reject(new Error('Expiry date must be today or in the future')),
            },
          ]}
        >
          <DatePicker
            style={{ width: '100%' }}
            disabledDate={(d) => d.isBefore(dayjs(), 'day')}
            format="DD/MM/YYYY"
          />
        </Form.Item>

        <Form.Item
          name="quantity"
          label="Quantity"
          rules={[{ required: true, message: 'Enter quantity' }]}
        >
          <InputNumber min={1} style={{ width: '100%' }} addonAfter={medicine?.unit ?? ''} />
        </Form.Item>

        <Form.Item
          name="purchasePrice"
          label="Purchase Price (₹)"
          rules={[{ required: true, message: 'Enter purchase price' }]}
        >
          <InputNumber min={0.01} step={0.01} precision={2} style={{ width: '100%' }} prefix="₹" />
        </Form.Item>

        <Form.Item
          name="salePrice"
          label="Sale Price (₹)"
          rules={[{ required: true, message: 'Enter sale price' }]}
        >
          <InputNumber min={0.01} step={0.01} precision={2} style={{ width: '100%' }} prefix="₹" />
        </Form.Item>
      </Form>
    </Modal>
  )
}

// ── Stock Page ────────────────────────────────────────────────────────────────

export function StockPage() {
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [batchMedicine, setBatchMedicine] = useState<Medicine | null>(null)

  const { hasPermission } = useAuthStore()
  const canCreate = hasPermission('PHARMACY.CREATE')

  const { data: medicines, isLoading } = useMedicines(query || undefined, page)
  const { data: lowStock } = useLowStockMedicines()
  const { data: expiring } = useExpiringBatches(30)

  const medicineColumns: TableProps<Medicine>['columns'] = [
    { title: 'Name', dataIndex: 'name' },
    { title: 'Generic', dataIndex: 'genericName', render: (v) => v ?? '—' },
    { title: 'Category', dataIndex: 'categoryName' },
    { title: 'Unit', dataIndex: 'unit' },
    {
      title: 'Stock', dataIndex: 'availableStock',
      render: (v, r) => (
        <Tag color={v <= r.reorderLevel ? 'red' : 'green'}>
          {v <= r.reorderLevel && <WarningOutlined />} {v} {r.unit}
        </Tag>
      ),
    },
    { title: 'Reorder @', dataIndex: 'reorderLevel' },
    ...(canCreate
      ? [
          {
            title: 'Action',
            key: 'action',
            render: (_: unknown, r: Medicine) => (
              <Button
                size="small"
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => setBatchMedicine(r)}
              >
                Add Batch
              </Button>
            ),
          },
        ]
      : []),
  ]

  const batchColumns: TableProps<MedicineBatch>['columns'] = [
    { title: 'Medicine', dataIndex: 'medicineName' },
    { title: 'Batch #', dataIndex: 'batchNumber' },
    { title: 'Expiry', dataIndex: 'expiryDate', render: formatDate },
    { title: 'Qty', dataIndex: 'quantity' },
    { title: 'Sale Price', dataIndex: 'salePrice', render: formatCurrency },
    {
      title: 'Status', key: 'status',
      render: (_, r) =>
        r.expired
          ? <Tag color="red">EXPIRED</Tag>
          : r.lowStock
            ? <Tag color="orange">LOW STOCK</Tag>
            : <Tag color="green">OK</Tag>,
    },
  ]

  const lowStockColumns: TableProps<Medicine>['columns'] = [
    { title: 'Medicine', dataIndex: 'name' },
    { title: 'Category', dataIndex: 'categoryName' },
    {
      title: 'Available',
      dataIndex: 'availableStock',
      render: (v, r) => (
        <Tag color="red">
          <WarningOutlined /> {v} {r.unit}
        </Tag>
      ),
    },
    { title: 'Reorder @', dataIndex: 'reorderLevel' },
    ...(canCreate
      ? [
          {
            title: 'Action',
            key: 'action',
            render: (_: unknown, r: Medicine) => (
              <Button
                size="small"
                type="primary"
                danger
                icon={<PlusOutlined />}
                onClick={() => setBatchMedicine(r)}
              >
                Restock
              </Button>
            ),
          },
        ]
      : []),
  ]

  return (
    <div>
      <PageHeader
        title="Pharmacy Stock"
        subtitle="Medicine catalogue and batch management"
        breadcrumbs={[{ title: 'Dashboard', href: '/dashboard' }, { title: 'Pharmacy Stock' }]}
      />

      {lowStock && lowStock.length > 0 && (
        <Alert
          type="error" showIcon className="mb-4"
          message={`${lowStock.length} medicine(s) need restocking`}
        />
      )}
      {expiring && expiring.length > 0 && (
        <Alert
          type="warning" showIcon className="mb-4"
          message={`${expiring.length} batch(es) expiring within 30 days`}
        />
      )}

      <Tabs defaultActiveKey="catalogue" items={[
        {
          key: 'catalogue',
          label: 'Catalogue',
          children: (
            <>
              <Input.Search
                placeholder="Search medicines…"
                allowClear
                enterButton={<SearchOutlined />}
                style={{ maxWidth: 380, marginBottom: 16 }}
                onSearch={setQuery}
                onChange={(e) => !e.target.value && setQuery('')}
              />
              <Table
                rowKey="id"
                dataSource={medicines?.content}
                columns={medicineColumns}
                loading={isLoading}
                pagination={{
                  current: (medicines?.page ?? 0) + 1,
                  pageSize: medicines?.size ?? 20,
                  total: medicines?.total ?? 0,
                  onChange: (p) => setPage(p - 1),
                }}
              />
            </>
          ),
        },
        {
          key: 'expiring',
          label: (
            <Badge count={expiring?.length ?? 0} offset={[8, 0]}>
              Expiring Soon
            </Badge>
          ),
          children: (
            <Table
              rowKey="id"
              size="small"
              dataSource={expiring}
              columns={batchColumns}
              pagination={false}
            />
          ),
        },
        {
          key: 'lowstock',
          label: (
            <Badge count={lowStock?.length ?? 0} offset={[8, 0]}>
              Low Stock
            </Badge>
          ),
          children: (
            <Table
              rowKey="id"
              size="small"
              dataSource={lowStock}
              columns={lowStockColumns}
              pagination={false}
            />
          ),
        },
      ]} />

      <AddBatchModal medicine={batchMedicine} onClose={() => setBatchMedicine(null)} />
    </div>
  )
}
