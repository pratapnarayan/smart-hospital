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
    <Card loading={loading} styles={{ body: { padding: '16px 20px' } }}
      style={{ borderRadius: 12, boxShadow: '0 1px 8px rgba(0,0,0,0.06)', height: '100%' }}>
      <Typography.Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 6, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
        {title}
      </Typography.Text>
      <Typography.Title level={3} style={{ margin: 0, fontSize: 22, lineHeight: 1.2, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
        {prefix}{value}
      </Typography.Title>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 6, flexWrap: 'wrap' }}>
        {trendText && (
          <Tag color={trendColor} icon={trendIcon} style={{ fontSize: 11, margin: 0 }}>
            {trendText}
          </Tag>
        )}
        {subtitle && (
          <Typography.Text type="secondary" style={{ fontSize: 11 }}>
            {subtitle}
          </Typography.Text>
        )}
      </div>
    </Card>
  )
}
