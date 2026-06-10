import { useEffect, useState } from 'react'
import { Modal, Form, Input, InputNumber, Select, Switch, Row, Col } from 'antd'
import { useCreateDoctor, useUpdateDoctor, useSpecializations } from '@/hooks/useDoctor'
import { useEmployees } from '@/hooks/useHr'
import type { DoctorProfile, DoctorProfileRequest } from '@/types'

interface Props {
  open: boolean
  onClose: () => void
  doctor?: DoctorProfile
  employeeId?: string        // pre-filled when launched from HR employee page
  employeeName?: string      // display label when employeeId is pre-filled
}

export function DoctorProfileModal({ open, onClose, doctor, employeeId, employeeName }: Props) {
  const [form] = Form.useForm<DoctorProfileRequest>()
  const isEdit = !!doctor
  const prefilledEmployee = !!employeeId && !isEdit

  const [employeeSearch, setEmployeeSearch]             = useState('')
  const { data: specs = [] }                            = useSpecializations()
  const { data: employeesPage }                         = useEmployees(undefined, employeeSearch || undefined, 0)
  const { mutate: create, isPending: creating }         = useCreateDoctor()
  const { mutate: update, isPending: updating }         = useUpdateDoctor(doctor?.id ?? '')

  useEffect(() => {
    if (open) {
      if (doctor) {
        form.setFieldsValue({
          ...doctor,
          specializationIds: doctor.specializations.map(s => s.id),
        })
      } else {
        form.resetFields()
        form.setFieldsValue({
          onlineBookingEnabled: true,
          displayOnPortal: true,
          ...(employeeId ? { employeeId } : {}),
        })
      }
    }
  }, [open, doctor, employeeId, form])

  const onFinish = (values: DoctorProfileRequest) => {
    if (isEdit) {
      update(values, { onSuccess: onClose })
    } else {
      create(values, { onSuccess: onClose })
    }
  }

  return (
    <Modal
      title={isEdit ? 'Edit Doctor Profile' : 'Create Doctor Profile'}
      open={open}
      onCancel={onClose}
      onOk={() => form.submit()}
      okText={isEdit ? 'Save' : 'Create'}
      confirmLoading={creating || updating}
      width={720}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" onFinish={onFinish}>
        {!isEdit && (
          <Form.Item name="employeeId" label="Employee" rules={[{ required: true, message: 'Select an employee' }]}>
            {prefilledEmployee ? (
              <Input disabled value={employeeName ?? employeeId} />
            ) : (
              <Select
                showSearch
                placeholder="Type to search employee…"
                filterOption={false}
                onSearch={setEmployeeSearch}
                options={(employeesPage?.content ?? []).map(e => ({
                  value: e.id,
                  label: `${e.employeeCode} — ${e.firstName} ${e.lastName}`,
                }))}
              />
            )}
          </Form.Item>
        )}

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="qualifications" label="Qualifications">
              <Input placeholder="MBBS, MD, etc." />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="experienceYears" label="Experience (years)">
              <InputNumber style={{ width: '100%' }} min={0} />
            </Form.Item>
          </Col>
        </Row>

        <Form.Item name="specializationIds" label="Specializations">
          <Select
            mode="multiple"
            placeholder="Select specializations"
            options={specs.map(s => ({ value: s.id, label: s.name }))}
          />
        </Form.Item>

        <Row gutter={16}>
          <Col span={8}>
            <Form.Item name="consultationFee" label="Consultation Fee (₹)">
              <InputNumber style={{ width: '100%' }} min={0} precision={2} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="followUpFee" label="Follow-up Fee (₹)">
              <InputNumber style={{ width: '100%' }} min={0} precision={2} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="teleConsultationFee" label="Tele-consultation Fee (₹)">
              <InputNumber style={{ width: '100%' }} min={0} precision={2} />
            </Form.Item>
          </Col>
        </Row>

        <Form.Item name="languages" label="Languages Spoken">
          <Input placeholder="English, Hindi, Marathi" />
        </Form.Item>

        <Form.Item name="biography" label="Biography / About">
          <Input.TextArea rows={3} />
        </Form.Item>

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="awards" label="Awards & Recognition">
              <Input.TextArea rows={2} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="achievements" label="Achievements">
              <Input.TextArea rows={2} />
            </Form.Item>
          </Col>
        </Row>

        <Form.Item name="publications" label="Publications">
          <Input.TextArea rows={2} />
        </Form.Item>

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="onlineBookingEnabled" label="Online Booking" valuePropName="checked">
              <Switch />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="displayOnPortal" label="Display on Portal" valuePropName="checked">
              <Switch />
            </Form.Item>
          </Col>
        </Row>
      </Form>
    </Modal>
  )
}
