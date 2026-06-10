import { useState } from 'react'
import { Card, Table, Button, Modal, Form, Input, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined, EditOutlined } from '@ant-design/icons'
import { PageHeader } from '@/components/common/PageHeader'
import { useSpecializations, useCreateSpecialization, useUpdateSpecialization } from '@/hooks/useDoctor'
import { useAuthStore } from '@/store/authStore'
import type { Specialization, SpecializationRequest } from '@/types'

export function SpecializationsPage() {
  const { hasPermission }                             = useAuthStore()
  const [editItem, setEditItem]                       = useState<Specialization | null>(null)
  const [formOpen, setFormOpen]                       = useState(false)
  const [form]                                        = Form.useForm<SpecializationRequest>()
  const { data: specs = [], isLoading }               = useSpecializations()
  const { mutate: create, isPending: creating }       = useCreateSpecialization()
  const { mutate: update, isPending: updating }       = useUpdateSpecialization(editItem?.id ?? '')

  const openCreate = () => { setEditItem(null); form.resetFields(); setFormOpen(true) }
  const openEdit   = (s: Specialization) => {
    setEditItem(s)
    form.setFieldsValue({ name: s.name, code: s.code, description: s.description })
    setFormOpen(true)
  }
  const handleOk = (values: SpecializationRequest) => {
    const cb = { onSuccess: () => { setFormOpen(false); form.resetFields() } }
    editItem ? update(values, cb) : create(values, cb)
  }

  const columns: ColumnsType<Specialization> = [
    { title: 'Name',        dataIndex: 'name' },
    {
      title: 'Code', dataIndex: 'code', width: 100,
      render: (v: string) => <Tag>{v}</Tag>,
    },
    {
      title: 'Description', dataIndex: 'description',
      render: (v?: string) => v ?? '—',
      ellipsis: true,
    },
    {
      title: 'Active', dataIndex: 'active', width: 80,
      render: (v: boolean) => <Tag color={v ? 'success' : 'default'}>{v ? 'Yes' : 'No'}</Tag>,
    },
    ...(hasPermission('DOCTOR.MANAGE') ? [{
      title: '', key: 'actions', width: 80,
      render: (_: unknown, r: Specialization) => (
        <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(r)}>Edit</Button>
      ),
    }] : []),
  ]

  return (
    <>
      <PageHeader
        title="Specializations"
        subtitle="Manage medical specialization categories"
        extra={
          hasPermission('DOCTOR.MANAGE') ? (
            <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
              Add Specialization
            </Button>
          ) : undefined
        }
      />
      <Card>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={specs}
          loading={isLoading}
          pagination={false}
        />
      </Card>

      <Modal
        title={editItem ? 'Edit Specialization' : 'Add Specialization'}
        open={formOpen}
        onCancel={() => setFormOpen(false)}
        onOk={() => form.submit()}
        okText={editItem ? 'Save' : 'Add'}
        confirmLoading={creating || updating}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" onFinish={handleOk}>
          <Form.Item name="name" label="Name" rules={[{ required: true }]}>
            <Input placeholder="e.g. Cardiology" />
          </Form.Item>
          <Form.Item name="code" label="Code" rules={[{ required: true }]}>
            <Input placeholder="e.g. CARDIO" />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
