import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { DatePicker, Button, Tooltip } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { cn } from '@/utils/cn'

const { RangePicker } = DatePicker

type DatePreset = 'today' | '7d' | '30d' | '90d' | 'custom'

interface AnalyticsFilterProps {
  onChange?: (startDate: string, endDate: string) => void
  className?: string
}

const presets: { label: string; value: DatePreset; days: number }[] = [
  { label: 'Today',       value: 'today', days: 0  },
  { label: 'Last 7 Days', value: '7d',    days: 7  },
  { label: 'Last 30 Days',value: '30d',   days: 30 },
  { label: 'Last 90 Days',value: '90d',   days: 90 },
]

export function AnalyticsFilter({ onChange, className }: AnalyticsFilterProps) {
  const [, setSearchParams] = useSearchParams()
  const [activePreset, setActivePreset] = useState<DatePreset>('30d')
  const [customRange, setCustomRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null)

  const handlePresetClick = (preset: DatePreset, days: number) => {
    setActivePreset(preset)
    setCustomRange(null)
    const end   = dayjs()
    const start = days === 0 ? end : end.subtract(days, 'day')
    const s = start.format('YYYY-MM-DD')
    const e = end.format('YYYY-MM-DD')
    setSearchParams({ from: s, to: e })
    onChange?.(s, e)
  }

  const handleCustomRangeChange = (dates: [dayjs.Dayjs, dayjs.Dayjs] | null) => {
    if (dates) {
      setActivePreset('custom')
      setCustomRange(dates)
      const s = dates[0].format('YYYY-MM-DD')
      const e = dates[1].format('YYYY-MM-DD')
      setSearchParams({ from: s, to: e })
      onChange?.(s, e)
    }
  }

  const handleRefresh = () => {
    if (activePreset !== 'custom') {
      const preset = presets.find((p) => p.value === activePreset)
      if (preset) handlePresetClick(preset.value, preset.days)
    } else if (customRange) {
      const s = customRange[0].format('YYYY-MM-DD')
      const e = customRange[1].format('YYYY-MM-DD')
      setSearchParams({ from: s, to: e })
      onChange?.(s, e)
    }
  }

  return (
    <div className={cn('flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6', className)}>
      <div className="flex flex-wrap items-center gap-2">
        {presets.map((preset) => (
          <Button
            key={preset.value}
            type={activePreset === preset.value ? 'primary' : 'default'}
            size="small"
            onClick={() => handlePresetClick(preset.value, preset.days)}
            className={cn(
              'rounded-lg transition-all',
              activePreset === preset.value ? 'shadow-glow-primary' : 'hover:bg-neutral-100'
            )}
          >
            {preset.label}
          </Button>
        ))}
        <RangePicker
          size="small"
          className="rounded-lg"
          value={customRange}
          onChange={handleCustomRangeChange as never}
          format="DD MMM YYYY"
          allowClear={false}
        />
      </div>
      <Tooltip title="Refresh data">
        <Button
          icon={<ReloadOutlined />}
          size="small"
          onClick={handleRefresh}
          className="rounded-lg hover:bg-neutral-100"
        />
      </Tooltip>
    </div>
  )
}
