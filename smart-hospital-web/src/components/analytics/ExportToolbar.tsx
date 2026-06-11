import { Space, Button, Tag, Tooltip } from 'antd'
import { FileExcelOutlined, FilePdfOutlined, PrinterOutlined, InfoCircleOutlined } from '@ant-design/icons'
import { useSearchParams } from 'react-router-dom'

interface ExportToolbarProps {
  section: string
  isDemoData?: boolean
}

export function ExportToolbar({ section, isDemoData }: ExportToolbarProps) {
  const [params] = useSearchParams()

  const from = params.get('from') ?? ''
  const to = params.get('to') ?? ''

  const buildUrl = (format: string) => {
    const base = `/api/v1/analytics/export/${section}`
    const qp = new URLSearchParams({ format })
    if (from) qp.set('from', from)
    if (to) qp.set('to', to)
    return `${base}?${qp.toString()}`
  }

  const handleDownload = (format: string) => {
    window.open(buildUrl(format), '_blank')
  }

  return (
    <Space wrap>
      {isDemoData && (
        <Tooltip title="Charts are displaying sample data for demonstration purposes">
          <Tag color="orange" icon={<InfoCircleOutlined />}>Demo Data</Tag>
        </Tooltip>
      )}
      <Button
        icon={<FileExcelOutlined />}
        onClick={() => handleDownload('excel')}
        size="small"
      >
        Excel
      </Button>
      <Button
        icon={<FilePdfOutlined />}
        onClick={() => handleDownload('pdf')}
        size="small"
        danger
      >
        PDF
      </Button>
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
