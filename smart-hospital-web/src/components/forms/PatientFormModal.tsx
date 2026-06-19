import { useEffect } from 'react'
import { Modal, Row, Col, Button, Form, Divider } from 'antd'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useCreatePatient, useUpdatePatient } from '@/hooks/usePatients'
import { FormField } from './FormField'
import {
  UserOutlined, PhoneOutlined, TeamOutlined,
} from '@ant-design/icons'
import type { Patient } from '@/types'

const patientSchema = z.object({
  firstName: z.string().trim().min(1, 'First name is required').max(50, 'Too long'),
  lastName: z.string().trim().min(1, 'Last name is required').max(50, 'Too long'),
  dateOfBirth: z.string().min(1, 'Date of birth is required'),
  gender: z.enum(['MALE', 'FEMALE', 'OTHER'], { required_error: 'Gender is required' }),
  mobile: z
    .string()
    .regex(/^[0-9]{10,15}$/, 'Invalid mobile number')
    .optional()
    .or(z.literal('')),
  bloodGroup: z
    .enum(['A+', 'A-', 'B+', 'B-', 'O+', 'O-', 'AB+', 'AB-'])
    .optional()
    .or(z.literal('')),
  address: z.string().max(500, 'Address too long').optional().or(z.literal('')),
  guardianName: z.string().max(100, 'Too long').optional().or(z.literal('')),
  // Note: Patient type does not include guardianMobile — only PatientCreateRequest does.
  // In edit mode this field will be empty (not pre-populated).
  guardianMobile: z
    .string()
    .regex(/^[0-9]{10,15}$/, 'Invalid mobile number')
    .optional()
    .or(z.literal('')),
})

type PatientFormData = z.infer<typeof patientSchema>

interface Props {
  open: boolean
  onClose: () => void
  patient?: Patient
}

const bloodGroupOptions = ['A+', 'A-', 'B+', 'B-', 'O+', 'O-', 'AB+', 'AB-'].map((v) => ({
  label: v,
  value: v,
}))

const genderOptions = [
  { label: 'Male',   value: 'MALE'   },
  { label: 'Female', value: 'FEMALE' },
  { label: 'Other',  value: 'OTHER'  },
]

const defaultValues: PatientFormData = {
  firstName: '',
  lastName: '',
  dateOfBirth: '',
  gender: undefined as never,
  mobile: '',
  bloodGroup: '',
  address: '',
  guardianName: '',
  guardianMobile: '',
}

