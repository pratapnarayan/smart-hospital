import { Button, Space, Tooltip, Badge } from 'antd'
import { FileExcelOutlined, FilePdfOutlined, InfoCircleOutlined } from '@ant-design/icons'
import { cn } from '@/utils/cn'

interface ExportToolbarProps {
  section: string
  isDemoData?: boolean
  onExportExcel?: () => void
  onExportPdf?: () => void
  className?: string
}

export function ExportToolbar({
  section: _section,
  isDemoData = false,
  onExportExcel,
  onExportPdf,
  className,
}: ExportToolbarProps) {
  return (
    <Space className={cn('items-center', className)} wrap>
      {isDemoData && (
        <Tooltip title="Showing demo data. Connect the API to see live data.">
          <Badge
            count="DEMO"
            style={{ backgroundColor: '#faad14', color: '#fff', fontSize: '10px', fontWeight: 600, padding: '0 6px' }}
          >
            <InfoCircleOutlined style={{ color: '#faad14', fontSize: 18 }} />
          </Badge>
        </Tooltip>
      )}
      <Button
        icon={<FileExcelOutlined />}
        onClick={onExportExcel}
        disabled={!onExportExcel}
        className="rounded-lg hover:bg-success-50 hover:text-success-600 hover:border-success-200 transition-all"
      >
        Excel
      </Button>
      <Button
        icon={<FilePdfOutlined />}
        onClick={onExportPdf}
        disabled={!onExportPdf}
        className="rounded-lg hover:bg-danger-50 hover:text-danger-600 hover:border-danger-200 transition-all"
      >
        PDF
      </Button>
    </Space>
  )
}
