import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Table, Input, Button, Tag, Tooltip, Avatar, Card, Space } from 'antd'
import {
  SearchOutlined, PlusOutlined, EyeOutlined,
  FilterOutlined, DownloadOutlined, PhoneOutlined,
} from '@ant-design/icons'
import { usePatients } from '@/hooks/usePatients'
import { PageHeader } from '@/components/common/PageHeader'
import { PatientFormModal } from '@/components/forms/PatientFormModal'
import { formatDate, calcAge } from '@/utils'
import { cn } from '@/utils/cn'
import type { Patient } from '@/types'

const genderConfig = {
  MALE:   { bg: 'bg-blue-50',    text: 'text-blue-600',    avatarBg: '#dbeafe', avatarColor: '#1d4ed8' },
  FEMALE: { bg: 'bg-pink-50',    text: 'text-pink-600',    avatarBg: '#fce7f3', avatarColor: '#be185d' },
  OTHER:  { bg: 'bg-neutral-100',text: 'text-neutral-600', avatarBg: '#f1f5f9', avatarColor: '#475569' },
}

export function PatientListPage() {
  const navigate = useNavigate()
  const [query, setQuery]       = useState('')
  const [page, setPage]         = useState(0)
  const [showForm, setShowForm] = useState(false)

  const { data, isLoading } = usePatients(query || undefined, page)

  const columns = [
    {
      title: 'Patient',
      key: 'patient',
      width: 280,
      render: (_: unknown, record: Patient) => {
        const cfg = genderConfig[record.gender as keyof typeof genderConfig] ?? genderConfig.OTHER
        return (
          <div className="flex items-center gap-3">
            <Avatar
              size={40}
              style={{ background: cfg.avatarBg, color: cfg.avatarColor, fontWeight: 600, borderRadius: 10 }}
            >
              {record.firstName[0]}{record.lastName[0]}
            </Avatar>
            <div>
              <div className="font-medium" style={{ color: 'var(--text-primary)' }}>
                {record.firstName} {record.lastName}
              </div>
              <div className="text-xs" style={{ color: 'var(--text-muted)' }}>
                ID: {record.id.slice(0, 8).toUpperCase()}
              </div>
            </div>
          </div>
        )
      },
    },
    {
      title: 'Demographics',
      key: 'demographics',
      width: 180,
      render: (_: unknown, record: Patient) => {
        const cfg = genderConfig[record.gender as keyof typeof genderConfig] ?? genderConfig.OTHER
        return (
          <div className="flex items-center gap-2">
            <span
              className={cn(
                'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
                cfg.bg, cfg.text
              )}
            >
              {record.gender}
            </span>
            <span className="text-sm" style={{ color: 'var(--text-secondary)' }}>
              {calcAge(record.dateOfBirth)} yrs
            </span>
          </div>
        )
      },
    },
    {
      title: 'Contact',
      key: 'contact',
      width: 160,
      render: (_: unknown, record: Patient) => (
        <div className="flex items-center gap-1.5 text-sm" style={{ color: 'var(--text-primary)' }}>
          <PhoneOutlined style={{ color: 'var(--text-tertiary)', fontSize: 12 }} />
          {record.mobile ?? '—'}
        </div>
      ),
    },
    {
      title: 'Blood Group',
      key: 'bloodGroup',
      width: 120,
      align: 'center' as const,
      render: (_: unknown, record: Patient) =>
        record.bloodGroup ? (
          <Tag
            color="red"
            className="rounded-full px-3 text-xs font-bold"
            style={{ border: '1px solid #fca5a5', background: '#fff1f0' }}
          >
            {record.bloodGroup}
          </Tag>
        ) : (
          <span style={{ color: 'var(--text-tertiary)' }}>—</span>
        ),
    },
    {
      title: 'Registered',
      key: 'registered',
      width: 150,
      render: (_: unknown, record: Patient) => (
        <span className="text-sm" style={{ color: 'var(--text-secondary)' }}>
          {formatDate(record.createdAt)}
        </span>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 80,
      align: 'center' as const,
      render: (_: unknown, record: Patient) => (
        <Tooltip title="View / Edit">
          <Button
            type="text"
            icon={<EyeOutlined style={{ color: 'var(--color-primary-500)' }} />}
            className="rounded-lg"
            style={{ transition: 'background 150ms' }}
            onClick={() => navigate(`/patients/${record.id}`)}
          />
        </Tooltip>
      ),
    },
  ]

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader
        title="Patients"
        subtitle="Search and manage patient records across all departments"
        breadcrumbs={[
          { title: 'Dashboard', href: '/dashboard' },
          { title: 'Patients' },
        ]}
        extra={
          <Space>
            <Button
              icon={<DownloadOutlined />}
              className="rounded-lg"
            >
              Export
            </Button>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setShowForm(true)}
              className="rounded-lg shadow-glow-primary"
            >
              New Patient
            </Button>
          </Space>
        }
      />

      {/* Search & Filter bar */}
      <Card className="medical-card" styles={{ body: { padding: '16px 24px' } }}>
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <Input.Search
            placeholder="Search by name, mobile, or ID..."
            allowClear
            enterButton={<SearchOutlined />}
            className="max-w-md"
            onSearch={setQuery}
            onChange={(e) => !e.target.value && setQuery('')}
          />
          <Button icon={<FilterOutlined />} className="rounded-lg">
            Filters
          </Button>
        </div>
      </Card>

      {/* Data table */}
      <Card className="medical-card" styles={{ body: { padding: 0 } }}>
        <Table
          rowKey="id"
          dataSource={data?.content}
          columns={columns}
          loading={isLoading}
          pagination={{
            current: (data?.page ?? 0) + 1,
            pageSize: data?.size ?? 20,
            total: data?.total ?? 0,
            onChange: (p) => setPage(p - 1),
            showTotal: (total) => (
              <span className="text-sm" style={{ color: 'var(--text-muted)' }}>
                Showing{' '}
                <span className="font-medium" style={{ color: 'var(--text-primary)' }}>
                  {data?.content?.length ?? 0}
                </span>{' '}
                of{' '}
                <span className="font-medium" style={{ color: 'var(--text-primary)' }}>
                  {total}
                </span>{' '}
                patients
              </span>
            ),
            className: 'px-6 py-4',
          }}
          onRow={(record) => ({
            onDoubleClick: () => navigate(`/patients/${record.id}`),
            className: 'cursor-pointer table-row-hover',
          })}
          className="[&_.ant-table-thead>tr>th]:bg-neutral-50 [&_.ant-table-thead>tr>th]:font-semibold"
        />
      </Card>

      <PatientFormModal
        open={showForm}
        onClose={() => setShowForm(false)}
      />
    </div>
  )
}
