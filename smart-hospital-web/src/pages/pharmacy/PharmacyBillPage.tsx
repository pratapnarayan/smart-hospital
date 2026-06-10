import { useState } from 'react'
import {
  Card, Form, Select, InputNumber, Button, Table, Divider, Typography,
  type TableProps
} from 'antd'
import { PlusOutlined, DeleteOutlined, PrinterOutlined, PlusCircleOutlined } from '@ant-design/icons'
import { useCreateBill } from '@/hooks/usePharmacy'
import { useMedicines, useStockSummary } from '@/hooks/usePharmacy'
import { usePatients } from '@/hooks/usePatients'
import { PageHeader } from '@/components/common/PageHeader'
import { formatCurrency, formatDate } from '@/utils'
import type { PharmacyBill, BillItem, BillCreateRequest } from '@/types'

// ── Printable Bill Receipt ────────────────────────────────────────────────────

const receiptItemColumns: TableProps<BillItem>['columns'] = [
  { title: '#', key: 'no', width: 40, render: (_: unknown, __: BillItem, i: number) => i + 1 },
  { title: 'Medicine', dataIndex: 'medicineName' },
  { title: 'Qty', dataIndex: 'quantity', align: 'right', width: 70 },
  { title: 'Unit Price', dataIndex: 'unitPrice', align: 'right', width: 110,
    render: (v: number) => formatCurrency(v) },
  { title: 'Amount', dataIndex: 'totalPrice', align: 'right', width: 110,
    render: (v: number) => formatCurrency(v) },
]

function BillReceipt({ bill, onNew }: { bill: PharmacyBill; onNew: () => void }) {
  return (
    <>
      {/* Print-only CSS — visibility approach so nested elements can override */}
      <style>{`
        @media print {
          body * { visibility: hidden; }
          #pharmacy-bill-receipt,
          #pharmacy-bill-receipt * { visibility: visible; }
          #pharmacy-bill-receipt {
            position: absolute;
            top: 0; left: 0;
            width: 100%;
            max-width: 100%;
            padding: 24px;
            box-shadow: none;
            border: none;
          }
          .no-print { display: none !important; }
        }
      `}</style>

      <div className="no-print" style={{ marginBottom: 16, display: 'flex', gap: 8 }}>
        <Button type="primary" icon={<PrinterOutlined />} onClick={() => window.print()}>
          Print Bill
        </Button>
        <Button icon={<PlusCircleOutlined />} onClick={onNew}>
          Create Another Bill
        </Button>
      </div>

      <Card id="pharmacy-bill-receipt" style={{ maxWidth: 720, margin: '0 auto' }}>
        {/* Header */}
        <div style={{ textAlign: 'center', borderBottom: '2px solid #1677ff', paddingBottom: 12, marginBottom: 16 }}>
          <Typography.Title level={3} style={{ margin: 0, color: '#1677ff' }}>
            🏥 SmartHospital
          </Typography.Title>
          <Typography.Text type="secondary">Pharmacy Invoice</Typography.Text>
        </div>

        {/* Bill meta */}
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
          <div>
            <div><Typography.Text strong>Bill No: </Typography.Text>{bill.billNumber}</div>
            <div><Typography.Text strong>Date: </Typography.Text>{formatDate(bill.createdAt)}</div>
            <div><Typography.Text strong>Payment: </Typography.Text>
              <Typography.Text>{bill.paymentMode}</Typography.Text>
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            {bill.patientName && (
              <>
                <div><Typography.Text strong>Patient: </Typography.Text>{bill.patientName}</div>
              </>
            )}
            <div>
              <Typography.Text
                style={{ fontSize: 12, background: bill.status === 'PAID' ? '#f6ffed' : '#fff7e6',
                  color: bill.status === 'PAID' ? '#52c41a' : '#fa8c16',
                  border: `1px solid ${bill.status === 'PAID' ? '#b7eb8f' : '#ffd591'}`,
                  borderRadius: 4, padding: '1px 8px' }}
              >
                {bill.status}
              </Typography.Text>
            </div>
          </div>
        </div>

        {/* Items table */}
        <Table
          rowKey="id"
          size="small"
          dataSource={bill.items}
          columns={receiptItemColumns}
          pagination={false}
          bordered
          summary={() => (
            <Table.Summary>
              <Table.Summary.Row>
                <Table.Summary.Cell index={0} colSpan={4} align="right">
                  <Typography.Text>Subtotal</Typography.Text>
                </Table.Summary.Cell>
                <Table.Summary.Cell index={4} align="right">
                  {formatCurrency(bill.totalAmount)}
                </Table.Summary.Cell>
              </Table.Summary.Row>
              {bill.discount > 0 && (
                <Table.Summary.Row>
                  <Table.Summary.Cell index={0} colSpan={4} align="right">
                    <Typography.Text>Discount</Typography.Text>
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={4} align="right">
                    <Typography.Text type="danger">− {formatCurrency(bill.discount)}</Typography.Text>
                  </Table.Summary.Cell>
                </Table.Summary.Row>
              )}
              <Table.Summary.Row>
                <Table.Summary.Cell index={0} colSpan={4} align="right">
                  <Typography.Text strong style={{ fontSize: 15 }}>Net Amount</Typography.Text>
                </Table.Summary.Cell>
                <Table.Summary.Cell index={4} align="right">
                  <Typography.Text strong style={{ fontSize: 15, color: '#1677ff' }}>
                    {formatCurrency(bill.netAmount)}
                  </Typography.Text>
                </Table.Summary.Cell>
              </Table.Summary.Row>
            </Table.Summary>
          )}
        />

        {/* Footer */}
        <div style={{ textAlign: 'center', marginTop: 24, paddingTop: 12,
          borderTop: '1px dashed #d9d9d9', color: '#999', fontSize: 12 }}>
          Thank you for choosing SmartHospital &nbsp;·&nbsp; This is a computer-generated invoice
        </div>
      </Card>
    </>
  )
}

