import { Controller, type Control, type FieldValues, type Path } from 'react-hook-form'
import { Form, Input, Select, DatePicker, InputNumber, Radio, Checkbox, Switch } from 'antd'
import { cn } from '@/utils/cn'
import dayjs from 'dayjs'

const { TextArea } = Input

type FormFieldType =
  | 'text' | 'textarea' | 'number' | 'select' | 'date'
  | 'datetime' | 'radio' | 'checkbox' | 'switch' | 'password'
  | 'email' | 'tel'

interface FormFieldProps<T extends FieldValues> {
  name: Path<T>
  control: Control<T>
  label: string
  type?: FormFieldType
  placeholder?: string
  options?: { label: string; value: string | number }[]
  disabled?: boolean
  className?: string
  helperText?: string
  prefix?: React.ReactNode
  rows?: number
  min?: number
  max?: number
  allowClear?: boolean
  showSearch?: boolean
  mode?: 'multiple' | 'tags'
  format?: string
  size?: 'large' | 'middle' | 'small'
}

export function FormField<T extends FieldValues>({
  name,
  control,
  label,
  type = 'text',
  placeholder,
  options,
  disabled,
  className,
  helperText,
  prefix,
  rows = 3,
  min,
  max,
  allowClear = true,
  showSearch = false,
  mode,
  format = 'DD/MM/YYYY',
  size = 'middle',
}: FormFieldProps<T>) {
  return (
    <Controller
      name={name}
      control={control}
      render={({ field, fieldState: { error } }) => {
        const commonProps = { placeholder, disabled, size, className: 'rounded-lg' }

        let input: React.ReactNode

        switch (type) {
          case 'textarea':
            input = (
              <TextArea
                {...commonProps}
                rows={rows}
                value={field.value as string}
                onChange={(e) => field.onChange(e.target.value)}
                onBlur={field.onBlur}
              />
            )
            break
          case 'number':
            input = (
              <InputNumber
                placeholder={placeholder}
                disabled={disabled}
                size={size}
                min={min}
                max={max}
                className={cn('w-full rounded-lg', className)}
                value={field.value as number}
                onChange={(val) => field.onChange(val)}
                onBlur={field.onBlur}
              />
            )
            break
          case 'select':
            input = (
              <Select
                placeholder={placeholder}
                disabled={disabled}
                size={size}
                options={options}
                allowClear={allowClear}
                showSearch={showSearch}
                mode={mode}
                className={cn('w-full', className)}
                value={field.value as string | string[]}
                onChange={(val) => field.onChange(val)}
                onBlur={field.onBlur}
              />
            )
            break
          case 'date':
            input = (
              <DatePicker
                placeholder={placeholder}
                disabled={disabled}
                size={size}
                format={format}
                className={cn('w-full rounded-lg', className)}
                value={field.value ? dayjs(field.value as string) : null}
                onChange={(date) => field.onChange(date ? date.format('YYYY-MM-DD') : null)}
                onBlur={field.onBlur}
              />
            )
            break
          case 'datetime':
            input = (
              <DatePicker
                placeholder={placeholder}
                disabled={disabled}
                size={size}
                showTime
                format={`${format} HH:mm`}
                className={cn('w-full rounded-lg', className)}
                value={field.value ? dayjs(field.value as string) : null}
                onChange={(date) => field.onChange(date ? date.toISOString() : null)}
                onBlur={field.onBlur}
              />
            )
            break
          case 'radio':
            input = (
              <Radio.Group
                options={options}
                disabled={disabled}
                value={field.value as string}
                onChange={(e) => field.onChange(e.target.value)}
              />
            )
            break
          case 'checkbox':
            input = (
              <Checkbox
                disabled={disabled}
                checked={field.value as boolean}
                onChange={(e) => field.onChange(e.target.checked)}
              >
                {label}
              </Checkbox>
            )
            break
          case 'switch':
            input = (
              <Switch
                disabled={disabled}
                checked={field.value as boolean}
                onChange={(checked) => field.onChange(checked)}
              />
            )
            break
          case 'password':
            input = (
              <Input.Password
                {...commonProps}
                prefix={prefix}
                value={field.value as string}
                onChange={(e) => field.onChange(e.target.value)}
                onBlur={field.onBlur}
              />
            )
            break
          case 'email':
            input = (
              <Input
                {...commonProps}
                type="email"
                prefix={prefix}
                value={field.value as string}
                onChange={(e) => field.onChange(e.target.value)}
                onBlur={field.onBlur}
              />
            )
            break
          case 'tel':
            input = (
              <Input
                {...commonProps}
                type="tel"
                prefix={prefix}
                value={field.value as string}
                onChange={(e) => field.onChange(e.target.value)}
                onBlur={field.onBlur}
              />
            )
            break
          default:
            input = (
              <Input
                {...commonProps}
                prefix={prefix}
                value={field.value as string}
                onChange={(e) => field.onChange(e.target.value)}
                onBlur={field.onBlur}
              />
            )
        }

        return (
          <Form.Item
            label={type !== 'checkbox' ? label : undefined}
            validateStatus={error ? 'error' : undefined}
            help={error?.message ?? helperText}
            className={cn('mb-4', className)}
          >
            {input}
          </Form.Item>
        )
      }}
    />
  )
}
