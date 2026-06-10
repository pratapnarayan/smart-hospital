export interface Specialization {
  id: string
  name: string
  code: string
  description?: string
  active: boolean
}

export interface DoctorProfile {
  id: string               // doctor_profiles.id (use this for all doctor API calls)
  employeeId: string
  employeeCode: string
  firstName: string
  lastName: string
  gender?: string
  mobile?: string
  email?: string
  departmentId?: string
  designationId?: string
  status: string
  joinDate: string
  profilePhoto?: string
  biography?: string
  qualifications?: string
  experienceYears?: number
  consultationFee?: number
  followUpFee?: number
  teleConsultationFee?: number
  languages?: string
  awards?: string
  achievements?: string
  publications?: string
  onlineBookingEnabled: boolean
  displayOnPortal: boolean
  specializations: Specialization[]
}

export interface DoctorSchedule {
  id: string
  dayOfWeek: string
  shiftStart: string   // "HH:mm:ss"
  shiftEnd: string
  slotDurationMins: number
  active: boolean
}

export interface AvailableSlot {
  date: string
  slots: string[]
}

export interface DoctorDashboard {
  totalDoctors: number
  activeDoctors: number
  availableToday: number
  totalSpecializations: number
}

export interface DoctorProfileRequest {
  employeeId?: string
  profilePhoto?: string
  biography?: string
  qualifications?: string
  experienceYears?: number
  consultationFee?: number
  followUpFee?: number
  teleConsultationFee?: number
  languages?: string
  awards?: string
  achievements?: string
  publications?: string
  onlineBookingEnabled?: boolean
  displayOnPortal?: boolean
  specializationIds?: string[]
}

export interface SpecializationRequest {
  name: string
  code: string
  description?: string
}

export interface DoctorScheduleRequest {
  dayOfWeek: string
  shiftStart: string
  shiftEnd: string
  slotDurationMins: number
}
