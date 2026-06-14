import { Modal, Form, Input, InputNumber, Select, Button, message } from 'antd'
import type { AxiosError } from 'axios'
import { useQuickRegisterPatient } from '@/hooks/useClinic'
import type { QuickRegisterRequest } from '@/types/clinic.types'

interface Props {
  open: boolean
  onClose: () => void
  onSuccess?: (patientId: string) => void
}

export function QuickRegisterModal({ open, onClose, onSuccess }: Props) {
  const [form] = Form.useForm<QuickRegisterRequest>()
  const { mutate, isPending } = useQuickRegisterPatient()

  const handleFinish = (values: QuickRegisterRequest) => {
    mutate(values, {
      onSuccess: (patient) => {
        message.success(`Patient "${patient.firstName} ${patient.lastName}" registered`)
        form.resetFields()
        onSuccess?.(patient.id)
        onClose()
      },
      onError: (err: unknown) => message.error((err as AxiosError<{ message?: string }>)?.response?.data?.message ?? 'Registration failed'),
    })
  }

  return (
    <Modal title="Quick Patient Registration" open={open} onCancel={onClose} footer={null} destroyOnClose>
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item name="name" label="Patient Name" rules={[{ required: true, message: 'Name is required' }]}>
          <Input placeholder="Full name or single name" />
        </Form.Item>
        <Form.Item
          name="phone"
          label="Phone Number"
          rules={[{ required: true }, { pattern: /^\d{10}$/, message: 'Must be 10 digits' }]}
        >
          <Input placeholder="10-digit mobile number" />
        </Form.Item>
        <Form.Item name="age" label="Age (years)">
          <InputNumber min={0} max={150} style={{ width: '100%' }} placeholder="e.g. 35" />
        </Form.Item>
        <Form.Item name="gender" label="Gender" initialValue="OTHER">
          <Select
            options={[
              { value: 'MALE', label: 'Male' },
              { value: 'FEMALE', label: 'Female' },
              { value: 'OTHER', label: 'Other' },
            ]}
          />
        </Form.Item>
        <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
          <Button onClick={onClose} style={{ marginRight: 8 }}>Cancel</Button>
          <Button type="primary" htmlType="submit" loading={isPending}>Register</Button>
        </Form.Item>
      </Form>
    </Modal>
  )
}
