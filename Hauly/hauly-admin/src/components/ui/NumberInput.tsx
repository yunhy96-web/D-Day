import { Controller, type Control, type FieldPath, type FieldValues } from 'react-hook-form'
import { NumericFormat } from 'react-number-format'
import { Input } from '@/components/ui/input'

interface Props<TFieldValues extends FieldValues> {
  name: FieldPath<TFieldValues>
  control: Control<TFieldValues>
  /** Number of decimal places allowed. 0 = integer-only. */
  decimals?: number
  min?: number
  max?: number
  placeholder?: string
  id?: string
  className?: string
  /**
   * 'string' (default) sends raw digit strings ("1234.56") to the form value —
   * matches schemas like priceRegex or zod.coerce.number().
   * 'number' sends a JS number — for fields whose schema expects number directly
   * (e.g. dynamic decimal attributes serialized into JSON).
   */
  valueType?: 'string' | 'number'
}

export function NumberInput<TFieldValues extends FieldValues>({
  name,
  control,
  decimals = 0,
  min,
  max,
  placeholder,
  id,
  className,
  valueType = 'string',
}: Props<TFieldValues>) {
  return (
    <Controller
      name={name}
      control={control}
      render={({ field }) => (
        <NumericFormat
          customInput={Input}
          getInputRef={field.ref}
          id={id}
          className={className}
          placeholder={placeholder}
          inputMode={decimals > 0 ? 'decimal' : 'numeric'}
          thousandSeparator=","
          decimalScale={decimals}
          allowNegative={false}
          value={field.value ?? ''}
          onValueChange={(values) => {
            if (values.value === '') {
              field.onChange(valueType === 'number' ? undefined : '')
              return
            }
            field.onChange(valueType === 'number' ? Number(values.value) : values.value)
          }}
          onBlur={field.onBlur}
          isAllowed={(values) => {
            if (values.floatValue == null) return true
            if (min != null && values.floatValue < min) return false
            if (max != null && values.floatValue > max) return false
            return true
          }}
        />
      )}
    />
  )
}
