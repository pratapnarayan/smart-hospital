import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Row, Col, Input, Select, Button, Tag, Avatar, Space, Pagination, Empty, Spin, Statistic } from 'antd'
import { SearchOutlined, UserOutlined, PlusOutlined, CalendarOutlined } from '@ant-design/icons'
import { PageHeader } from '@/components/common/PageHeader'
import { useDoctors, useSpecializations, useDoctorDashboard } from '@/hooks/useDoctor'
import { useHrDepartments } from '@/hooks/useHr'
import { useAuthStore } from '@/store/authStore'
import { AppointmentFormModal } from '@/pages/frontoffice/AppointmentFormModal'
import { DoctorProfileModal } from './DoctorProfileModal'
import { formatCurrency } from '@/utils'
import type { DoctorProfile } from '@/types'

export function DoctorDirectoryPage() {
  const navigate = useNavigate()
  const { hasPermission } = useAuthStore()
  const [search, setSearch]         = useState('')
  const [deptId, setDeptId]         = useState<string | undefined>()
  const [specId, setSpecId]         = useState<string | undefined>()
  const [page, setPage]             = useState(0)
  const [apptDoctor, setApptDoctor] = useState<DoctorProfile | null>(null)
  const [addOpen, setAddOpen]       = useState(false)

  const { data, isLoading }         = useDoctors(search || undefined, deptId, specId, page)
  const { data: specs = [] }        = useSpecializations()
  const { data: depts = [] }        = useHrDepartments()
  const { data: dash }              = useDoctorDashboard()

  const deptMap = Object.fromEntries(depts.map(d => [d.id, d.name]))

  return (
    <>
      <PageHeader
        title="Doctor Directory"
        subtitle="Find and connect with our specialists"
        extra={
          hasPermission('DOCTOR.CREATE') ? (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setAddOpen(true)}>
              Add Doctor Profile
            </Button>
          ) : undefined
        }
      />

      {dash && (
        <Row gutter={16} style={{ marginBottom: 24 }}>
          <Col span={6}><Card><Statistic title="Total Doctors"      value={dash.totalDoctors} /></Card></Col>
          <Col span={6}><Card><Statistic title="Active"             value={dash.activeDoctors}        valueStyle={{ color: '#52c41a' }} /></Card></Col>
          <Col span={6}><Card><Statistic title="Available Today"    value={dash.availableToday}       valueStyle={{ color: '#1677ff' }} /></Card></Col>
          <Col span={6}><Card><Statistic title="Specializations"    value={dash.totalSpecializations} /></Card></Col>
        </Row>
      )}

      <Card
        style={{ marginBottom: 16 }}
        styles={{ body: { paddingBottom: 0 } }}
        title={
          <Space wrap>
            <Input
              placeholder="Search by name"
              prefix={<SearchOutlined />}
              style={{ width: 220 }}
              allowClear
              onChange={e => { setSearch(e.target.value); setPage(0) }}
            />
            <Select
              allowClear
              placeholder="Department"
              style={{ width: 180 }}
              onChange={(v: string | undefined) => { setDeptId(v); setPage(0) }}
              options={depts.map(d => ({ value: d.id, label: d.name }))}
            />
            <Select
              allowClear
              placeholder="Specialization"
              style={{ width: 180 }}
              onChange={(v: string | undefined) => { setSpecId(v); setPage(0) }}
              options={specs.map(s => ({ value: s.id, label: s.name }))}
            />
          </Space>
        }
      >
        {isLoading ? (
          <div style={{ textAlign: 'center', padding: 48 }}><Spin size="large" /></div>
        ) : !data?.content?.length ? (
          <Empty description="No doctors found" style={{ padding: 48 }} />
        ) : (
          <Row gutter={[16, 16]} style={{ padding: '16px 0' }}>
            {data.content.map(doctor => (
              <Col key={doctor.id} xs={24} sm={12} lg={8}>
                <Card
                  hoverable
                  actions={[
                    <Button type="link" key="view" onClick={() => navigate(`/doctors/${doctor.id}`)}>
                      View Profile
                    </Button>,
                    <Button
                      type="link"
                      key="book"
                      icon={<CalendarOutlined />}
                      onClick={() => setApptDoctor(doctor)}
                    >
                      Book Appointment
                    </Button>,
                  ]}
                >
                  <Card.Meta
                    avatar={
                      doctor.profilePhoto
                        ? <Avatar size={64} src={doctor.profilePhoto} />
                        : (
                          <Avatar size={64} icon={<UserOutlined />} style={{ background: '#1677ff', fontSize: 20 }}>
                            {doctor.firstName[0]}{doctor.lastName[0]}
                          </Avatar>
                        )
                    }
                    title={`Dr. ${doctor.firstName} ${doctor.lastName}`}
                    description={
                      <Space direction="vertical" size={4} style={{ width: '100%' }}>
                        {doctor.qualifications && (
                          <span style={{ color: '#666', fontSize: 12 }}>{doctor.qualifications}</span>
                        )}
                        {doctor.departmentId && (
                          <span style={{ fontSize: 12, color: '#888' }}>
                            Dept: {deptMap[doctor.departmentId] ?? '—'}
                          </span>
                        )}
                        <div>
                          {doctor.specializations.slice(0, 2).map(s => (
                            <Tag key={s.id} color="blue" style={{ marginBottom: 4 }}>{s.name}</Tag>
                          ))}
                          {doctor.specializations.length > 2 && (
                            <Tag>+{doctor.specializations.length - 2}</Tag>
                          )}
                        </div>
                        {doctor.experienceYears != null && doctor.experienceYears > 0 && (
                          <span style={{ fontSize: 12 }}>{doctor.experienceYears} yrs experience</span>
                        )}
                        {doctor.consultationFee != null && doctor.consultationFee > 0 && (
                          <span style={{ fontSize: 12, color: '#52c41a' }}>
                            Consultation: {formatCurrency(doctor.consultationFee)}
                          </span>
                        )}
                      </Space>
                    }
                  />
                </Card>
              </Col>
            ))}
          </Row>
        )}

        {data && data.total > 0 && (
          <div style={{ textAlign: 'right', padding: '16px 0' }}>
            <Pagination
              current={page + 1}
              pageSize={20}
              total={data.total}
              onChange={p => setPage(p - 1)}
              showSizeChanger={false}
            />
          </div>
        )}
      </Card>

      {apptDoctor && (
        <AppointmentFormModal
          open={!!apptDoctor}
          onClose={() => setApptDoctor(null)}
        />
      )}

      {addOpen && <DoctorProfileModal open={addOpen} onClose={() => setAddOpen(false)} />}
    </>
  )
}
