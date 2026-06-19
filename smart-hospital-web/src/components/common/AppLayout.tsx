import { useState, useCallback, useEffect } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import {
  Layout, Menu, Avatar, Dropdown, Badge, Drawer, Tooltip,
  type MenuProps,
} from 'antd'
import {
  DashboardOutlined, UserOutlined, MedicineBoxOutlined,
  ExperimentOutlined, LogoutOutlined, MenuFoldOutlined, MenuUnfoldOutlined,
  BellOutlined, ShopOutlined, HomeOutlined, ScheduleOutlined, TeamOutlined,
  DollarOutlined, InboxOutlined, HeartOutlined, AlertOutlined, RadarChartOutlined,
  SolutionOutlined, BarChartOutlined, SettingOutlined,
} from '@ant-design/icons'
import { useAuthStore } from '@/store/authStore'
import { useUiStore } from '@/store/uiStore'
import { useLogout } from '@/hooks/useAuth'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { initials } from '@/utils'
import { cn } from '@/utils/cn'
import { designTokens } from '@/theme'

const { Header, Sider, Content } = Layout

// ─── Menu configuration ───────────────────────────────────────────────────────

interface MenuItem {
  key: string
  icon: React.ReactNode
  label: string
  path?: string
  children?: MenuItem[]
}

const menuConfig: MenuItem[] = [
  { key: 'dashboard', icon: <DashboardOutlined />, label: 'Dashboard', path: '/dashboard' },
  { key: 'patients', icon: <UserOutlined />, label: 'Patients', path: '/patients' },
  {
    key: 'doctors', icon: <SolutionOutlined />, label: 'Doctors',
    children: [
      { key: 'doctor-directory', icon: <UserOutlined />, label: 'Doctor Directory', path: '/doctors' },
      { key: 'doctors/specializations', icon: <ExperimentOutlined />, label: 'Specializations', path: '/doctors/specializations' },
    ],
  },
  { key: 'opd', icon: <MedicineBoxOutlined />, label: 'OPD', path: '/opd' },
  {
    key: 'pharmacy', icon: <ShopOutlined />, label: 'Pharmacy',
    children: [
      { key: 'pharmacy/stock', icon: <InboxOutlined />, label: 'Stock', path: '/pharmacy/stock' },
      { key: 'pharmacy/bill', icon: <DollarOutlined />, label: 'Billing', path: '/pharmacy/bill' },
    ],
  },
  { key: 'ipd', icon: <HomeOutlined />, label: 'IPD', path: '/ipd' },
  {
    key: 'frontoffice', icon: <ScheduleOutlined />, label: 'Front Office',
    children: [
      { key: 'frontoffice/appointments', icon: <ScheduleOutlined />, label: 'Appointments', path: '/frontoffice/appointments' },
      { key: 'frontoffice/queue', icon: <TeamOutlined />, label: 'Token Queue', path: '/frontoffice/queue' },
    ],
  },
  {
    key: 'hr', icon: <TeamOutlined />, label: 'HR',
    children: [
      { key: 'hr/employees', icon: <UserOutlined />, label: 'Employees', path: '/hr/employees' },
      { key: 'hr/attendance', icon: <ScheduleOutlined />, label: 'Attendance', path: '/hr/attendance' },
      { key: 'hr/leave', icon: <AlertOutlined />, label: 'Leave', path: '/hr/leave' },
    ],
  },
  { key: 'pathology', icon: <ExperimentOutlined />, label: 'Pathology', path: '/pathology' },
  {
    key: 'radiology', icon: <RadarChartOutlined />, label: 'Radiology',
    children: [
      { key: 'radiology/dashboard', icon: <DashboardOutlined />, label: 'Overview', path: '/radiology/dashboard' },
      { key: 'radiology/orders', icon: <InboxOutlined />, label: 'Orders', path: '/radiology/orders' },
    ],
  },
  {
    key: 'operation', icon: <AlertOutlined />, label: 'Operation Theatre',
    children: [
      { key: 'operation/dashboard', icon: <DashboardOutlined />, label: 'Overview', path: '/operation/dashboard' },
      { key: 'operation/schedules', icon: <ScheduleOutlined />, label: 'Schedules', path: '/operation/schedules' },
    ],
  },
  {
    key: 'bloodbank', icon: <HeartOutlined />, label: 'Blood Bank',
    children: [
      { key: 'bloodbank/dashboard', icon: <DashboardOutlined />, label: 'Overview', path: '/bloodbank/dashboard' },
      { key: 'bloodbank/units', icon: <InboxOutlined />, label: 'Blood Units', path: '/bloodbank/units' },
      { key: 'bloodbank/requests', icon: <UserOutlined />, label: 'Requests', path: '/bloodbank/requests' },
    ],
  },
  {
    key: 'inventory', icon: <InboxOutlined />, label: 'Inventory',
    children: [
      { key: 'inventory/dashboard', icon: <DashboardOutlined />, label: 'Overview', path: '/inventory/dashboard' },
      { key: 'inventory/items', icon: <ShopOutlined />, label: 'Item Catalogue', path: '/inventory/items' },
      { key: 'inventory/movements', icon: <InboxOutlined />, label: 'Stock Movements', path: '/inventory/movements' },
    ],
  },
  {
    key: 'finance', icon: <DollarOutlined />, label: 'Finance',
    children: [
      { key: 'finance/dashboard', icon: <DashboardOutlined />, label: 'Overview', path: '/finance/dashboard' },
      { key: 'finance/income', icon: <DollarOutlined />, label: 'Income', path: '/finance/income' },
      { key: 'finance/expenses', icon: <DollarOutlined />, label: 'Expenses', path: '/finance/expenses' },
    ],
  },
  {
    key: 'analytics', icon: <BarChartOutlined />, label: 'Reports & Analytics',
    children: [
      { key: 'analytics/executive', icon: <BarChartOutlined />, label: 'Executive Dashboard', path: '/analytics/executive' },
      { key: 'analytics/financial', icon: <DollarOutlined />, label: 'Financial Analytics', path: '/analytics/financial' },
      { key: 'analytics/patients', icon: <UserOutlined />, label: 'Patient Analytics', path: '/analytics/patients' },
      { key: 'analytics/doctors', icon: <SolutionOutlined />, label: 'Doctor Analytics', path: '/analytics/doctors' },
      { key: 'analytics/appointments', icon: <ScheduleOutlined />, label: 'Appointment Analytics', path: '/analytics/appointments' },
      { key: 'analytics/pharmacy', icon: <ShopOutlined />, label: 'Pharmacy Analytics', path: '/analytics/pharmacy' },
      { key: 'analytics/laboratory', icon: <ExperimentOutlined />, label: 'Laboratory Analytics', path: '/analytics/laboratory' },
      { key: 'analytics/inventory', icon: <InboxOutlined />, label: 'Inventory Analytics', path: '/analytics/inventory' },
    ],
  },
]

