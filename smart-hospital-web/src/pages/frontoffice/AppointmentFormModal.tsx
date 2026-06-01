import { useEffect, useState } from 'react'
import { Modal, Form, Input, Select, DatePicker, Row, Col, Typography } from 'antd'
import dayjs from 'dayjs'
import { useBookAppointment } from '@/hooks/useFrontOffice'
import { usePatients } from '@/hooks/usePatients'
import { useHrDepartments, useEmployees } from '@/hooks/useHr'
import type { BookAppointmentPayload } from '@/types'

const TIME_SLOTS = [
  '08:00-08:30','08:30-09:00','09:00-09:30','09:30-10:00',
  '10:00-10:30','10:30-11:00','11:00-11:30','11:30-12:00',
  '14:00-14:30','14:30-15:00','15:00-15:30','15:30-16:00',
  '16:00-16:30','16:30-17:00',
]

interface Props {
  open: boolean
  onClose: () => void
  patientId?: string
  patientName?: string
}

export function AppointmentFormModal({ open, onClose, patientId, patientName }: Props) {
  const [form] = Form.useForm<BookAppointmentPayload & { _date: dayjs.Dayjs }>()
  const { mutate: book, isPending } = useBookAppointment()

  // ── Patient search (used when no patientId is pre-filled) ─────────────────
  const [patientSearch, setPatientSearch]         = useState('')
  const [debouncedPatient, setDebouncedPatient]   = useState('')
  useEffect(() => {
    const t = setTimeout(() => setDebouncedPatient(patientSearch), 300)
    return () => clearTimeout(t)
  }, [patientSearch])
  const { data: patientResults, isFetching: searchingPatients } = usePatients(
    debouncedPatient || undefined, 0, 10
  )
  const patientOptions = (patientResults?.content ?? []).map(p => ({
    value: p.id,
    label: `${p.firstName} ${p.lastName}${p.mobile ? ` · ${p.mobile}` : ''}`,
  }))

  // ── Department list ────────────────────────────────────────────────────────
  const { data: departments } = useHrDepartments()
  const deptOptions = (departments ?? []).map(d => ({ value: d.name, label: d.name, id: d.id }))

  // ── Doctor search — filtered by selected department ────────────────────────
  const [selectedDeptId, setSelectedDeptId]       = useState<string | undefined>()
  const [doctorSearch, setDoctorSearch]           = useState('')
  const [debouncedDoctor, setDebouncedDoctor]     = useState('')
  useEffect(() => {
    const t = setTimeout(() => setDebouncedDoctor(doctorSearch), 300)
    return () => clearTimeout(t)
  }, [doctorSearch])
  const { data: employeePage, isFetching: searchingDoctors } = useEmployees(
    selectedDeptId, debouncedDoctor || undefined, 0
  )
  const doctorOptions = (employeePage?.content ?? []).map(e => ({
    value: e.id,
    label: `Dr. ${e.firstName} ${e.lastName}`,
  }))

  // ── Reset on open ──────────────────────────────────────────────────────────
  useEffect(() => {
    if (open) {
      form.resetFields()
      setPatientSearch(''); setDebouncedPatient('')
      setDoctorSearch('');  setDebouncedDoctor('')
      setSelectedDeptId(undefined)
      form.setFieldsValue({ _date: dayjs() })
      if (patientId) form.setFieldValue('patientId', patientId)
    }
  }, [open, patientId, form])

  // ── Handlers ───────────────────────────────────────────────────────────────
  const onDeptChange = (deptName: string) => {
    const dept = (departments ?? []).find(d => d.name === deptName)
    setSelectedDeptId(dept?.id)
    // Clear doctor whenever department changes
    form.setFieldsValue({ doctorId: undefined, doctorName: undefined })
    setDoctorSearch(''); setDebouncedDoctor('')
  }

  const onDoctorChange = (empId: string) => {
    const emp = (employeePage?.content ?? []).find(e => e.id === empId)
    if (emp) form.setFieldValue('doctorName', `Dr. ${emp.firstName} ${emp.lastName}`)
  }

  const onFinish = (values: BookAppointmentPayload & { _date: dayjs.Dayjs }) => {
    const { _date, ...rest } = values
    const payload = Object.fromEntries(
      Object.entries(rest).filter(([, v]) => v !== '' && v != null)
    ) as BookAppointmentPayload
    book({ ...payload, appointmentDate: _date.format('YYYY-MM-DD') }, { onSuccess: onClose })
  }

  return (
    <Modal
      title="Book Appointment"
      open={open}
      onCancel={onClose}
      onOk={() => form.submit()}
      okText="Book"
      confirmLoading={isPending}
      width={600}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" onFinish={onFinish}>

        {/* ── Patient ──────────────────────────────────────────────────────── */}
        {patientId ? (
          <>
            <Form.Item name="patientId" hidden><Input /></Form.Item>
            <Form.Item label="Patient">
              <Typography.Text strong>{patientName ?? patientId}</Typography.Text>
            </Form.Item>
          </>
        ) : (
          <Form.Item
            name="patientId"
            label="Patient"
            rules={[{ required: true, message: 'Please select a patient' }]}
          >
            <Select
              showSearch
              placeholder="Search by name or mobile…"
              filterOption={false}
              onSearch={setPatientSearch}
              loading={searchingPatients}
              options={patientOptions}
              notFoundContent={
                debouncedPatient
                  ? (searchingPatients ? 'Searching…' : 'No patients found')
                  : 'Type a name or mobile number'
              }
            />
          </Form.Item>
        )}

        {/* ── Date + Time Slot ─────────────────────────────────────────────── */}
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="_date" label="Date" rules={[{ required: true }]}>
              <DatePicker style={{ width: '100%' }} disabledDate={d => d.isBefore(dayjs(), 'day')} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="timeSlot" label="Time Slot">
              <Select
                showSearch
                placeholder="Select slot"
                options={TIME_SLOTS.map(s => ({ value: s, label: s }))}
              />
            </Form.Item>
          </Col>
        </Row>

        {/* ── Department + Appointment Type ─────────────────────────────────── */}
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="department" label="Department">
              <Select
                showSearch
                placeholder="Select department"
                allowClear
                filterOption={(input, opt) =>
                  (opt?.label as string ?? '').toLowerCase().includes(input.toLowerCase())
                }
                options={deptOptions}
                onChange={onDeptChange}
              />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="appointmentType" label="Type" initialValue="CONSULTATION">
              <Select options={[
                { value: 'CONSULTATION', label: 'Consultation' },
                { value: 'FOLLOW_UP',    label: 'Follow-up' },
                { value: 'EMERGENCY',    label: 'Emergency' },
                { value: 'PROCEDURE',    label: 'Procedure' },
              ]} />
            </Form.Item>
          </Col>
        </Row>

        {/* ── Doctor ───────────────────────────────────────────────────────── */}
        {/* doctorName is submitted but never shown — it's set by onDoctorChange */}
        <Form.Item name="doctorName" hidden><Input /></Form.Item>
        <Form.Item name="doctorId" label="Doctor">
          <Select
            showSearch
            placeholder={selectedDeptId ? 'Search doctor in selected department…' : 'Select department first, or search all doctors…'}
            allowClear
            filterOption={false}
            onSearch={setDoctorSearch}
            loading={searchingDoctors}
            onChange={onDoctorChange}
            options={doctorOptions}
            notFoundContent={
              debouncedDoctor
                ? (searchingDoctors ? 'Searching…' : 'No doctors found')
                : (selectedDeptId ? 'No staff in this department' : 'Type to search all staff')
            }
          />
        </Form.Item>

        {/* ── Notes ────────────────────────────────────────────────────────── */}
        <Form.Item name="notes" label="Notes">
          <Input.TextArea rows={2} placeholder="Additional notes" />
        </Form.Item>

      </Form>
    </Modal>
  )
}
