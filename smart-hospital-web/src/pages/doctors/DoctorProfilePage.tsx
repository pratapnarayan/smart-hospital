import { useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  Card, Descriptions, Tag, Button, Tabs, Spin, Alert, Row, Col,
  Typography, Space, Calendar, Badge,
} from 'antd'
import type { BadgeProps } from 'antd'
import { EditOutlined, CalendarOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { PageHeader } from '@/components/common/PageHeader'
import { useDoctor, useDoctorSchedules, useDoctorAvailability, useUploadDoctorPhoto } from '@/hooks/useDoctor'
import { useHrDepartments } from '@/hooks/useHr'
import { useAuthStore } from '@/store/authStore'
import { AppointmentFormModal } from '@/pages/frontoffice/AppointmentFormModal'
import { DoctorProfileModal } from './DoctorProfileModal'
import { PhotoUpload } from '@/components/common/PhotoUpload'
import { formatDate, formatCurrency } from '@/utils'

const { Title, Paragraph, Text } = Typography

// BadgeProps referenced for type completeness
type _BadgeProps = BadgeProps

export function DoctorProfilePage() {
  const { id } = useParams<{ id: string }>()
  const { hasPermission } = useAuthStore()
  const [editOpen, setEditOpen]   = useState(false)
  const [apptOpen, setApptOpen]   = useState(false)
  const [calDate, setCalDate]     = useState(dayjs())

  const { data: doctor, isLoading, isError } = useDoctor(id!)
  const { mutate: uploadPhoto, isPending: uploading } = useUploadDoctorPhoto(id!)
  const { data: schedules = [] }             = useDoctorSchedules(id!)
  const { data: depts = [] }                 = useHrDepartments()

  const fromStr = dayjs().format('YYYY-MM-DD')
  const toStr   = dayjs().add(60, 'day').format('YYYY-MM-DD')
  const { data: availability = [] } = useDoctorAvailability(id!, fromStr, toStr)

  if (isLoading) return <Spin size="large" style={{ display: 'flex', justifyContent: 'center', marginTop: 80 }} />
  if (isError || !doctor) return <Alert type="error" message="Doctor not found" />

  const deptName = depts.find(d => d.id === doctor.departmentId)?.name ?? '—'

  function dateCellRender(value: Dayjs) {
    const dateStr = value.format('YYYY-MM-DD')
    const slot    = availability.find(a => a.date === dateStr)
    if (!slot) return null
    return (
      <Badge status="success" text={`${slot.slots.length} slots`} style={{ fontSize: 10 }} />
    )
  }

  const selectedDateSlots = availability.find(
    a => a.date === calDate.format('YYYY-MM-DD')
  )?.slots ?? []

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader
        title={`Dr. ${doctor.firstName} ${doctor.lastName}`}
        subtitle={doctor.qualifications ?? ''}
        breadcrumbs={[
          { title: 'Doctor Directory', href: '/doctors' },
          { title: `Dr. ${doctor.firstName} ${doctor.lastName}` },
        ]}
        extra={
          <Space>
            <Button type="primary" icon={<CalendarOutlined />} onClick={() => setApptOpen(true)}>
              Book Appointment
            </Button>
            {hasPermission('DOCTOR.EDIT') && (
              <Button icon={<EditOutlined />} onClick={() => setEditOpen(true)}>Edit Profile</Button>
            )}
          </Space>
        }
      />

      {/* Hero Section */}
      <Card className="medical-card">
        <Row gutter={24} align="middle">
          <Col>
            <PhotoUpload
              photoUrl={doctor.profilePhoto}
              name={`${doctor.firstName} ${doctor.lastName}`}
              size={100}
              uploading={uploading}
              onFileSelect={(file) => uploadPhoto(file)}
              editable={hasPermission('DOCTOR.EDIT')}
            />
          </Col>
          <Col flex="auto">
            <Title level={3} style={{ marginBottom: 4 }}>
              Dr. {doctor.firstName} {doctor.lastName}
            </Title>
            {doctor.qualifications && (
              <Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>
                {doctor.qualifications}
              </Text>
            )}
            <Space wrap>
              <Tag color="blue">{deptName}</Tag>
              {doctor.specializations.map(s => (
                <Tag key={s.id} color="geekblue">{s.name}</Tag>
              ))}
              <Tag color={doctor.status === 'ACTIVE' ? 'success' : 'default'}>
                {doctor.status}
              </Tag>
            </Space>
          </Col>
          <Col>
            <Space direction="vertical" size={4}>
              {doctor.consultationFee != null && doctor.consultationFee > 0 && (
                <Text><strong>Consultation:</strong> {formatCurrency(doctor.consultationFee)}</Text>
              )}
              {doctor.followUpFee != null && doctor.followUpFee > 0 && (
                <Text><strong>Follow-up:</strong> {formatCurrency(doctor.followUpFee)}</Text>
              )}
              {doctor.teleConsultationFee != null && doctor.teleConsultationFee > 0 && (
                <Text><strong>Teleconsultation:</strong> {formatCurrency(doctor.teleConsultationFee)}</Text>
              )}
              {doctor.experienceYears != null && doctor.experienceYears > 0 && (
                <Text><strong>Experience:</strong> {doctor.experienceYears} years</Text>
              )}
            </Space>
          </Col>
        </Row>
      </Card>

      <Tabs
        defaultActiveKey="overview"
        items={[
          {
            key: 'overview',
            label: 'Overview',
            children: (
              <Row gutter={16}>
                <Col xs={24} lg={16} className="space-y-4">
                  {doctor.biography && (
                    <Card title="About" className="medical-card">
                      <Paragraph>{doctor.biography}</Paragraph>
                    </Card>
                  )}
                  {doctor.specializations.length > 0 && (
                    <Card title="Areas of Expertise" className="medical-card">
                      <Space wrap>
                        {doctor.specializations.map(s => (
                          <Tag key={s.id} color="blue" style={{ padding: '4px 12px', fontSize: 13 }}>
                            {s.name}
                          </Tag>
                        ))}
                      </Space>
                    </Card>
                  )}
                  {doctor.awards && (
                    <Card title="Awards & Recognition" className="medical-card">
                      <Paragraph>{doctor.awards}</Paragraph>
                    </Card>
                  )}
                  {doctor.achievements && (
                    <Card title="Achievements" className="medical-card">
                      <Paragraph>{doctor.achievements}</Paragraph>
                    </Card>
                  )}
                  {doctor.publications && (
                    <Card title="Publications" className="medical-card">
                      <Paragraph>{doctor.publications}</Paragraph>
                    </Card>
                  )}
                </Col>
                <Col xs={24} lg={8}>
                  <Card title="Professional Details" className="medical-card">
                    <Descriptions column={1} size="small">
                      <Descriptions.Item label="Employee Code">{doctor.employeeCode}</Descriptions.Item>
                      <Descriptions.Item label="Department">{deptName}</Descriptions.Item>
                      <Descriptions.Item label="Join Date">{formatDate(doctor.joinDate)}</Descriptions.Item>
                      <Descriptions.Item label="Mobile">{doctor.mobile ?? '—'}</Descriptions.Item>
                      <Descriptions.Item label="Email">{doctor.email ?? '—'}</Descriptions.Item>
                      <Descriptions.Item label="Languages">{doctor.languages ?? '—'}</Descriptions.Item>
                      <Descriptions.Item label="Online Booking">
                        <Tag color={doctor.onlineBookingEnabled ? 'success' : 'default'}>
                          {doctor.onlineBookingEnabled ? 'Enabled' : 'Disabled'}
                        </Tag>
                      </Descriptions.Item>
                    </Descriptions>
                  </Card>
                </Col>
              </Row>
            ),
          },
          {
            key: 'schedule',
            label: 'Weekly Schedule',
            children: (
              <Card className="medical-card">
                {!schedules.length ? (
                  <Alert type="info" message="No schedule configured for this doctor." />
                ) : (
                  <Row gutter={[8, 8]}>
                    {['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'].map(day => {
                      const daySchedules = schedules.filter(s => s.dayOfWeek === day && s.active)
                      return (
                        <Col key={day} xs={24} sm={12} md={8} lg={6}>
                          <Card
                            size="small"
                            title={day.charAt(0) + day.slice(1).toLowerCase()}
                            style={{ background: daySchedules.length ? '#f6ffed' : '#fafafa' }}
                          >
                            {daySchedules.length === 0
                              ? <Text type="secondary" style={{ fontSize: 12 }}>Not available</Text>
                              : daySchedules.map(s => (
                                  <div key={s.id} style={{ fontSize: 12, marginBottom: 4 }}>
                                    <Tag color="green">{s.shiftStart.slice(0, 5)} – {s.shiftEnd.slice(0, 5)}</Tag>
                                    <Text type="secondary">{s.slotDurationMins} min slots</Text>
                                  </div>
                                ))
                            }
                          </Card>
                        </Col>
                      )
                    })}
                  </Row>
                )}
              </Card>
            ),
          },
          {
            key: 'availability',
            label: 'Availability & Booking',
            children: (
              <Row gutter={16}>
                <Col xs={24} lg={16}>
                  <Card title="Availability Calendar" className="medical-card">
                    <Calendar
                      fullscreen={false}
                      onSelect={(date: Dayjs) => setCalDate(date)}
                      cellRender={(value: Dayjs) => {
                        if (value.format('YYYY-MM').startsWith(calDate.format('YYYY-MM'))) {
                          return dateCellRender(value)
                        }
                        return null
                      }}
                    />
                  </Card>
                </Col>
                <Col xs={24} lg={8}>
                  <Card title={`Slots — ${calDate.format('DD MMM YYYY')}`} className="medical-card">
                    {selectedDateSlots.length === 0 ? (
                      <Alert type="info" message="No slots available on this date." />
                    ) : (
                      <Space wrap>
                        {selectedDateSlots.map(slot => (
                          <Button
                            key={slot}
                            size="small"
                            type="default"
                            style={{ marginBottom: 8 }}
                            onClick={() => setApptOpen(true)}
                          >
                            {slot.slice(0, 5)}
                          </Button>
                        ))}
                      </Space>
                    )}
                    {selectedDateSlots.length > 0 && (
                      <Button
                        type="primary"
                        block
                        icon={<CalendarOutlined />}
                        style={{ marginTop: 16 }}
                        onClick={() => setApptOpen(true)}
                      >
                        Book Appointment
                      </Button>
                    )}
                  </Card>
                </Col>
              </Row>
            ),
          },
        ]}
      />

      <AppointmentFormModal
        open={apptOpen}
        onClose={() => setApptOpen(false)}
      />

      {editOpen && (
        <DoctorProfileModal
          open={editOpen}
          onClose={() => setEditOpen(false)}
          doctor={doctor}
        />
      )}
    </div>
  )
}
