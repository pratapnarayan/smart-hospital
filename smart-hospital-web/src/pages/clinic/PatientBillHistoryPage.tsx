import { useParams } from 'react-router-dom'
import { Card, Table, Tag } from 'antd'
import type { TableProps } from 'antd'
import { usePatientBills } from '@/hooks/useClinic'
import type { ClinicBill, ClinicBillItem, BillStatus } from '@/types/clinic.types'

const STATUS_COLOR: Record<BillStatus, string> = {
  DRAFT: 'blue', FINALIZED: 'green', CANCELLED: 'red',
}

export function PatientBillHistoryPage() {
  const { patientId } = useParams<{ patientId: string }>()
  const { data: bills = [], isLoading } = usePatientBills(patientId)

  const columns: TableProps<ClinicBill>['columns'] = [
    { title: 'Bill #', dataIndex: 'billNumber', key: 'billNumber' },
    { title: 'Visit Date', dataIndex: 'visitDate', key: 'visitDate' },
    {
      title: 'Total (₹)', dataIndex: 'totalAmount', key: 'totalAmount',
      render: (v: number) => v.toFixed(2),
    },
    {
      title: 'Status', dataIndex: 'status', key: 'status',
      render: (s: BillStatus) => <Tag color={STATUS_COLOR[s]}>{s}</Tag>,
    },
    { title: 'Items', key: 'items', render: (_: unknown, r: ClinicBill) => r.items.length },
  ]

  return (
    <div style={{ padding: 24 }}>
      <h2>Patient Bill History</h2>
      <Card>
        <Table
          columns={columns}
          dataSource={bills}
          rowKey="id"
          loading={isLoading}
          pagination={{ pageSize: 20 }}
          expandable={{
            expandedRowRender: (record: ClinicBill) => (
              <Table<ClinicBillItem>
                size="small"
                dataSource={record.items}
                rowKey="id"
                columns={[
                  {
                    title: 'Type', dataIndex: 'lineType',
                    render: (t: string) => <Tag>{t}</Tag>,
                  },
                  { title: 'Description', dataIndex: 'description' },
                  {
                    title: 'Amount (₹)', dataIndex: 'amount',
                    render: (v: number) => v.toFixed(2),
                  },
                ]}
                pagination={false}
              />
            ),
          }}
        />
      </Card>
    </div>
  )
}
