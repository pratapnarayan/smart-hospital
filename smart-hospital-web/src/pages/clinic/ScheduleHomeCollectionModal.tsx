import { Modal, Form, Input, DatePicker, Button, message } from 'antd'
import type { AxiosError } from 'axios'
import dayjs from 'dayjs'
import { PatientSearchSelect } from '@/components/common/PatientSearchSelect'
import { StaffSearchSelect } from '@/components/common/StaffSearchSelect'
import { useCreateHomeCollection } from '@/hooks/useClinic'
import type { HomeCollectionCreateRequest } from '@/types/clinic.types'
import type { Patient, Employee } from '@/types'

interface ScheduleFormValues {
  patientId: string
  patientName: string
  patientPhone: string
  address: string
  scheduledAt: dayjs.Dayjs
  technicianId?: string
  technicianName?: string
  notes?: string
}

interface Props {
  open: boolean
  onClose: () => void
}

export function ScheduleHomeCollectionModal({ open, onClose }: Props) {
  const [form] = Form.useForm()
  const { mutate, isPending } = useCreateHomeCollection()

  const handleFinish = (values: ScheduleFormValues) => {
    const req: HomeCollectionCreateRequest = {
      patientId: values.patientId,
      patientName: values.patientName,
      patientPhone: values.patientPhone,
      address: values.address,
      scheduledAt: (values.scheduledAt as dayjs.Dayjs).toISOString(),
      technicianId: values.technicianId,
      technicianName: values.technicianName,
      notes: values.notes,
    }
    mutate(req, {
      onSuccess: () => {
        message.success('Home collection scheduled')
        form.resetFields()
        onClose()
      },
      onError: (err: unknown) => message.error((err as AxiosError<{ message?: string }>)?.response?.data?.message ?? 'Failed to schedule'),
    })
  }

  return (
    <Modal title="Schedule Home Collection" open={open} onCancel={onClose} footer={null} width={600} destroyOnClose>
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item name="patientId" label="Patient" rules={[{ required: true, message: 'Select a patient' }]}>
          <PatientSearchSelect
            valueMode="id"
            onPatientSelect={(p: Patient) => {
              form.setFieldsValue({
                patientName: `${p.firstName} ${p.lastName}`,
                patientPhone: p.mobile ?? '',
              })
            }}
          />
        </Form.Item>
        <Form.Item name="patientName" hidden><Input /></Form.Item>
        <Form.Item name="patientPhone" hidden><Input /></Form.Item>

        <Form.Item name="address" label="Collection Address" rules={[{ required: true }]}>
          <Input.TextArea rows={2} placeholder="Full address for sample pickup" />
        </Form.Item>
        <Form.Item name="scheduledAt" label="Scheduled Date & Time" rules={[{ required: true }]}>
          <DatePicker showTime style={{ width: '100%' }} disabledDate={d => d.isBefore(dayjs(), 'day')} />
        </Form.Item>
        <Form.Item name="technicianName" label="Technician (optional)">
          <StaffSearchSelect
            placeholder="Search technician..."
            onEmployeeSelect={(e: Employee) => form.setFieldValue('technicianId', e.id)}
          />
        </Form.Item>
        <Form.Item name="technicianId" hidden><Input /></Form.Item>
        <Form.Item name="notes" label="Notes">
          <Input.TextArea rows={2} placeholder="Any special instructions" />
        </Form.Item>
        <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
          <Button onClick={onClose} style={{ marginRight: 8 }}>Cancel</Button>
          <Button type="primary" htmlType="submit" loading={isPending}>Schedule</Button>
        </Form.Item>
      </Form>
    </Modal>
  )
}
