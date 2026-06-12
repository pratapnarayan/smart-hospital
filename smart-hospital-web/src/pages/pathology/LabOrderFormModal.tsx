import { useEffect, useState } from 'react'
import { Modal, Form, Input, Select, Checkbox, Divider, Tag, Space } from 'antd'
import { useLabCategories, useLabTests, useCreateLabOrder } from '@/hooks/usePathology'
import { PatientSearchSelect, StaffSearchSelect } from '@/components/common'
import type { CreateLabOrderPayload, LabTest } from '@/types'

interface Props {
  open: boolean
  onClose: () => void
  patientId?: string
  sourceType?: string
  sourceId?: string
}

export function LabOrderFormModal({ open, onClose, patientId, sourceType, sourceId }: Props) {
  const [form] = Form.useForm<CreateLabOrderPayload>()
  const [selectedCategoryId, setSelectedCategoryId] = useState<string | undefined>()
  const [selectedTests, setSelectedTests] = useState<LabTest[]>([])

  const { data: categories = [] } = useLabCategories()
  const { data: tests = [] }      = useLabTests(selectedCategoryId)
  const { mutate: createOrder, isPending } = useCreateLabOrder()

  useEffect(() => {
    if (open) {
      form.resetFields()
      setSelectedTests([])
      setSelectedCategoryId(undefined)
      if (patientId)   form.setFieldValue('patientId', patientId)
      if (sourceType)  form.setFieldValue('sourceType', sourceType)
      if (sourceId)    form.setFieldValue('sourceId', sourceId)
    }
  }, [open, patientId, sourceType, sourceId, form])

  const toggleTest = (test: LabTest) => {
    setSelectedTests(prev =>
      prev.find(t => t.id === test.id)
        ? prev.filter(t => t.id !== test.id)
        : [...prev, test]
    )
  }

  const totalPrice = selectedTests.reduce((s, t) => s + Number(t.price), 0)

  const onFinish = (values: CreateLabOrderPayload) => {
    if (selectedTests.length === 0) {
      form.setFields([{ name: 'testIds', errors: ['Select at least one test'] }])
      return
    }
    createOrder({ ...values, testIds: selectedTests.map(t => t.id) },
      { onSuccess: () => { setSelectedTests([]); onClose() } })
  }

  return (
    <Modal
      title="New Lab Order"
      open={open}
      onCancel={onClose}
      onOk={() => form.submit()}
      okText="Create Order"
      confirmLoading={isPending}
      width={680}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" onFinish={onFinish}>
        <Form.Item name="patientId" label="Patient" rules={[{ required: true, message: 'Select a patient' }]}>
          <PatientSearchSelect
            valueMode="id"
            placeholder="Search patient by name or mobile…"
            disabled={!!patientId}
          />
        </Form.Item>

        <Space style={{ width: '100%' }} size={16}>
          <Form.Item name="sourceType" label="Source" style={{ width: 160 }} initialValue="WALK_IN">
            <Select options={[
              { value: 'WALK_IN', label: 'Walk-in' },
              { value: 'OPD',     label: 'OPD' },
              { value: 'IPD',     label: 'IPD' },
            ]} />
          </Form.Item>
          <Form.Item name="priority" label="Priority" style={{ width: 140 }} initialValue="ROUTINE">
            <Select options={[
              { value: 'ROUTINE', label: 'Routine' },
              { value: 'URGENT',  label: '⚡ Urgent' },
              { value: 'STAT',    label: '🚨 STAT' },
            ]} />
          </Form.Item>
          <Form.Item name="referredByName" label="Referred By" style={{ flex: 1 }}>
            <StaffSearchSelect isDoctor placeholder="Search doctor…" />
          </Form.Item>
        </Space>

        <Divider orientation="left">Select Tests</Divider>

        <Select
          placeholder="Filter by category"
          allowClear
          style={{ width: '100%', marginBottom: 12 }}
          options={categories.map(c => ({ value: c.id, label: c.name }))}
          onChange={setSelectedCategoryId}
        />

        <div style={{ maxHeight: 220, overflowY: 'auto', border: '1px solid #f0f0f0',
                      borderRadius: 6, padding: 8 }}>
          {tests.length === 0
            ? <div style={{ textAlign: 'center', color: '#999', padding: 16 }}>
                {selectedCategoryId ? 'No tests in this category' : 'Select a category to see tests'}
              </div>
            : tests.map(test => (
              <div key={test.id}
                style={{ display: 'flex', justifyContent: 'space-between',
                         alignItems: 'center', padding: '6px 4px',
                         borderBottom: '1px solid #f5f5f5', cursor: 'pointer' }}
                onClick={() => toggleTest(test)}>
                <Space>
                  <Checkbox checked={!!selectedTests.find(t => t.id === test.id)} />
                  <span><Tag>{test.code}</Tag> {test.name}</span>
                  {test.unit && <span style={{ color: '#999', fontSize: 12 }}>({test.unit})</span>}
                </Space>
                <span style={{ color: '#1677ff', fontWeight: 500 }}>₹{test.price}</span>
              </div>
            ))
          }
        </div>

        {selectedTests.length > 0 && (
          <div style={{ marginTop: 12, padding: '8px 12px', background: '#f6ffed',
                        borderRadius: 6, border: '1px solid #b7eb8f' }}>
            <strong>Selected ({selectedTests.length}):</strong>{' '}
            {selectedTests.map(t => <Tag key={t.id} color="green">{t.code}</Tag>)}
            <span style={{ float: 'right' }}>Total: <strong>₹{totalPrice.toLocaleString('en-IN')}</strong></span>
          </div>
        )}

        <Form.Item name="notes" label="Notes" style={{ marginTop: 12 }}>
          <Input.TextArea rows={2} placeholder="Special instructions" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
