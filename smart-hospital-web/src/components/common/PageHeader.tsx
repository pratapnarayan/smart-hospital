import { Typography, Breadcrumb, Button, Space, type BreadcrumbProps } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { cn } from '@/utils/cn'
import type { ReactNode } from 'react'

const { Title, Text } = Typography

interface Props {
  title: string
  subtitle?: string
  breadcrumbs?: BreadcrumbProps['items']
  extra?: ReactNode
  back?: boolean
  onBack?: () => void
  className?: string
}

export function PageHeader({
  title,
  subtitle,
  breadcrumbs,
  extra,
  back = false,
  onBack,
  className,
}: Props) {
  const navigate = useNavigate()

  return (
    <div className={cn('mb-6', className)}>
      {breadcrumbs && breadcrumbs.length > 0 && (
        <Breadcrumb items={breadcrumbs} className="mb-3 text-sm" />
      )}

      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-start gap-3">
          {back && (
            <Button
              type="text"
              icon={<ArrowLeftOutlined />}
              onClick={onBack ?? (() => navigate(-1))}
              className="mt-1 hover:bg-neutral-100"
              aria-label="Go back"
            />
          )}
          <div>
            <Title
              level={3}
              className="!mb-0 !text-2xl !font-bold !leading-tight"
              style={{ color: 'var(--text-primary)' }}
            >
              {title}
            </Title>
            {subtitle && (
              <Text className="text-sm mt-1 block" style={{ color: 'var(--text-muted)' }}>
                {subtitle}
              </Text>
            )}
          </div>
        </div>

        {extra && (
          <Space className="flex-shrink-0" wrap>
            {extra}
          </Space>
        )}
      </div>
    </div>
  )
}