export function PatientFormModal({ open, onClose, patient }: Props) {
  const {
    control,
    handleSubmit,
    reset,
    formState: { isDirty },
  } = useForm<PatientFormData>({
    resolver: zodResolver(patientSchema),
    defaultValues,
  })

  const { mutate: create, isPending: creating } = useCreatePatient()
  const { mutate: update, isPending: updating } = useUpdatePatient(patient?.id ?? '')
  const isEdit = !!patient
  const isPending = creating || updating

  useEffect(() => {
    if (!open) return
    if (patient) {
      reset({
        firstName:      patient.firstName,
        lastName:       patient.lastName,
        dateOfBirth:    patient.dateOfBirth,
        gender:         patient.gender,
        mobile:         patient.mobile       ?? '',
        bloodGroup:     (patient.bloodGroup  ?? '') as PatientFormData['bloodGroup'],
        address:        patient.address      ?? '',
        guardianName:   patient.guardianName ?? '',
        // Patient type has no guardianMobile — leave empty in edit mode
        guardianMobile: '',
      })
    } else {
      reset(defaultValues)
    }
  }, [open, patient, reset])

  const onSubmit = (data: PatientFormData) => {
    const payload = { ...data }
    if (isEdit) {
      if (!payload.guardianMobile) delete payload.guardianMobile
      update(payload, { onSuccess: () => { reset(); onClose() } })
    } else {
      create(payload, { onSuccess: () => { reset(); onClose() } })
    }
  }

  const handleCancel = () => {
    if (isDirty && !window.confirm('You have unsaved changes. Discard them?')) return
    reset()
    onClose()
  }

  return (
    <Modal
      title={
        <div className="flex items-center gap-3">
          <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-primary-50">
            <UserOutlined className="text-primary-500 text-lg" />
          </div>
          <div>
            <h3 className="text-lg font-semibold m-0" style={{ color: 'var(--text-primary)' }}>
              {isEdit ? 'Edit Patient' : 'Register New Patient'}
            </h3>
            <p className="text-sm m-0" style={{ color: 'var(--text-muted)' }}>
              {isEdit ? 'Update patient information' : 'Fill in the details to register a new patient'}
            </p>
          </div>
        </div>
      }
      open={open}
      onCancel={handleCancel}
      footer={null}
      width={720}
      destroyOnClose
    >
      <Form layout="vertical" onFinish={handleSubmit(onSubmit)} className="mt-4">
        {/* Personal Information */}
        <div className="mb-4">
          <h4
            className="text-xs font-semibold uppercase tracking-wider mb-4 flex items-center gap-2"
            style={{ color: 'var(--text-secondary)' }}
          >
            <UserOutlined style={{ color: 'var(--color-primary-500)' }} />
            Personal Information
          </h4>
          <Row gutter={[16, 0]}>
            <Col span={12}>
              <FormField
                name="firstName" control={control} label="First Name"
                placeholder="Raj" prefix={<UserOutlined className="text-neutral-400" />}
              />
            </Col>
            <Col span={12}>
              <FormField
                name="lastName" control={control} label="Last Name"
                placeholder="Sharma" prefix={<UserOutlined className="text-neutral-400" />}
              />
            </Col>
          </Row>
          <Row gutter={[16, 0]}>
            <Col span={12}>
              <FormField
                name="dateOfBirth" control={control} label="Date of Birth"
                type="date"
              />
            </Col>
            <Col span={12}>
              <FormField
                name="gender" control={control} label="Gender"
                type="select" options={genderOptions} placeholder="Select gender" allowClear={false}
              />
            </Col>
          </Row>
        </div>

        <Divider className="my-4" />

        {/* Contact Information */}
        <div className="mb-4">
          <h4
            className="text-xs font-semibold uppercase tracking-wider mb-4 flex items-center gap-2"
            style={{ color: 'var(--text-secondary)' }}
          >
            <PhoneOutlined style={{ color: 'var(--color-primary-500)' }} />
            Contact Information
          </h4>
          <Row gutter={[16, 0]}>
            <Col span={12}>
              <FormField
                name="mobile" control={control} label="Mobile"
                type="tel" placeholder="9876543210"
                prefix={<PhoneOutlined className="text-neutral-400" />}
              />
            </Col>
            <Col span={12}>
              <FormField
                name="bloodGroup" control={control} label="Blood Group"
                type="select" options={bloodGroupOptions} placeholder="Select blood group"
              />
            </Col>
          </Row>
          <FormField
            name="address" control={control} label="Address"
            type="textarea" placeholder="House no, Street, City, State, PIN" rows={2}
          />
        </div>

        <Divider className="my-4" />

        {/* Guardian Information */}
        <div className="mb-4">
          <h4
            className="text-xs font-semibold uppercase tracking-wider mb-4 flex items-center gap-2"
            style={{ color: 'var(--text-secondary)' }}
          >
            <TeamOutlined style={{ color: 'var(--color-primary-500)' }} />
            Guardian Information
          </h4>
          <Row gutter={[16, 0]}>
            <Col span={12}>
              <FormField
                name="guardianName" control={control} label="Guardian Name"
                placeholder="Parent / Spouse name"
                prefix={<UserOutlined className="text-neutral-400" />}
              />
            </Col>
            <Col span={12}>
              <FormField
                name="guardianMobile" control={control} label="Guardian Mobile"
                type="tel" placeholder="9876500000"
                prefix={<PhoneOutlined className="text-neutral-400" />}
              />
            </Col>
          </Row>
        </div>

        {/* Footer */}
        <div className="flex justify-end gap-3 pt-4 border-t border-neutral-200">
          <Button onClick={handleCancel} size="large" className="rounded-lg px-6">
            Cancel
          </Button>
          <Button
            type="primary" htmlType="submit" loading={isPending}
            size="large" className="rounded-lg px-8 shadow-glow-primary"
          >
            {isEdit ? 'Save Changes' : 'Register Patient'}
          </Button>
        </div>
      </Form>
    </Modal>
  )
}
