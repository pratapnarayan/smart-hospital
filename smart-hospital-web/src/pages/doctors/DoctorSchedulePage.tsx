import { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { Card, Button, TimePicker, Select, InputNumber, Table, Space, Alert, Spin } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined, SaveOutlined, DeleteOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { PageHeader } from '@/components/common/PageHeader'
import { useDoctor, useDoctorSchedules, useSaveDoctorSchedules } from '@/hooks/useDoctor'
import type { DoctorScheduleRequest } from '@/types'

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

interface SlotRow extends DoctorScheduleRequest { key: string }

export function DoctorSchedulePage() {
  const { id } = useParams<{ id: string }>()
  const { data: doctor, isLoading: loadingDoc }         = useDoctor(id!)
  const { data: schedules = [], isLoading: loadingSched } = useDoctorSchedules(id!)
  const { mutate: save, isPending: saving }             = useSaveDoctorSchedules(id!)

  const [rows, setRows] = useState<SlotRow[]>([])

  useEffect(() => {
    if (schedules.length > 0) {
      setRows(schedules.map((s, i) => ({
        key: String(i),
        dayOfWeek: s.dayOfWeek,
        shiftStart: s.shiftStart,
        shiftEnd: s.shiftEnd,
        slotDurationMins: s.slotDurationMins,
      })))
    }
  }, [schedules])

  const addRow = () => setRows(prev => [
    ...prev,
    { key: String(Date.now()), dayOfWeek: 'MONDAY', shiftStart: '09:00:00', shiftEnd: '13:00:00', slotDurationMins: 15 },
  ])

  const removeRow = (key: string) => setRows(prev => prev.filter(r => r.key !== key))

  const updateRow = (key: string, field: keyof DoctorScheduleRequest, value: unknown) =>
    setRows(prev => prev.map(r => r.key === key ? { ...r, [field]: value } : r))

  const handleSave = () => {
    save(rows.map(r => ({
      dayOfWeek: r.dayOfWeek,
      shiftStart: r.shiftStart,
      shiftEnd: r.shiftEnd,
      slotDurationMins: r.slotDurationMins,
    })))
  }

  if (loadingDoc || loadingSched) return <Spin size="large" style={{ display: 'flex', justifyContent: 'center', marginTop: 80 }} />
  if (!doctor) return <Alert type="error" message="Doctor not found" />

  const columns: ColumnsType<SlotRow> = [
    {
      title: 'Day', dataIndex: 'dayOfWeek', width: 140,
      render: (v: string, r) => (
        <Select
          value={v}
          onChange={val => updateRow(r.key, 'dayOfWeek', val)}
          style={{ width: '100%' }}
          options={DAYS.map(d => ({ value: d, label: d.charAt(0) + d.slice(1).toLowerCase() }))}
        />
      ),
    },
    {
      title: 'Shift Start', dataIndex: 'shiftStart', width: 130,
      render: (v: string, r) => (
        <TimePicker
          value={dayjs(v, 'HH:mm:ss')}
          format="HH:mm"
          onChange={(t) => updateRow(r.key, 'shiftStart', t ? t.format('HH:mm:ss') : '09:00:00')}
        />
      ),
    },
    {
      title: 'Shift End', dataIndex: 'shiftEnd', width: 130,
      render: (v: string, r) => (
        <TimePicker
          value={dayjs(v, 'HH:mm:ss')}
          format="HH:mm"
          onChange={(t) => updateRow(r.key, 'shiftEnd', t ? t.format('HH:mm:ss') : '13:00:00')}
        />
      ),
    },
    {
      title: 'Slot (min)', dataIndex: 'slotDurationMins', width: 110,
      render: (v: number, r) => (
        <InputNumber
          value={v}
          min={5}
          max={60}
          step={5}
          onChange={val => updateRow(r.key, 'slotDurationMins', val ?? 15)}
        />
      ),
    },
    {
      title: '', key: 'del', width: 60,
      render: (_: unknown, r) => (
        <Button danger icon={<DeleteOutlined />} size="small" onClick={() => removeRow(r.key)} />
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title={`Schedule — Dr. ${doctor.firstName} ${doctor.lastName}`}
        subtitle="Configure weekly OPD schedule"
        breadcrumbs={[
          { title: 'Doctors', href: '/doctors' },
          { title: `Dr. ${doctor.firstName} ${doctor.lastName}`, href: `/doctors/${id}` },
          { title: 'Schedule' },
        ]}
        extra={
          <Space>
            <Button icon={<PlusOutlined />} onClick={addRow}>Add Shift</Button>
            <Button type="primary" icon={<SaveOutlined />} onClick={handleSave} loading={saving}>
              Save Schedule
            </Button>
          </Space>
        }
      />
      <Card>
        <Table
          rowKey="key"
          dataSource={rows}
          columns={columns}
          pagination={false}
          locale={{ emptyText: 'No shifts configured. Click "Add Shift" to start.' }}
        />
      </Card>
    </div>
  )
}
