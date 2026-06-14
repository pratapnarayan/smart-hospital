import { Modal, Form, Select, Input, Button, message } from 'antd'
import type { AxiosError } from 'axios'
import { useUpdateHomeCollectionStatus } from '@/hooks/useClinic'
import type { HomeCollection, CollectionStatus } from '@/types/clinic.types'

const TRANSITIONS: Record<CollectionStatus, CollectionStatus[]> = {
  SCHEDULED: ['EN_ROUTE', 'CANCELLED'],
  EN_ROUTE: ['COLLECTED', 'FAILED', 'CANCELLED'],
  COLLECTED: [],
  CANCELLED: [],
  FAILED: [],
}

interface Props {
  collection: HomeCollection | null
  onClose: () => void
}

export function HomeCollectionStatusModal({ collection, onClose }: Props) {
  const [form] = Form.useForm()
  const { mutate, isPending } = useUpdateHomeCollectionStatus()
  const allowed = collection ? TRANSITIONS[collection.status] : []
  const watchedStatus = Form.useWatch('status', form) as CollectionStatus | undefined

  const handleFinish = (values: { status: CollectionStatus; failureReason?: string }) => {
    if (!collection) return
    mutate(
      { id: collection.id, data: { status: values.status, failureReason: values.failureReason } },
      {
        onSuccess: () => {
          message.success('Status updated')
          form.resetFields()
          onClose()
        },
        onError: (err: unknown) => message.error((err as AxiosError<{ message?: string }>)?.response?.data?.message ?? 'Update failed'),
      }
    )
  }

  return (
    <Modal title="Update Collection Status" open={!!collection} onCancel={onClose} footer={null} destroyOnClose>
      {collection && (
        <Form form={form} layout="vertical" onFinish={handleFinish}>
          <Form.Item name="status" label="New Status" rules={[{ required: true }]}>
            <Select options={allowed.map(s => ({ value: s, label: s.replace('_', ' ') }))} />
          </Form.Item>
          {watchedStatus === 'FAILED' && (
            <Form.Item
              name="failureReason"
              label="Failure Reason"
              rules={[{ required: true, message: 'Reason is required for FAILED status' }]}
            >
              <Input.TextArea rows={3} placeholder="Describe why collection failed" />
            </Form.Item>
          )}
          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Button onClick={onClose} style={{ marginRight: 8 }}>Cancel</Button>
            <Button type="primary" htmlType="submit" loading={isPending} disabled={allowed.length === 0}>
              Update
            </Button>
          </Form.Item>
        </Form>
      )}
    </Modal>
  )
}
