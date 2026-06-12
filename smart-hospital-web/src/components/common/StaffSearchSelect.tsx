import { useEffect, useState } from 'react'
import { Select } from 'antd'
import { useEmployees } from '@/hooks/useHr'
import type { Employee } from '@/types'

interface Props {
  value?: string
  onChange?: (value: string) => void
  onEmployeeSelect?: (employee: Employee) => void
  placeholder?: string
  disabled?: boolean
  style?: React.CSSProperties
  /** Prefix selected name with "Dr." — use for doctor/physician fields */
  isDoctor?: boolean
  /** Filter employees by department ID */
  departmentId?: string
}

/**
 * Searchable staff/doctor dropdown. Stores the employee's formatted name as the form value.
 * Use `onEmployeeSelect` to also capture the employee's ID or other fields.
 */
export function StaffSearchSelect({
  value,
  onChange,
  onEmployeeSelect,
  placeholder = 'Search staff by name…',
  disabled,
  style,
  isDoctor = false,
  departmentId,
}: Props) {
  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')

  useEffect(() => {
    const t = setTimeout(() => setDebouncedSearch(search), 300)
    return () => clearTimeout(t)
  }, [search])

  const { data: employeePage, isFetching } = useEmployees(
    departmentId,
    debouncedSearch || undefined,
    0
  )

  const formatName = (e: Employee) =>
    isDoctor ? `Dr. ${e.firstName} ${e.lastName}` : `${e.firstName} ${e.lastName}`

  const options = (employeePage?.content ?? []).map(e => ({
    value: formatName(e),
    label: formatName(e),
    employee: e,
  }))

  const handleSelect = (_: string, option: (typeof options)[number]) => {
    onChange?.(option.value)
    onEmployeeSelect?.(option.employee)
  }

  return (
    <Select
      showSearch
      allowClear
      filterOption={false}
      value={value || undefined}
      onSearch={setSearch}
      onSelect={handleSelect}
      onChange={v => { if (!v) onChange?.('') }}
      options={options}
      loading={isFetching}
      placeholder={placeholder}
      disabled={disabled}
      style={style}
      notFoundContent={
        debouncedSearch
          ? (isFetching ? 'Searching…' : 'No staff found')
          : 'Type to search staff'
      }
    />
  )
}
