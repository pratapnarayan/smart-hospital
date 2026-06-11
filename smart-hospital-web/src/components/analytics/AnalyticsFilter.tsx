import { DatePicker, Space, Button } from 'antd'
import { FilterOutlined, ReloadOutlined } from '@ant-design/icons'
import dayjs, { type Dayjs } from 'dayjs'
import { useSearchParams } from 'react-router-dom'

const { RangePicker } = DatePicker

export function AnalyticsFilter() {
  const [params, setParams] = useSearchParams()

  const from = params.get('from') ? dayjs(params.get('from')) : dayjs().subtract(29, 'day')
  const to = params.get('to') ? dayjs(params.get('to')) : dayjs()

  const handleChange = (dates: [Dayjs | null, Dayjs | null] | null) => {
    if (dates?.[0] && dates?.[1]) {
      const next = new URLSearchParams(params)
      next.set('from', dates[0].format('YYYY-MM-DD'))
      next.set('to', dates[1].format('YYYY-MM-DD'))
      setParams(next)
    }
  }

  const handleReset = () => {
    const next = new URLSearchParams(params)
    next.delete('from')
    next.delete('to')
    setParams(next)
  }

  return (
    <Space style={{ marginBottom: 16 }}>
      <FilterOutlined style={{ color: '#1677ff' }} />
      <RangePicker
        value={[from, to]}
        onChange={handleChange}
        format="DD MMM YYYY"
        allowClear={false}
        presets={[
          { label: 'Last 7 days', value: [dayjs().subtract(6, 'day'), dayjs()] },
          { label: 'Last 30 days', value: [dayjs().subtract(29, 'day'), dayjs()] },
          { label: 'Last 90 days', value: [dayjs().subtract(89, 'day'), dayjs()] },
          { label: 'This month', value: [dayjs().startOf('month'), dayjs()] },
          { label: 'This year', value: [dayjs().startOf('year'), dayjs()] },
        ]}
      />
      <Button icon={<ReloadOutlined />} onClick={handleReset} size="small">Reset</Button>
    </Space>
  )
}
