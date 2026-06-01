export type RadiologyOrderStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
export type RadiologyPaymentStatus = 'PENDING' | 'PAID' | 'PARTIAL' | 'WAIVED'
export type RadiologyPriority = 'ROUTINE' | 'URGENT' | 'STAT'
export type RadiologyItemStatus = 'PENDING' | 'IN_PROGRESS' | 'REPORTED'

export interface RadiologyOrderItem {
  id: string
  studyId: string
  studyCode: string
  studyName: string
  modalityName: string
  price: number
  prepInstructions?: string
  status: RadiologyItemStatus
  findings?: string
  impression?: string
  reportedAt?: string
  reportedBy?: string
}

export interface RadiologyOrder {
  id: string
  orderNumber: string
  patientId: string
  patientName: string
  patientMobile?: string
  referredById?: string
  referredByName?: string
  sourceType: 'OPD' | 'IPD' | 'WALK_IN'
  sourceId?: string
  priority: RadiologyPriority
  status: RadiologyOrderStatus
  scheduledAt?: string
  clinicalHistory?: string
  totalAmount: number
  discount: number
  netAmount: number
  paymentStatus: RadiologyPaymentStatus
  notes?: string
  items: RadiologyOrderItem[]
  createdAt: string
}

export interface RadiologyDashboard {
  pendingOrders: number
  scheduledOrders: number
  inProgressOrders: number
  completedOrders: number
  totalStudies: number
}