// ─── SidebarContent sub-component ────────────────────────────────────────────

interface SidebarContentProps {
  collapsed: boolean
  selectedKeys: string[]
  openKeys: string[]
  onOpenChange: (keys: string[]) => void
  onNavigate: (path: string) => void
}

function SidebarContent({
  collapsed,
  selectedKeys,
  openKeys,
  onOpenChange,
  onNavigate,
}: SidebarContentProps) {
  const renderMenuItems = (items: MenuItem[]): MenuProps['items'] =>
    items.map((item) => {
      if (item.children) {
        return {
          key: item.key,
          icon: item.icon,
          label: item.label,
          children: renderMenuItems(item.children),
        }
      }
      return {
        key: item.key,
        icon: item.icon,
        label: item.label,
        onClick: () => item.path && onNavigate(item.path),
      }
    })

  return (
    <div className="flex flex-col h-full" style={{ background: designTokens.colors.sidebar.bg }}>
      {/* Logo */}
      <div
        className={cn(
          'flex items-center justify-center border-b px-4',
          collapsed ? 'py-4' : 'py-5'
        )}
        style={{ borderColor: designTokens.colors.sidebar.border }}
      >
        {collapsed ? (
          <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-blue-600 text-white shadow-lg">
            <MedicineBoxOutlined className="text-lg" />
          </div>
        ) : (
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-blue-600 text-white shadow-lg">
              <MedicineBoxOutlined className="text-lg" />
            </div>
            <div className="flex flex-col">
              <span className="text-white font-semibold text-base tracking-tight">SmartHospital</span>
              <span className="text-xs" style={{ color: designTokens.colors.sidebar.text }}>
                Hospital Management
              </span>
            </div>
          </div>
        )}
      </div>

      {/* Menu */}
      <div className="flex-1 overflow-y-auto py-2" style={{ scrollbarWidth: 'none' }}>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={selectedKeys}
          openKeys={openKeys}
          onOpenChange={onOpenChange}
          items={renderMenuItems(menuConfig)}
          style={{ background: 'transparent', border: 'none' }}
        />
      </div>

      {/* Footer — system status */}
      {!collapsed && (
        <div
          className="px-4 py-3 border-t"
          style={{ borderColor: designTokens.colors.sidebar.border }}
        >
          <div className="flex items-center gap-2 text-xs" style={{ color: designTokens.colors.sidebar.text }}>
            <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
            <span>System Online</span>
          </div>
        </div>
      )}
    </div>
  )
}