// ── Bill Form ─────────────────────────────────────────────────────────────────

interface BillLineItem {
  batchId: string
  medicineName: string
  batchNumber: string
  quantity: number
  unitPrice: number
}

export function PharmacyBillPage() {
  const [form] = Form.useForm()
  const [items, setItems] = useState<BillLineItem[]>([])
  const [discount, setDiscount] = useState(0)
  const [createdBill, setCreatedBill] = useState<PharmacyBill | null>(null)

  const [patientQuery, setPatientQuery] = useState('')
  const [medQuery, setMedQuery] = useState('')
  const [selectedMedId, setSelectedMedId] = useState<string>()

  const { mutate: createBill, isPending } = useCreateBill()
  const { data: patients } = usePatients(patientQuery || undefined)
  const { data: medicines } = useMedicines(medQuery || undefined)
  const { data: stock } = useStockSummary(selectedMedId ?? '')

  const total = items.reduce((s, i) => s + i.quantity * i.unitPrice, 0)
  const net   = total - discount

  function addItem() {
    const batchId = form.getFieldValue('batchId')
    const qty     = form.getFieldValue('qty') ?? 1
    const batch   = stock?.batches.find((b) => b.id === batchId)
    if (!batch) return

    setItems((prev) => {
      const existing = prev.find((i) => i.batchId === batchId)
      if (existing) {
        return prev.map((i) => i.batchId === batchId ? { ...i, quantity: i.quantity + qty } : i)
      }
      return [...prev, {
        batchId: batch.id,
        medicineName: batch.medicineName,
        batchNumber: batch.batchNumber,
        quantity: qty,
        unitPrice: batch.salePrice,
      }]
    })
    form.resetFields(['batchId', 'qty'])
  }

  function removeItem(batchId: string) {
    setItems((prev) => prev.filter((i) => i.batchId !== batchId))
  }

  function handleSubmit() {
    const patientId = form.getFieldValue('patientId')
    const paymentMode = form.getFieldValue('paymentMode') ?? 'CASH'

    const payload: BillCreateRequest = {
      patientId: patientId || undefined,
      paymentMode,
      discount,
      items: items.map((i) => ({ batchId: i.batchId, quantity: i.quantity })),
    }

    createBill(payload, {
      onSuccess: (data) => {
        setCreatedBill(data)
        setItems([])
        setDiscount(0)
        form.resetFields()
      },
    })
  }

  const lineColumns: TableProps<BillLineItem>['columns'] = [
    { title: 'Medicine', dataIndex: 'medicineName' },
    { title: 'Batch', dataIndex: 'batchNumber' },
    { title: 'Qty', dataIndex: 'quantity', align: 'right' },
    { title: 'Unit Price', dataIndex: 'unitPrice', render: formatCurrency, align: 'right' },
    {
      title: 'Total', key: 'total',
      render: (_, r) => formatCurrency(r.quantity * r.unitPrice),
      align: 'right',
    },
    {
      title: '', key: 'del',
      render: (_, r) => (
        <Button danger size="small" icon={<DeleteOutlined />} onClick={() => removeItem(r.batchId)} />
      ),
    },
  ]

  if (createdBill) {
    return <BillReceipt bill={createdBill} onNew={() => setCreatedBill(null)} />
  }

  return (
    <div>
      <PageHeader
        title="New Pharmacy Bill"
        breadcrumbs={[{ title: 'Dashboard', href: '/dashboard' }, { title: 'Pharmacy Bill' }]}
      />

      <Card title="Bill Details" style={{ marginBottom: 16 }}>
        <Form form={form} layout="vertical">
          <Form.Item name="patientId" label="Patient (optional for OTC sale)">
            <Select
              showSearch allowClear filterOption={false}
              onSearch={setPatientQuery} placeholder="Search patient…"
              options={patients?.content.map((p) => ({
                value: p.id,
                label: `${p.firstName} ${p.lastName} — ${p.mobile ?? ''}`,
              }))}
            />
          </Form.Item>
          <Form.Item name="paymentMode" label="Payment Mode" initialValue="CASH">
            <Select options={[
              { label: 'Cash',    value: 'CASH' },
              { label: 'Card',    value: 'CARD' },
              { label: 'UPI',     value: 'UPI' },
              { label: 'Credit',  value: 'CREDIT' },
            ]} />
          </Form.Item>
        </Form>
      </Card>

      <Card title="Add Medicines" style={{ marginBottom: 16 }}>
        <Form form={form} layout="inline">
          <Form.Item name="medicineId" label="Medicine">
            <Select
              showSearch filterOption={false}
              onSearch={setMedQuery}
              onChange={setSelectedMedId}
              placeholder="Search medicine…"
              style={{ width: 260 }}
              options={medicines?.content.map((m) => ({
                value: m.id,
                label: `${m.name} (${m.availableStock} ${m.unit})`,
              }))}
            />
          </Form.Item>
          <Form.Item name="batchId" label="Batch">
            <Select
              placeholder="Select batch"
              style={{ width: 200 }}
              disabled={!selectedMedId}
              options={stock?.batches
                .filter((b) => !b.expired && b.quantity > 0)
                .map((b) => ({
                  value: b.id,
                  label: `${b.batchNumber} · exp ${b.expiryDate} · ₹${b.salePrice} · ${b.quantity} left`,
                }))}
            />
          </Form.Item>
          <Form.Item name="qty" label="Qty" initialValue={1}>
            <InputNumber min={1} style={{ width: 80 }} />
          </Form.Item>
          <Form.Item label=" ">
            <Button icon={<PlusOutlined />} onClick={addItem} type="dashed">
              Add
            </Button>
          </Form.Item>
        </Form>
      </Card>

      {items.length > 0 && (
        <Card title="Bill Items">
          <Table
            rowKey="batchId"
            size="small"
            dataSource={items}
            columns={lineColumns}
            pagination={false}
            summary={() => (
              <Table.Summary>
                <Table.Summary.Row>
                  <Table.Summary.Cell index={0} colSpan={4} align="right">
                    <Typography.Text type="secondary">Subtotal</Typography.Text>
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={4} align="right">{formatCurrency(total)}</Table.Summary.Cell>
                  <Table.Summary.Cell index={5} />
                </Table.Summary.Row>
                <Table.Summary.Row>
                  <Table.Summary.Cell index={0} colSpan={4} align="right">
                    Discount ₹
                    <InputNumber
                      min={0} max={total} value={discount}
                      onChange={(v) => setDiscount(v ?? 0)}
                      size="small" style={{ width: 80, marginLeft: 8 }}
                    />
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={4} align="right">
                    − {formatCurrency(discount)}
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={5} />
                </Table.Summary.Row>
                <Table.Summary.Row>
                  <Table.Summary.Cell index={0} colSpan={4} align="right">
                    <strong>Net Amount</strong>
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={4} align="right">
                    <strong style={{ color: '#1677ff', fontSize: 16 }}>{formatCurrency(net)}</strong>
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={5} />
                </Table.Summary.Row>
              </Table.Summary>
            )}
          />

          <Divider />

          <div className="flex justify-end">
            <Button
              type="primary" size="large"
              loading={isPending}
              disabled={items.length === 0}
              onClick={handleSubmit}
            >
              Generate Bill
            </Button>
          </div>
        </Card>
      )}
    </div>
  )
}
