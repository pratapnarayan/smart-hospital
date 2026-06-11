import ReactApexChart from 'react-apexcharts'
import { Row, Col, Card } from 'antd'
import { PageHeader } from '@/components/common'
import { KpiCard, EmptyChart, AnalyticsFilter, ExportToolbar, baseChartOptions, chartPalette } from '@/components/analytics'
import { usePatientAnalytics } from '@/hooks/useAnalytics'
import { withDemoFallback, DEMO_PATIENTS } from '@/hooks/useDemoData'
import type { PatientAnalytics } from '@/types'

export function PatientAnalyticsPage() {
  const { data: raw, isLoading } = usePatientAnalytics()
  const { data, isDemo } = withDemoFallback<PatientAnalytics>(raw, DEMO_PATIENTS)

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
        <PageHeader title="Patient Analytics" subtitle="Demographics and patient growth insights" />
        <ExportToolbar section="patients" isDemoData={isDemo} />
      </div>
      <AnalyticsFilter />

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {[
          { title: 'Total Patients', value: data.totalPatients?.toLocaleString() ?? '0' },
          { title: 'New This Period', value: data.newPatientsThisPeriod?.toLocaleString() ?? '0' },
          { title: 'Returning Patients', value: data.returningPatients?.toLocaleString() ?? '0' },
          { title: 'Retention Rate', value: `${data.retentionRatePct?.toFixed(1) ?? '0'}%` },
        ].map(k => <Col xs={24} sm={12} md={6} key={k.title}><KpiCard {...k} loading={isLoading} /></Col>)}
      </Row>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} lg={16}>
          <Card title="Patient Registration Trend">
            {data.registrationTrend?.length ? (
              <ReactApexChart type="area" height={260}
                series={[{ name: 'New Patients', data: data.registrationTrend.map(p => p.value) }]}
                options={{ ...baseChartOptions, xaxis: { categories: data.registrationTrend.map(p => p.label) }, colors: ['#52c41a'], fill: { type: 'gradient', gradient: { opacityFrom: 0.4, opacityTo: 0.05 } }, stroke: { curve: 'smooth', width: 2 } }} />
            ) : <EmptyChart height={260} />}
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="Gender Distribution">
            {data.genderDistribution?.length ? (
              <ReactApexChart type="pie" height={260}
                series={data.genderDistribution.map(p => p.value)}
                options={{ ...baseChartOptions, labels: data.genderDistribution.map(p => p.name), colors: ['#1677ff', '#ff4d4f', '#faad14'], legend: { position: 'bottom' } }} />
            ) : <EmptyChart height={260} />}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="Age Distribution">
            {data.ageDistribution?.length ? (
              <ReactApexChart type="bar" height={240}
                series={[{ name: 'Patients', data: data.ageDistribution.map(p => p.value) }]}
                options={{ ...baseChartOptions, xaxis: { categories: data.ageDistribution.map(p => p.name) }, colors: ['#13c2c2'], plotOptions: { bar: { borderRadius: 4 } } }} />
            ) : <EmptyChart height={240} />}
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Blood Group Distribution">
            {data.bloodGroupDistribution?.length ? (
              <ReactApexChart type="bar" height={240}
                series={[{ name: 'Patients', data: data.bloodGroupDistribution.map(p => p.value) }]}
                options={{ ...baseChartOptions, xaxis: { categories: data.bloodGroupDistribution.map(p => p.name) }, colors: ['#ff4d4f'], plotOptions: { bar: { borderRadius: 4 } } }} />
            ) : <EmptyChart height={240} />}
          </Card>
        </Col>
      </Row>
    </div>
  )
}
