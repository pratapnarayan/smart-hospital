export type EmploymentType   = 'FULL_TIME' | 'PART_TIME' | 'CONTRACT' | 'CONSULTANT'
export type EmployeeStatus   = 'ACTIVE' | 'ON_LEAVE' | 'SUSPENDED' | 'RESIGNED' | 'TERMINATED'
export type EmployeeGender   = 'MALE' | 'FEMALE' | 'OTHER'
export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'HALF_DAY' | 'ON_LEAVE' | 'HOLIDAY'
export type LeaveType        = 'CASUAL' | 'SICK' | 'EARNED' | 'MATERNITY' | 'PATERNITY' | 'UNPAID'
export type LeaveStatus      = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'

export interface HrDepartment {
  id: string
  name: string
  code: string
  active: boolean
}

export interface Designation {
  id: string
  title: string
  departmentId?: string
  active: boolean
}

export interface Employee {
  id: string
  employeeCode: string
  firstName: string
  lastName: string
  dateOfBirth?: string
  gender?: EmployeeGender
  mobile?: string
  email?: string
  address?: string
  bloodGroup?: string
  departmentId?: string
  designationId?: string
  userId?: string
  employmentType: EmploymentType
  joinDate: string
  status: EmployeeStatus
  profilePhoto?: string
  createdAt: string
}

export interface AttendanceRecord {
  id: string
  employeeId: string
  attendanceDate: string
  checkIn?: string
  checkOut?: string
  status: AttendanceStatus
  notes?: string
  createdAt: string
}

export interface LeaveRequest {
  id: string
  leaveNumber: string
  employeeId: string
  employeeName: string
  leaveType: LeaveType
  fromDate: string
  toDate: string
  totalDays: number
  reason?: string
  status: LeaveStatus
  approvedById?: string
  approverNotes?: string
  createdAt: string
}

export interface HrDashboard {
  totalEmployees: number
  activeEmployees: number
  presentToday: number
  absentToday: number
  onLeaveToday: number
  pendingLeaveRequests: number
}

export interface CreateEmployeePayload {
  firstName: string
  lastName: string
  joinDate: string
  dateOfBirth?: string
  gender?: EmployeeGender
  mobile?: string
  email?: string
  address?: string
  bloodGroup?: string
  departmentId?: string
  designationId?: string
  employmentType?: EmploymentType
}

export interface ApplyLeavePayload {
  employeeId: string
  leaveType: LeaveType
  fromDate: string
  toDate: string
  reason?: string
}
