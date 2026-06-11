import { Card, Typography, Tag } from 'antd'
import { ArrowUpOutlined, ArrowDownOutlined, MinusOutlined } from '@ant-design/icons'

interface KpiCardProps {
  title: string
  value: string | number
  trend?: number | null  // % change, positive = up, negative = down, null = unknown
  subtitle?: string
  loading?: boolean
  prefix?: string
}

export function KpiCard({ title, value, trend, subtitle, loading, prefix = '' }: KpiCardProps) {
  const trendColor = trend == null ? 'default'
    : trend > 2 ? 'success'
    : trend < -2 ? 'error'
    : 'warning'

  const trendIcon = trend == null ? <MinusOutlined />
    : trend >= 0 ? <ArrowUpOutlined />
    : <ArrowDownOutlined />

  const trendText = trend != null ? `${Math.abs(trend).toFixed(1)}%` : null

  return (
    <Card loading={loading} bodyStyle={{ padding: '20px 24px' }}
      style={{ borderRadius: 12, boxShadow: '0 1px 8px rgba(0,0,0,0.06)' }}>
      <Typography.Text type="secondary" style={{ fontSize: 13 }}>{title}</Typography.Text>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 8 }}>
        <Typography.Title level={3} style={{ margin: 0, fontSize: 26 }}>
          {prefix}{value}
        </Typography.Title>
        {trendText && (
          <Tag color={trendColor} icon={trendIcon} style={{ fontSize: 12 }}>
            {trendText}
          </Tag>
        )}
      </div>
      {subtitle && (
        <Typography.Text type="secondary" style={{ fontSize: 11, marginTop: 4, display: 'block' }}>
          {subtitle}
        </Typography.Text>
      )}
    </Card>
  )
}
