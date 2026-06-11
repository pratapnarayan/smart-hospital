import ReactApexChart from 'react-apexcharts'
import { Row, Col, Card, Table, Tag } from 'antd'
import { WarningOutlined } from '@ant-design/icons'
import { PageHeader } from '@/components/common'
import { KpiCard, EmptyChart, AnalyticsFilter, ExportToolbar, baseChartOptions, chartPalette } from '@/components/analytics'
import { useInventoryAnalytics } from '@/hooks/useAnalytics'
import { withDemoFallback, DEMO_INVENTORY } from '@/hooks/useDemoData'
import type { LowStockEntry, InventoryAnalytics } from '@/types'

const fmt = (v: number) => `₹${Number(v ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`

export function InventoryAnalyticsPage() {
  const { data: raw, isLoading } = useInventoryAnalytics()
  const { data, isDemo } = withDemoFallback<InventoryAnalytics>(raw, DEMO_INVENTORY)

  const lowStockColumns = [
    { title: 'Item', dataIndex: 'itemName', key: 'itemName' },
    { title: 'Category', dataIndex: 'category', key: 'category' },
    { title: 'Current Stock', dataIndex: 'currentStock', key: 'stock', render: (v: number, r: LowStockEntry) => (
      <Tag color={v === 0 ? 'red' : v <= r.reorderLevel / 2 ? 'orange' : 'gold'}>{v}</Tag>
    )},
    { title: 'Reorder Level', dataIndex: 'reorderLevel', key: 'reorder' },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
        <PageHeader title="Inventory Analytics" subtitle="Stock value, movement and low stock alerts" />
        <ExportToolbar section="inventory" isDemoData={isDemo} />
      </div>
      <AnalyticsFilter />

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {[
          { title: 'Total Stock Value', value: fmt(data.totalStockValue) },
          { title: 'Total Items', value: data.totalItems?.toLocaleString() ?? '0' },
          { title: 'Low Stock Items', value: data.lowStockItems?.toString() ?? '0' },
          { title: 'Out of Stock', value: data.outOfStockItems?.toString() ?? '0' },
        ].map(k => <Col xs={24} sm={12} md={6} key={k.title}><KpiCard {...k} loading={isLoading} /></Col>)}
      </Row>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} lg={16}>
          <Card title="Stock Value Trend">
            {data.stockValueTrend?.length ? (
              <ReactApexChart type="area" height={260}
                series={[{ name: 'Stock Value (₹)', data: data.stockValueTrend.map(p => p.value) }]}
                options={{ ...baseChartOptions, xaxis: { categories: data.stockValueTrend.map(p => p.label), labels: { rotate: -45, style: { fontSize: '10px' } } }, colors: ['#2f54eb'], fill: { type: 'gradient', gradient: { opacityFrom: 0.4, opacityTo: 0.05 } }, stroke: { curve: 'smooth', width: 2 } }} />
            ) : <EmptyChart height={260} />}
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="Stock by Category">
            {data.stockByCategory?.length ? (
              <ReactApexChart type="donut" height={260}
                series={data.stockByCategory.map(p => p.value)}
                options={{ ...baseChartOptions, labels: data.stockByCategory.map(p => p.name), colors: chartPalette, legend: { position: 'bottom' } }} />
            ) : <EmptyChart height={260} />}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} lg={12}>
          <Card title="Fast Moving Items (Top 10)">
            {data.fastMovingItems?.length ? (
              <ReactApexChart type="bar" height={240}
                series={[{ name: 'Units Issued', data: data.fastMovingItems.map(p => p.value) }]}
                options={{ ...baseChartOptions, plotOptions: { bar: { horizontal: true, borderRadius: 4 } }, xaxis: { categories: data.fastMovingItems.map(p => p.name) }, colors: ['#52c41a'] }} />
            ) : <EmptyChart height={240} />}
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Slow Moving Items">
            {data.slowMovingItems?.length ? (
              <ReactApexChart type="bar" height={240}
                series={[{ name: 'Units Issued', data: data.slowMovingItems.map(p => p.value) }]}
                options={{ ...baseChartOptions, plotOptions: { bar: { horizontal: true, borderRadius: 4 } }, xaxis: { categories: data.slowMovingItems.map(p => p.name) }, colors: ['#ff4d4f'] }} />
            ) : <EmptyChart height={240} />}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24}>
          <Card title={<span><WarningOutlined style={{ color: '#faad14', marginRight: 8 }} />Low Stock Alert List</span>}>
            <Table
              dataSource={data.lowStockList ?? []}
              columns={lowStockColumns}
              rowKey="itemName"
              size="small"
              pagination={{ pageSize: 10 }}
              loading={isLoading}
              locale={{ emptyText: 'All items are sufficiently stocked' }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}
