export type ClinicType = 'FULL_HMS' | 'CLINIC_OPD'

export interface TenantClinicConfig {
  clinicType: ClinicType
}

export type CollectionStatus = 'SCHEDULED' | 'EN_ROUTE' | 'COLLECTED' | 'CANCELLED' | 'FAILED'

export interface HomeCollection {
  id: string
  patientId: string
  patientName: string
  patientPhone?: string
  address: string
  scheduledAt: string
  collectedAt?: string
  technicianId?: string
  technicianName?: string
  status: CollectionStatus
  failureReason?: string
  notes?: string
  createdAt: string
}

export interface HomeCollectionSummary {
  total: number
  byStatus: Record<string, number>
  collections: HomeCollection[]
}

export interface HomeCollectionCreateRequest {
  patientId: string
  patientName: string
  patientPhone?: string
  address: string
  scheduledAt: string
  technicianId?: string
  technicianName?: string
  notes?: string
}

export interface HomeCollectionUpdateRequest {
  scheduledAt: string
  technicianId?: string
  technicianName?: string
  notes?: string
}

export interface HomeCollectionStatusRequest {
  status: CollectionStatus
  failureReason?: string
}

export type BillStatus = 'DRAFT' | 'FINALIZED' | 'CANCELLED'
export type LineType = 'CONSULTATION' | 'PATHOLOGY' | 'PHARMACY'

export interface ClinicBillItem {
  id: string
  lineType: LineType
  description: string
  amount: number
  sourceId?: string
}

export interface ClinicBill {
  id: string
  billNumber: string
  opdVisitId: string
  patientId: string
  patientName: string
  visitDate: string
  totalAmount: number
  status: BillStatus
  items: ClinicBillItem[]
  createdAt: string
}

export interface QuickRegisterRequest {
  name: string
  phone: string
  age?: number
  gender?: 'MALE' | 'FEMALE' | 'OTHER'
}