// ─── Main AppLayout ───────────────────────────────────────────────────────────

export function AppLayout() {
  const { user } = useAuthStore()
  const { sidebarCollapsed, toggleSidebar } = useUiStore()
  const { mutate: logout } = useLogout()
  const navigate = useNavigate()
  const location = useLocation()

  const isMobile = useMediaQuery('(max-width: 768px)')
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false)
  const [openKeys, setOpenKeys] = useState<string[]>(() => {
    const first = location.pathname.split('/').filter(Boolean)[0]
    return first ? [first] : []
  })

  const pathSegments = location.pathname.split('/').filter(Boolean)
  const selectedKey = pathSegments.slice(0, 2).join('/') || 'dashboard'
  const selectedKeys = [selectedKey]

  const currentPageLabel =
    pathSegments[0]
      ? pathSegments[0].charAt(0).toUpperCase() + pathSegments[0].slice(1)
      : 'Home'

  useEffect(() => {
    if (isMobile && !sidebarCollapsed) {
      toggleSidebar()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isMobile])

  useEffect(() => {
    const first = location.pathname.split('/').filter(Boolean)[0]
    if (first) setOpenKeys(prev => prev.includes(first) ? prev : [first])
  }, [location.pathname])

  const handleNavigate = useCallback(
    (path: string) => {
      navigate(path)
      if (isMobile) setMobileDrawerOpen(false)
    },
    [navigate, isMobile]
  )

  const handleOpenChange = useCallback((keys: string[]) => {
    setOpenKeys(keys)
  }, [])

  const sidebarProps: SidebarContentProps = {
    collapsed: isMobile ? false : sidebarCollapsed,
    selectedKeys,
    openKeys,
    onOpenChange: handleOpenChange,
    onNavigate: handleNavigate,
  }

  const userMenu: MenuProps['items'] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: 'Profile',
      onClick: () => navigate('/profile'),
    },
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: 'Settings',
      onClick: () => navigate('/settings'),
    },
    { type: 'divider' },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: 'Logout',
      danger: true,
      onClick: () => logout(),
    },
  ]

  return (
    <Layout className="min-h-screen" style={{ background: designTokens.colors.background.primary }}>
      {/* Mobile Drawer */}
      {isMobile && (
        <Drawer
          placement="left"
          closable={false}
          onClose={() => setMobileDrawerOpen(false)}
          open={mobileDrawerOpen}
          width={260}
          styles={{ body: { padding: 0, background: designTokens.colors.sidebar.bg } }}
        >
          <SidebarContent {...sidebarProps} />
        </Drawer>
      )}

      {/* Desktop Sider */}
      {!isMobile && (
        <Sider
          trigger={null}
          collapsible
          collapsed={sidebarCollapsed}
          width={260}
          collapsedWidth={80}
          className="fixed left-0 top-0 bottom-0 z-50 overflow-hidden"
          style={{
            background: designTokens.colors.sidebar.bg,
            boxShadow: '0 10px 15px -3px rgba(15, 23, 42, 0.1), 0 4px 6px -4px rgba(15, 23, 42, 0.1)',
          }}
        >
          <SidebarContent {...sidebarProps} />
        </Sider>
      )}

      {/* Main Layout */}
      <Layout
        className={cn(
          'transition-all duration-300',
          !isMobile && (sidebarCollapsed ? 'ml-20' : 'ml-[260px]')
        )}
        style={{ background: designTokens.colors.background.primary }}
      >
        {/* Header */}
        <Header
          className={cn(
            'fixed top-0 right-0 z-40 flex items-center justify-between px-6 h-16',
            'border-b border-slate-200',
            'transition-all duration-300',
            !isMobile && (sidebarCollapsed ? 'left-20' : 'left-[260px]')
          )}
          style={{
            background: 'rgba(255,255,255,0.85)',
            backdropFilter: 'blur(12px)',
            WebkitBackdropFilter: 'blur(12px)',
            boxShadow: '0 1px 3px rgba(15, 23, 42, 0.05)',
            lineHeight: 'normal',
          }}
        >
          {/* Left: Toggle + Breadcrumb */}
          <div className="flex items-center gap-4">
            {isMobile ? (
              <button
                onClick={() => setMobileDrawerOpen(true)}
                className="flex items-center justify-center w-10 h-10 rounded-lg hover:bg-slate-100 transition-colors"
                aria-label="Open menu"
              >
                <MenuUnfoldOutlined className="text-lg" style={{ color: designTokens.colors.text.primary }} />
              </button>
            ) : (
              <button
                onClick={toggleSidebar}
                className="flex items-center justify-center w-10 h-10 rounded-lg hover:bg-slate-100 transition-colors"
                aria-label={sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
              >
                {sidebarCollapsed
                  ? <MenuUnfoldOutlined className="text-lg" style={{ color: designTokens.colors.text.primary }} />
                  : <MenuFoldOutlined className="text-lg" style={{ color: designTokens.colors.text.primary }} />
                }
              </button>
            )}

            {/* Breadcrumb */}
            <nav className="hidden md:flex items-center text-sm" style={{ color: designTokens.colors.text.muted }}>
              <span
                className="hover:text-blue-500 transition-colors cursor-pointer"
                onClick={() => navigate('/dashboard')}
              >
                Dashboard
              </span>
              {pathSegments.length > 0 && (
                <>
                  <span className="mx-2 text-slate-300">/</span>
                  <span className="font-medium" style={{ color: designTokens.colors.text.primary }}>
                    {currentPageLabel}
                  </span>
                </>
              )}
            </nav>
          </div>

          {/* Right: Notifications + User */}
          <div className="flex items-center gap-3">
            <Tooltip title="Notifications">
              <button
                className="relative flex items-center justify-center w-10 h-10 rounded-lg hover:bg-slate-100 transition-colors"
                aria-label="Notifications"
              >
                <Badge count={0} size="small" offset={[-2, 2]}>
                  <BellOutlined className="text-lg" style={{ color: designTokens.colors.text.secondary }} />
                </Badge>
              </button>
            </Tooltip>

            <Dropdown menu={{ items: userMenu }} placement="bottomRight" arrow trigger={['click']}>
              <button
                className="flex items-center gap-3 px-3 py-1.5 rounded-xl hover:bg-slate-100 transition-all"
                aria-label="User menu"
              >
                <Avatar
                  size={36}
                  style={{ background: designTokens.colors.primary[500], color: '#fff', fontWeight: 500 }}
                >
                  {user ? initials(user.firstName, user.lastName) : 'U'}
                </Avatar>
                <div className="hidden md:flex flex-col items-start">
                  <span className="text-sm font-medium leading-tight" style={{ color: designTokens.colors.text.primary }}>
                    {user?.firstName} {user?.lastName}
                  </span>
                  <span className="text-xs leading-tight" style={{ color: designTokens.colors.text.muted }}>
                    {user?.role?.replace('_', ' ')}
                  </span>
                </div>
              </button>
            </Dropdown>
          </div>
        </Header>

        {/* Content */}
        <Content
          className="mt-16 min-h-[calc(100vh-64px)] transition-all duration-300"
          style={{ background: designTokens.colors.background.primary }}
        >
          <div className="p-6">
            <Outlet />
          </div>
        </Content>
      </Layout>
    </Layout>
  )
}
