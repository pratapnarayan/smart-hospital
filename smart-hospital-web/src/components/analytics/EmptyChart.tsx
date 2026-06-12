import { Empty } from 'antd'

interface EmptyChartProps {
  height?: number
  message?: string
}

export function EmptyChart({ height = 200, message = 'No data for selected period' }: EmptyChartProps) {
  return (
    <div style={{
      height,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: '#fafafa',
      borderRadius: 8,
    }}>
      <Empty description={message} image={Empty.PRESENTED_IMAGE_SIMPLE} />
    </div>
  )
}
