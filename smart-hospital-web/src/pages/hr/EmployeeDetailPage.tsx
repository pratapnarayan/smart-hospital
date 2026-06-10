import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { Card, Descriptions, Tag, Button, Tabs, Spin, Alert, Table, Empty, Typography } from 'antd'
import type { TableProps } from 'antd'
import { EditOutlined } from '@ant-design/icons'
import { PageHeader } from '@/components/common/PageHeader'
import {
  useEmployee, useEmployeeAttendance, useEmployeeLeaves,
  useHrDepartments, useDesignations,
} from '@/hooks/useHr'
import { EmployeeFormModal } from './EmployeeFormModal'
import { formatDate, formatDateTime, calcAge } from '@/utils'
import type { AttendanceRecord, AttendanceStatus, LeaveRequest, LeaveStatus } from '@/types'

const ATTENDANCE_COLOR: Record<AttendanceStatus, string> = {
  PRESENT: 'success', ABSENT: 'error', HALF_DAY: 'warning', ON_LEAVE: 'orange', HOLIDAY: 'default',
}
const LEAVE_COLOR: Record<LeaveStatus, string> = {
  PENDING: 'processing', APPROVED: 'success', REJECTED: 'error', CANCELLED: 'default',
}
const EMP_STATUS_COLOR: Record<string, string> = {
  ACTIVE: 'success', ON_LEAVE: 'warning', SUSPENDED: 'error', RESIGNED: 'default', TERMINATED: 'default',
}

export function EmployeeDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [editOpen, setEditOpen] = useState(false)

  const { data: employee, isLoading, isError } = useEmployee(id!)
  const { data: attendance = [] }              = useEmployeeAttendance(id!)
  const { data: leavesPage }                   = useEmployeeLeaves(id!)
  const { data: depts = [] }                   = useHrDepartments()
  const { data: designations = [] }            = useDesignations()

  if (isLoading) return <Spin size="large" className="flex justify-center mt-20" />
  if (isError || !employee) return <Alert type="error" message="Employee not found" />

  const deptName  = depts.find(d => d.id === employee.departmentId)?.name ?? '—'
  const desigTitle = designations.find(d => d.id === employee.designationId)?.title ?? '—'

  const attendanceCols: TableProps<AttendanceRecord>['columns'] = [
    { title: 'Date',     dataIndex: 'attendanceDate', render: formatDate, width: 120 },
    { title: 'Check In',  dataIndex: 'checkIn',  render: (v?: string) => v ?? '—', width: 110 },
    { title: 'Check Out', dataIndex: 'checkOut', render: (v?: string) => v ?? '—', width: 110 },
    {
      title: 'Status', dataIndex: 'status', width: 120,
      render: (v: AttendanceStatus) => <Tag color={ATTENDANCE_COLOR[v]}>{v.replace('_', ' ')}</Tag>,
    },
    { title: 'Notes', dataIndex: 'notes', render: (v?: string) => v ?? '—', ellipsis: true },
  ]

  const leaveCols: TableProps<LeaveRequest>['columns'] = [
    { title: 'Leave #',   dataIndex: 'leaveNumber', width: 140 },
    { title: 'Type',      dataIndex: 'leaveType', width: 110,
      render: (v: string) => <Tag>{v.replace('_', ' ')}</Tag> },
    { title: 'From',      dataIndex: 'fromDate', render: formatDate, width: 110 },
    { title: 'To',        dataIndex: 'toDate',   render: formatDate, width: 110 },
    { title: 'Days',      dataIndex: 'totalDays', width: 70 },
    { title: 'Reason',    dataIndex: 'reason', render: (v?: string) => v ?? '—', ellipsis: true },
    {
      title: 'Status', dataIndex: 'status', width: 110,
      render: (v: LeaveStatus) => <Tag color={LEAVE_COLOR[v]}>{v}</Tag>,
    },
    { title: 'Applied',   dataIndex: 'createdAt', render: formatDateTime, width: 170 },
  ]

  return (
    <div>
      <PageHeader
        title={`${employee.firstName} ${employee.lastName}`}
        subtitle={employee.employeeCode}
        breadcrumbs={[
          { title: 'HR — Employees', href: '/hr/employees' },
          { title: `${employee.firstName} ${employee.lastName}` },
        ]}
        extra={
          <Button icon={<EditOutlined />} onClick={() => setEditOpen(true)}>
            Edit
          </Button>
        }
      />

      <Card className="mb-4">
        <Descriptions column={{ xs: 1, sm: 2, lg: 3 }} bordered size="small">
          <Descriptions.Item label="Employee ID" span={3}>
            <Typography.Text code copyable={{ text: employee.id }}>{employee.id}</Typography.Text>
          </Descriptions.Item>
          <Descriptions.Item label="Employee Code">{employee.employeeCode}</Descriptions.Item>
          <Descriptions.Item label="Status">
            <Tag color={EMP_STATUS_COLOR[employee.status]}>{employee.status.replace('_', ' ')}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Employment Type">
            <Tag>{employee.employmentType.replace('_', ' ')}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Department">{deptName}</Descriptions.Item>
          <Descriptions.Item label="Designation">{desigTitle}</Descriptions.Item>
          <Descriptions.Item label="Gender">{employee.gender ?? '—'}</Descriptions.Item>
          <Descriptions.Item label="Date of Birth">
            {employee.dateOfBirth ? `${formatDate(employee.dateOfBirth)} (${calcAge(employee.dateOfBirth)})` : '—'}
          </Descriptions.Item>
          <Descriptions.Item label="Join Date">{formatDate(employee.joinDate)}</Descriptions.Item>
          <Descriptions.Item label="Blood Group">
            {employee.bloodGroup ? <Tag color="red">{employee.bloodGroup}</Tag> : '—'}
          </Descriptions.Item>
          <Descriptions.Item label="Mobile">{employee.mobile ?? '—'}</Descriptions.Item>
          <Descriptions.Item label="Email">{employee.email ?? '—'}</Descriptions.Item>
          <Descriptions.Item label="Address" span={3}>{employee.address ?? '—'}</Descriptions.Item>
          <Descriptions.Item label="Created">{formatDateTime(employee.createdAt)}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Tabs
        defaultActiveKey="attendance"
        items={[
          {
            key: 'attendance',
            label: `Attendance${attendance.length ? ` (${attendance.length})` : ''}`,
            children: (
              <Card>
                {!attendance.length ? (
                  <Empty description="No attendance records" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                ) : (
                  <Table
                    rowKey="id"
                    size="small"
                    dataSource={attendance}
                    columns={attendanceCols}
                    pagination={{ pageSize: 20 }}
                  />
                )}
              </Card>
            ),
          },
          {
            key: 'leaves',
            label: `Leave Requests${leavesPage?.total ? ` (${leavesPage.total})` : ''}`,
            children: (
              <Card>
                {!leavesPage?.content?.length ? (
                  <Empty description="No leave requests" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                ) : (
                  <Table
                    rowKey="id"
                    size="small"
                    dataSource={leavesPage.content}
                    columns={leaveCols}
                    pagination={false}
                  />
                )}
              </Card>
            ),
          },
        ]}
      />

      <EmployeeFormModal
        open={editOpen}
        onClose={() => setEditOpen(false)}
        employee={employee}
      />
    </div>
  )
}
