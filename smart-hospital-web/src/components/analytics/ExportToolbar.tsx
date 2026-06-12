import { Space, Button, Tag, Tooltip } from 'antd'
import { FileExcelOutlined, FilePdfOutlined, PrinterOutlined, InfoCircleOutlined } from '@ant-design/icons'
import { useSearchParams } from 'react-router-dom'
import { useState } from 'react'
import { apiClient } from '@/api/client'

interface ExportToolbarProps {
  section: string
  isDemoData?: boolean
}

export function ExportToolbar({ section, isDemoData }: ExportToolbarProps) {
  const [params] = useSearchParams()
  const [loading, setLoading] = useState<string | null>(null)

  const from = params.get('from') ?? ''
  const to = params.get('to') ?? ''

  const handleDownload = async (format: string) => {
    setLoading(format)
    try {
      const qp: Record<string, string> = { format }
      if (from) qp.from = from
      if (to) qp.to = to
      const res = await apiClient.get(`/v1/analytics/export/${section}`, {
        params: qp,
        responseType: 'blob',
      })
      const ext = format === 'excel' ? 'xlsx' : 'pdf'
      const url = URL.createObjectURL(res.data)
      const a = document.createElement('a')
      a.href = url
      a.download = `${section}-analytics.${ext}`
      a.click()
      URL.revokeObjectURL(url)
    } finally {
      setLoading(null)
    }
  }

  return (
    <Space wrap>
      {isDemoData && (
        <Tooltip title="Charts are displaying sample data for demonstration purposes">
          <Tag color="orange" icon={<InfoCircleOutlined />}>Demo Data</Tag>
        </Tooltip>
      )}
      <Tooltip title={isDemoData ? 'Export unavailable in demo mode' : undefined}>
        <Button
          icon={<FileExcelOutlined />}
          onClick={() => handleDownload('excel')}
          loading={loading === 'excel'}
          disabled={isDemoData}
          size="small"
        >
          Excel
        </Button>
      </Tooltip>
      <Tooltip title={isDemoData ? 'Export unavailable in demo mode' : undefined}>
        <Button
          icon={<FilePdfOutlined />}
          onClick={() => handleDownload('pdf')}
          loading={loading === 'pdf'}
          disabled={isDemoData}
          size="small"
          danger
        >
          PDF
        </Button>
      </Tooltip>
      <Button
        icon={<PrinterOutlined />}
        onClick={() => window.print()}
        size="small"
      >
        Print
      </Button>
    </Space>
  )
}
