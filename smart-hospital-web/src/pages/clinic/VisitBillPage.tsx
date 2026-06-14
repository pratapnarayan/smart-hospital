import { useState } from 'react'
import { Card, Row, Col, Button, Table, Tag, Space, message, Divider, Typography } from 'antd'
import { PrinterOutlined } from '@ant-design/icons'
import type { TableProps } from 'antd'
import type { AxiosError } from 'axios'
import { PatientSearchSelect } from '@/components/common/PatientSearchSelect'
import { useGenerateBill } from '@/hooks/useClinic'
import type { ClinicBillItem, BillStatus } from '@/types/clinic.types'
import type { Patient } from '@/types'

const STATUS_COLOR: Record<BillStatus, string> = {
  DRAFT: 'blue', FINALIZED: 'green', CANCELLED: 'red',
}

export function VisitBillPage() {
  const [selectedPatient, setSelectedPatient] = useState<Patient | null>(null)
  const [opdVisitId, setOpdVisitId] = useState<string>('')
  const { existingBills, generate, finalize, cancel } = useGenerateBill(opdVisitId || undefined)

  // suppress unused variable warning — selectedPatient is set alongside opdVisitId
  void selectedPatient

  const bills = existingBills.data ?? []
  const activeBill = bills.find(b => b.status !== 'CANCELLED')

  const handleGenerate = () => {
    if (!opdVisitId) { message.warning('Select an OPD visit first'); return }
    generate.mutate(opdVisitId, {
      onError: (err: unknown) => message.error((err as AxiosError<{ message?: string }>)?.response?.data?.message ?? 'Failed to generate bill'),
    })
  }

  const handleFinalize = (id: string) => {
    finalize.mutate(id, {
      onSuccess: () => setTimeout(() => window.print(), 300),
      onError: (err: unknown) => message.error((err as AxiosError<{ message?: string }>)?.response?.data?.message ?? 'Failed to finalize'),
    })
  }

  const itemColumns: TableProps<ClinicBillItem>['columns'] = [
    {
      title: 'Type', dataIndex: 'lineType', key: 'lineType', width: 130,
      render: (t: string) => <Tag>{t}</Tag>,
    },
    { title: 'Description', dataIndex: 'description', key: 'description' },
    {
      title: 'Amount (₹)', dataIndex: 'amount', key: 'amount', align: 'right',
      render: (v: number) => v.toFixed(2),
    },
  ]

  return (
    <div style={{ padding: 24 }} className="visit-bill-page">
      <style>{`@media print { .no-print { display: none !important; } }`}</style>

      <Row gutter={16} className="no-print" style={{ marginBottom: 16 }}>
        <Col span={16}>
          <PatientSearchSelect
            valueMode="id"
            placeholder="Search patient..."
            onPatientSelect={(p: Patient) => {
              setSelectedPatient(p)
              setOpdVisitId('')
            }}
          />
        </Col>
        <Col>
          <Button type="primary" onClick={handleGenerate} loading={generate.isPending}>
            Generate Bill
          </Button>
        </Col>
      </Row>

      {activeBill && (
        <Card
          title={
            <Space>
              <Typography.Text strong>Bill #{activeBill.billNumber}</Typography.Text>
              <Tag color={STATUS_COLOR[activeBill.status]}>{activeBill.status}</Tag>
            </Space>
          }
          extra={
            <Space className="no-print">
              {activeBill.status === 'DRAFT' && (
                <>
                  <Button
                    type="primary"
                    icon={<PrinterOutlined />}
                    loading={finalize.isPending}
                    onClick={() => handleFinalize(activeBill.id)}
                  >
                    Finalize & Print
                  </Button>
                  <Button danger onClick={() => cancel.mutate(activeBill.id)}>Cancel</Button>
                </>
              )}
              {activeBill.status === 'FINALIZED' && (
                <Button icon={<PrinterOutlined />} onClick={() => window.print()}>Print</Button>
              )}
            </Space>
          }
        >
          <p><strong>Patient:</strong> {activeBill.patientName}</p>
          <p><strong>Visit Date:</strong> {activeBill.visitDate}</p>
          <Divider />
          <Table
            columns={itemColumns}
            dataSource={activeBill.items}
            rowKey="id"
            pagination={false}
            summary={() => (
              <Table.Summary.Row>
                <Table.Summary.Cell index={0} colSpan={2}><strong>Total</strong></Table.Summary.Cell>
                <Table.Summary.Cell index={1} align="right">
                  <strong>₹{activeBill.totalAmount.toFixed(2)}</strong>
                </Table.Summary.Cell>
              </Table.Summary.Row>
            )}
          />
        </Card>
      )}
    </div>
  )
}
