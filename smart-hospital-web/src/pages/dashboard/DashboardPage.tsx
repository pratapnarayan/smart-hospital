import { Row, Col, Card, Statistic, Alert, Spin, Table, Tag } from 'antd'
import {
  UserOutlined, MedicineBoxOutlined, ShopOutlined, WarningOutlined,
} from '@ant-design/icons'
import { useAuthStore } from '@/store/authStore'
import { useLowStockMedicines, useExpiringBatches } from '@/hooks/usePharmacy'
import { useVisitsByDate } from '@/hooks/useOpdVisits'
import { usePatients } from '@/hooks/usePatients'
import { PageHeader } from '@/components/common/PageHeader'
import { formatDate, formatCurrency } from '@/utils'
import type { OpdVisit, VisitStatus } from '@/types'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'

const VISIT_STATUS_COLOR: Record<VisitStatus, string> = {
  REGISTERED:  'default',
  IN_PROGRESS: 'processing',
  COMPLETED:   'success',
  CANCELLED:   'error',
}

const recentColumns: ColumnsType<OpdVisit> = [
  { title: 'Visit No.',  dataIndex: 'visitNumber',  width: 130 },
  { title: 'Patient',    dataIndex: 'patientName' },
  { title: 'Doctor',     dataIndex: 'doctorName',   render: (v?: string) => v ?? '—' },
  { title: 'Department', dataIndex: 'department',   render: (v?: string) => v ?? '—' },
  { title: 'Fee',        dataIndex: 'consultationFee', render: formatCurrency },
  {
    title: 'Status', dataIndex: 'visitStatus',
    render: (v: VisitStatus) => <Tag color={VISIT_STATUS_COLOR[v]}>{v.replace('_', ' ')}</Tag>,
  },
]

export function DashboardPage() {
  const { user } = useAuthStore()
  const today = dayjs().format('YYYY-MM-DD')

  const { data: todayVisits,  isLoading: loadingVisits }  = useVisitsByDate(today)
  const { data: patientsPage, isLoading: loadingPatients } = usePatients(undefined, 0, 1)
  const { data: lowStock,     isLoading: loadingLow }     = useLowStockMedicines()
  const { data: expiring,     isLoading: loadingExpiry }  = useExpiringBatches(30)

  return (
    <div>
      <PageHeader
        title={`Good ${getGreeting()}, ${user?.firstName ?? ''}!`}
        subtitle={`Today is ${dayjs().format('DD MMM YYYY')} · ${user?.tenantId}`}
      />

      {/* ── KPI cards ────────────────────────────────────────────────────── */}
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            {loadingVisits
              ? <Spin />
              : <Statistic
                  title="Today's OPD Visits"
                  value={todayVisits?.total ?? 0}
                  prefix={<MedicineBoxOutlined style={{ color: '#1677ff' }} />}
                  valueStyle={{ color: '#1677ff' }}
                />
            }
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            {loadingPatients
              ? <Spin />
              : <Statistic
                  title="Registered Patients"
                  value={patientsPage?.total ?? 0}
                  prefix={<UserOutlined style={{ color: '#52c41a' }} />}
                  valueStyle={{ color: '#52c41a' }}
                />
            }
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            {loadingLow
              ? <Spin />
              : <Statistic
                  title="Low Stock Medicines"
                  value={lowStock?.length ?? 0}
                  prefix={<ShopOutlined style={{ color: lowStock?.length ? '#ff4d4f' : '#52c41a' }} />}
                  valueStyle={{ color: lowStock?.length ? '#ff4d4f' : '#52c41a' }}
                />
            }
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            {loadingExpiry
              ? <Spin />
              : <Statistic
                  title="Batches Expiring (30d)"
                  value={expiring?.length ?? 0}
                  prefix={<WarningOutlined style={{ color: expiring?.length ? '#faad14' : '#52c41a' }} />}
                  valueStyle={{ color: expiring?.length ? '#faad14' : '#52c41a' }}
                />
            }
          </Card>
        </Col>
      </Row>

      {/* ── Alerts ───────────────────────────────────────────────────────── */}
      <div className="mt-4 space-y-2">
        {lowStock && lowStock.length > 0 && (
          <Alert
            type="error"
            showIcon
            message={`${lowStock.length} medicine(s) at or below reorder level`}
            description={lowStock.slice(0, 5).map((m) => m.name).join(', ')}
          />
        )}
        {expiring && expiring.length > 0 && (
          <Alert
            type="warning"
            showIcon
            message={`${expiring.length} batch(es) expiring within 30 days`}
            description={expiring.slice(0, 5)
              .map((b) => `${b.medicineName} — ${b.batchNumber} (expires ${formatDate(b.expiryDate)})`)
              .join(' · ')}
          />
        )}
        {!loadingLow && !loadingExpiry && !lowStock?.length && !expiring?.length && (
          <Alert type="success" showIcon message="All systems normal — no stock or expiry alerts." />
        )}
      </div>

      {/* ── Today's OPD Visits table ──────────────────────────────────────── */}
      <Row gutter={[16, 16]} className="mt-4">
        <Col span={24}>
          <Card
            title={`Today's OPD Visits — ${formatDate(today)}`}
            extra={<a href="/opd">View all</a>}
          >
            <Table
              rowKey="id"
              size="small"
              loading={loadingVisits}
              dataSource={todayVisits?.content ?? []}
              columns={recentColumns}
              pagination={false}
              locale={{ emptyText: 'No OPD visits registered today' }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}

function getGreeting(): string {
  const hour = new Date().getHours()
  if (hour < 12) return 'morning'
  if (hour < 17) return 'afternoon'
  return 'evening'
}
