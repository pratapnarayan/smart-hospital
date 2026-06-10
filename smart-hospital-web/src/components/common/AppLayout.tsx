import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu, Avatar, Dropdown, Typography, Badge, type MenuProps } from 'antd'
import {
  DashboardOutlined, UserOutlined, MedicineBoxOutlined,
  ExperimentOutlined, LogoutOutlined, MenuFoldOutlined, MenuUnfoldOutlined,
  BellOutlined, ShopOutlined, HomeOutlined, ScheduleOutlined, TeamOutlined,
  DollarOutlined, InboxOutlined, HeartOutlined, AlertOutlined, RadarChartOutlined,
  SolutionOutlined,
} from '@ant-design/icons'
import { useAuthStore } from '@/store/authStore'
import { useUiStore } from '@/store/uiStore'
import { useLogout } from '@/hooks/useAuth'
import { initials } from '@/utils'

const { Header, Sider, Content } = Layout

export function AppLayout() {
  const { user } = useAuthStore()
  const { sidebarCollapsed, toggleSidebar } = useUiStore()
  const { mutate: logout } = useLogout()
  const navigate = useNavigate()
  const location = useLocation()

  const selectedKey = location.pathname.split('/')[1] || 'dashboard'

  const menuItems: MenuProps['items'] = [
    { key: 'dashboard', icon: <DashboardOutlined />, label: 'Dashboard',
      onClick: () => navigate('/dashboard') },
    { key: 'patients',  icon: <UserOutlined />,       label: 'Patients',
      onClick: () => navigate('/patients') },
    {
      key: 'doctors', icon: <SolutionOutlined />, label: 'Doctors',
      children: [
        { key: 'doctors',               label: 'Doctor Directory', onClick: () => navigate('/doctors') },
        { key: 'doctors/specializations', label: 'Specializations', onClick: () => navigate('/doctors/specializations') },
      ],
    },
    {
      key: 'opd', icon: <MedicineBoxOutlined />, label: 'OPD',
      onClick: () => navigate('/opd'),
    },
    {
      key: 'pharmacy', icon: <ShopOutlined />, label: 'Pharmacy',
      children: [
        { key: 'pharmacy/stock', label: 'Stock',   onClick: () => navigate('/pharmacy/stock') },
        { key: 'pharmacy/bill',  label: 'Billing', onClick: () => navigate('/pharmacy/bill') },
      ],
    },
    {
      key: 'ipd', icon: <HomeOutlined />, label: 'IPD',
      onClick: () => navigate('/ipd'),
    },
    {
      key: 'frontoffice', icon: <ScheduleOutlined />, label: 'Front Office',
      children: [
        { key: 'frontoffice/appointments', label: 'Appointments',
          onClick: () => navigate('/frontoffice/appointments') },
        { key: 'frontoffice/queue', label: 'Token Queue',
          onClick: () => navigate('/frontoffice/queue') },
      ],
    },
    {
      key: 'hr', icon: <TeamOutlined />, label: 'HR',
      children: [
        { key: 'hr/employees',  label: 'Employees',  onClick: () => navigate('/hr/employees') },
        { key: 'hr/attendance', label: 'Attendance', onClick: () => navigate('/hr/attendance') },
        { key: 'hr/leave',      label: 'Leave',      onClick: () => navigate('/hr/leave') },
      ],
    },
    {
      key: 'pathology', icon: <ExperimentOutlined />, label: 'Pathology',
      onClick: () => navigate('/pathology'),
    },
    {
      key: 'radiology', icon: <RadarChartOutlined />, label: 'Radiology',
      children: [
        { key: 'radiology/dashboard', label: 'Overview',
          onClick: () => navigate('/radiology/dashboard') },
        { key: 'radiology/orders', label: 'Orders',
          onClick: () => navigate('/radiology/orders') },
      ],
    },
    {
      key: 'operation', icon: <AlertOutlined />, label: 'Operation Theatre',
      children: [
        { key: 'operation/dashboard',  label: 'Overview',    onClick: () => navigate('/operation/dashboard') },
        { key: 'operation/schedules',  label: 'Schedules',   onClick: () => navigate('/operation/schedules') },
      ],
    },
    {
      key: 'bloodbank', icon: <HeartOutlined />, label: 'Blood Bank',
      children: [
        { key: 'bloodbank/dashboard', label: 'Overview',      onClick: () => navigate('/bloodbank/dashboard') },
        { key: 'bloodbank/units',     label: 'Blood Units',   onClick: () => navigate('/bloodbank/units') },
        { key: 'bloodbank/requests',  label: 'Requests',      onClick: () => navigate('/bloodbank/requests') },
      ],
    },
    {
      key: 'inventory', icon: <InboxOutlined />, label: 'Inventory',
      children: [
        { key: 'inventory/dashboard',  label: 'Overview',        onClick: () => navigate('/inventory/dashboard') },
        { key: 'inventory/items',      label: 'Item Catalogue',  onClick: () => navigate('/inventory/items') },
        { key: 'inventory/movements',  label: 'Stock Movements', onClick: () => navigate('/inventory/movements') },
      ],
    },
    {
      key: 'finance', icon: <DollarOutlined />, label: 'Finance',
      children: [
        { key: 'finance/dashboard', label: 'Overview',  onClick: () => navigate('/finance/dashboard') },
        { key: 'finance/income',    label: 'Income',    onClick: () => navigate('/finance/income') },
        { key: 'finance/expenses',  label: 'Expenses',  onClick: () => navigate('/finance/expenses') },
      ],
    },
  ]

  const userMenu: MenuProps['items'] = [
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: 'Logout',
      danger: true,
      onClick: () => logout(),
    },
  ]

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        trigger={null}
        collapsible
        collapsed={sidebarCollapsed}
        style={{ background: '#001529' }}
        width={220}
      >
        {/* Logo */}
        <div className="flex items-center justify-center py-4 px-2">
          {sidebarCollapsed
            ? <MedicineBoxOutlined style={{ fontSize: 24, color: '#1677ff' }} />
            : <Typography.Text strong style={{ color: '#fff', fontSize: 16 }}>
                🏥 SmartHospital
              </Typography.Text>
          }
        </div>

        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          defaultOpenKeys={['pharmacy']}
          items={menuItems}
        />
      </Sider>

      <Layout>
        <Header
          style={{
            background: '#fff',
            padding: '0 16px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            boxShadow: '0 1px 4px rgba(0,21,41,.08)',
          }}
        >
          {/* Collapse toggle */}
          <span style={{ fontSize: 18, cursor: 'pointer' }} onClick={toggleSidebar}>
            {sidebarCollapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          </span>

          {/* Right: notifications + user */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <Badge count={0}>
              <BellOutlined style={{ fontSize: 18, cursor: 'pointer' }} />
            </Badge>
            <Dropdown menu={{ items: userMenu }} placement="bottomRight">
              <div style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8 }}>
                <Avatar style={{ background: '#1677ff' }}>
                  {user ? initials(user.firstName, user.lastName) : 'U'}
                </Avatar>
                {!sidebarCollapsed && (
                  <Typography.Text>
                    {user?.firstName} {user?.lastName}
                  </Typography.Text>
                )}
              </div>
            </Dropdown>
          </div>
        </Header>

        <Content style={{ margin: '24px 16px', padding: 24, background: '#f0f2f5', minHeight: 360 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
