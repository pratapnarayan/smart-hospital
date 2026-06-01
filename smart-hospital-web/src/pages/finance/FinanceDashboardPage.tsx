import { Card, Row, Col, Statistic, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons'
import { PageHeader } from '@/components/common'
import { useFinanceDashboard } from '@/hooks/useFinance'

const SOURCE_COLOR: Record<string, string> = {
  OPD: 'blue', IPD: 'purple', PHARMACY: 'cyan',
  PATHOLOGY: 'orange', RADIOLOGY: 'geekblue', OTHER: 'default',
}

const fmt = (v: number) =>
  `₹${Number(v ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`

export function FinanceDashboardPage() {
  const { data: dash, isLoading } = useFinanceDashboard()

  const incomeColumns: ColumnsType<{ source: string; amount: number }> = [
    {
      title: 'Source',
      dataIndex: 'source',
      render: (v: string) => <Tag color={SOURCE_COLOR[v] ?? 'default'}>{v}</Tag>,
    },
    {
      title: 'Amount',
      dataIndex: 'amount',
      align: 'right',
      render: fmt,
    },
  ]

  const expenseColumns: ColumnsType<{ category: string; amount: number }> = [
    { title: 'Category', dataIndex: 'category' },
    { title: 'Amount', dataIndex: 'amount', align: 'right', render: fmt },
  ]

  const todayNet   = (dash?.todayNet   ?? 0)
  const monthNet   = (dash?.monthNet   ?? 0)

  return (
    <>
      <PageHeader title="Finance Overview" subtitle="Today and month-to-date summary" />

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} md={4}>
          <Card loading={isLoading}>
            <Statistic
              title="Today — Income"
              value={fmt(dash?.todayIncome ?? 0)}
              valueStyle={{ color: '#52c41a' }}
              prefix={<ArrowUpOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={4}>
          <Card loading={isLoading}>
            <Statistic
              title="Today — Expenses"
              value={fmt(dash?.todayExpenses ?? 0)}
              valueStyle={{ color: '#ff4d4f' }}
              prefix={<ArrowDownOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={4}>
          <Card loading={isLoading}>
            <Statistic
              title="Today — Net"
              value={fmt(todayNet)}
              valueStyle={{ color: todayNet >= 0 ? '#1677ff' : '#ff4d4f' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={4}>
          <Card loading={isLoading}>
            <Statistic
              title="Last 30 Days — Income"
              value={fmt(dash?.monthIncome ?? 0)}
              valueStyle={{ color: '#52c41a' }}
              prefix={<ArrowUpOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={4}>
          <Card loading={isLoading}>
            <Statistic
              title="Last 30 Days — Expenses"
              value={fmt(dash?.monthExpenses ?? 0)}
              valueStyle={{ color: '#ff4d4f' }}
              prefix={<ArrowDownOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={4}>
          <Card loading={isLoading}>
            <Statistic
              title="Last 30 Days — Net"
              value={fmt(monthNet)}
              valueStyle={{ color: monthNet >= 0 ? '#1677ff' : '#ff4d4f' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col xs={24} md={12}>
          <Card title="Income by Source — Last 30 Days" loading={isLoading}>
            <Table
              rowKey="source"
              size="small"
              columns={incomeColumns}
              dataSource={dash?.monthIncomeBySource ?? []}
              pagination={false}
              locale={{ emptyText: 'No income recorded this month' }}
            />
          </Card>
        </Col>
        <Col xs={24} md={12}>
          <Card title="Expenses by Category — Last 30 Days" loading={isLoading}>
            <Table
              rowKey="category"
              size="small"
              columns={expenseColumns}
              dataSource={dash?.monthExpenseByCategory ?? []}
              pagination={false}
              locale={{ emptyText: 'No expenses recorded this month' }}
            />
          </Card>
        </Col>
      </Row>
    </>
  )
}
