import { useState } from 'react'
import { Select } from 'antd'
import { usePatients } from '@/hooks/usePatients'
import type { Patient } from '@/types'

interface Props {
  value?: string
  onChange?: (value: string) => void
  onPatientSelect?: (patient: Patient) => void
  placeholder?: string
  disabled?: boolean
  style?: React.CSSProperties
  /**
   * 'name' (default) — form value is the patient's full name string.
   * 'id'             — form value is the patient's UUID; display shows the name.
   */
  valueMode?: 'name' | 'id'
}

/**
 * Searchable patient dropdown.
 * - valueMode='name' (default): stores full name — use where the API expects a name string.
 * - valueMode='id': stores the patient UUID — use where the API expects a patientId.
 */
export function PatientSearchSelect({
  value,
  onChange,
  onPatientSelect,
  placeholder = 'Search patient by name or mobile…',
  disabled,
  style,
  valueMode = 'name',
}: Props) {
  const [query, setQuery] = useState('')
  // When mode=id and a UUID is pre-set from outside, track the display label separately
  const [presetLabel, setPresetLabel] = useState<string | undefined>()

  const { data: patients, isFetching } = usePatients(query || undefined)

  const options = patients?.content.map(p => ({
    value: valueMode === 'id' ? p.id : `${p.firstName} ${p.lastName}`,
    label: `${p.firstName} ${p.lastName}${p.mobile ? ` — ${p.mobile}` : ''}`,
    patient: p,
  })) ?? []

  const handleSelect = (_: string, option: (typeof options)[number]) => {
    const stored = valueMode === 'id' ? option.patient.id : `${option.patient.firstName} ${option.patient.lastName}`
    onChange?.(stored)
    onPatientSelect?.(option.patient)
    if (valueMode === 'id') setPresetLabel(`${option.patient.firstName} ${option.patient.lastName}`)
  }

  // In id-mode: if a UUID is set but no matching option exists in the current list,
  // show the presetLabel so the field isn't blank.
  const selectValue = value || undefined
  const optionsWithPreset =
    valueMode === 'id' && value && presetLabel && !options.find(o => o.value === value)
      ? [{ value, label: presetLabel, patient: undefined as unknown as Patient }, ...options]
      : options

  return (
    <Select
      showSearch
      allowClear
      filterOption={false}
      value={selectValue}
      onSearch={setQuery}
      onSelect={handleSelect}
      onChange={v => { if (!v) { onChange?.(''); setPresetLabel(undefined) } }}
      options={optionsWithPreset}
      loading={isFetching}
      placeholder={placeholder}
      disabled={disabled}
      style={style}
      notFoundContent={query ? (isFetching ? 'Searching…' : 'No patients found') : 'Type to search patients'}
    />
  )
}
