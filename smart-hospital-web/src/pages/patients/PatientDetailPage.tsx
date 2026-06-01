import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { Card, Descriptions, Tag, Button, Tabs, Spin, Alert, Typography } from 'antd'
import { EditOutlined, PlusOutlined, CalendarOutlined } from '@ant-design/icons'
import { AppointmentFormModal } from '@/pages/frontoffice/AppointmentFormModal'
import { usePatient } from '@/hooks/usePatients'
import { useVisitsByPatient } from '@/hooks/useOpdVisits'
import { usePatientUpcomingAppointments, useCancelAppointment } from '@/hooks/useFrontOffice'
import { PageHeader } from '@/components/common/PageHeader'
import { PatientFormModal } from './PatientFormModal'
import { formatDate, formatDateTime, calcAge, formatCurrency } from '@/utils'
import type { TableProps } from 'antd'
import { Table, Popconfirm, Empty } from 'antd'
import type { OpdVisit, Appointment, AppointmentStatus } from '@/types'
import { useNavigate } from 'react-router-dom'

export function PatientDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [editOpen, setEditOpen] = useState(false)
  const [apptOpen, setApptOpen] = useState(false)

  const { data: patient, isLoading, isError } = usePatient(id!)
  const { data: visitsPage } = useVisitsByPatient(id!)
  const { data: upcomingApts } = usePatientUpcomingAppointments(id!)
  const { mutate: cancelApt } = useCancelAppointment()

  if (isLoading) return <Spin size="large" className="flex justify-center mt-20" />
  if (isError || !patient) return <Alert type="error" message="Patient not found" />

  const visitColumns: TableProps<OpdVisit>['columns'] = [
    { title: 'Visit #', dataIndex: 'visitNumber' },
    { title: 'Date', dataIndex: 'visitDate', render: formatDate },
    { title: 'Doctor', dataIndex: 'doctorName', render: (v) => v ?? '—' },
    { title: 'Diagnosis', dataIndex: 'diagnosis', render: (v) => v ?? '—', ellipsis: true },
    { title: 'Amount', dataIndex: 'netAmount', render: formatCurrency },
    {
      title: 'Status', dataIndex: 'visitStatus',
      render: (v: string) => {
        const colors: Record<string, string> = {
          COMPLETED: 'green', IN_PROGRESS: 'blue', REGISTERED: 'orange', CANCELLED: 'red'
        }
        return <Tag color={colors[v] ?? 'default'}>{v}</Tag>
      },
    },
    {
      title: '', key: 'actions',
      render: (_, r) => (
        <Button size="small" onClick={() => navigate(`/opd/${r.id}`)}>View</Button>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title={`${patient.firstName} ${patient.lastName}`}
        subtitle={`Registered ${formatDate(patient.createdAt)}`}
        breadcrumbs={[
          { title: 'Patients', href: '/patients' },
          { title: `${patient.firstName} ${patient.lastName}` },
        ]}
        extra={
          <Button.Group>
            <Button icon={<CalendarOutlined />} onClick={() => setApptOpen(true)}>
              Book Appointment
            </Button>
            <Button icon={<EditOutlined />} onClick={() => setEditOpen(true)}>
              Edit
            </Button>
          </Button.Group>
        }
      />

      <Card className="mb-4">
        <Descriptions column={{ xs: 1, sm: 2, lg: 3 }} bordered size="small">
          <Descriptions.Item label="Patient ID" span={3}>
            <Typography.Text code copyable={{ text: patient.id }}>
              {patient.id}
            </Typography.Text>
          </Descriptions.Item>
          <Descriptions.Item label="Gender">
            <Tag color={patient.gender === 'MALE' ? 'blue' : patient.gender === 'FEMALE' ? 'pink' : 'default'}>
              {patient.gender}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Date of Birth">
            {formatDate(patient.dateOfBirth)} ({calcAge(patient.dateOfBirth)})
          </Descriptions.Item>
          <Descriptions.Item label="Blood Group">
            {patient.bloodGroup ? <Tag color="red">{patient.bloodGroup}</Tag> : '—'}
          </Descriptions.Item>
          <Descriptions.Item label="Mobile">{patient.mobile ?? '—'}</Descriptions.Item>
          <Descriptions.Item label="Email">{patient.email ?? '—'}</Descriptions.Item>
          <Descriptions.Item label="Address" span={2}>{patient.address ?? '—'}</Descriptions.Item>
          <Descriptions.Item label="Guardian">
            {patient.guardianName ?? '—'}
          </Descriptions.Item>
          <Descriptions.Item label="Registered">{formatDateTime(patient.createdAt)}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Tabs
        defaultActiveKey="appointments"
        items={[
          {
            key: 'appointments',
            label: `Upcoming Appointments${upcomingApts?.length ? ` (${upcomingApts.length})` : ''}`,
            children: (
              <Card
                extra={
                  <Button icon={<CalendarOutlined />} size="small" type="primary"
                    onClick={() => setApptOpen(true)}>
                    Book Appointment
                  </Button>
                }
              >
                {!upcomingApts?.length ? (
                  <Empty description="No upcoming appointments" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                ) : (
                  <Table
                    rowKey="id"
                    size="small"
                    pagination={false}
                    dataSource={upcomingApts}
                    columns={[
                      { title: 'Date',       dataIndex: 'appointmentDate', render: formatDate, width: 110 },
                      { title: 'Time Slot',  dataIndex: 'timeSlot',        render: (v?: string) => v ?? '—' },
                      { title: 'Doctor',     dataIndex: 'doctorName',      render: (v?: string) => v ?? '—' },
                      { title: 'Department', dataIndex: 'department',      render: (v?: string) => v ?? '—' },
                      { title: 'Type',       dataIndex: 'appointmentType',
                        render: (v: string) => <Tag>{v.replace('_', ' ')}</Tag> },
                      { title: 'Status',     dataIndex: 'status',
                        render: (v: AppointmentStatus) => (
                          <Tag color={v === 'CONFIRMED' ? 'processing' : 'default'}>{v}</Tag>
                        ),
                      },
                      {
                        title: '', key: 'actions',
                        render: (_, r: Appointment) => (
                          <Popconfirm title="Cancel this appointment?" onConfirm={() => cancelApt(r.id)}>
                            <Button size="small" danger>Cancel</Button>
                          </Popconfirm>
                        ),
                      },
                    ] satisfies TableProps<Appointment>['columns']}
                  />
                )}
              </Card>
            ),
          },
          {
            key: 'opd',
            label: 'OPD History',
            children: (
              <Card
                extra={
                  <Button
                    type="primary" size="small" icon={<PlusOutlined />}
                    onClick={() => navigate(`/opd?patientId=${patient.id}`)}
                  >
                    New Visit
                  </Button>
                }
              >
                <Table
                  rowKey="id"
                  size="small"
                  dataSource={visitsPage?.content}
                  columns={visitColumns}
                  pagination={false}
                />
              </Card>
            ),
          },
          {
            key: 'pharmacy',
            label: 'Pharmacy Bills',
            children: (
              <Card>
                <p className="text-gray-400">Pharmacy bill history — coming in Phase 1c wire-up.</p>
              </Card>
            ),
          },
        ]}
      />

      <PatientFormModal
        open={editOpen}
        onClose={() => setEditOpen(false)}
        patient={patient}
      />
      <AppointmentFormModal
        open={apptOpen}
        onClose={() => setApptOpen(false)}
        patientId={patient.id}
        patientName={`${patient.firstName} ${patient.lastName}`}
      />
    </div>
  )
}
