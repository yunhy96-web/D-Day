import type { UseFormRegister } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import type { FieldDefinition } from '@/lib/api/categories'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { useCommonCodeGroup } from './commonCodeHooks'

interface Props {
  /** Schema fields from the selected category. */
  fields: FieldDefinition[]
  /** RHF register from the parent form. */
  register: UseFormRegister<any>
  /** Path prefix in the form, e.g. "items.0.attributes" — keys append to this. */
  pathPrefix: string
}

export function DynamicFields({ fields, register, pathPrefix }: Props) {
  if (!fields || fields.length === 0) return null
  return (
    <div className="space-y-3 border-t pt-3">
      {fields.map((field) => (
        <FieldRenderer
          key={field.key}
          field={field}
          register={register}
          path={`${pathPrefix}.${field.key}`}
        />
      ))}
    </div>
  )
}

function FieldRenderer({
  field,
  register,
  path,
}: {
  field: FieldDefinition
  register: UseFormRegister<any>
  path: string
}) {
  const { t } = useTranslation()
  const label = field.labelKey ? t(field.labelKey) : field.key

  if (field.type === 'group' && field.fields?.length) {
    return (
      <div className="space-y-2">
        <div className="text-sm font-medium">{label}</div>
        <div className="grid grid-cols-3 gap-2 pl-3 border-l-2 border-muted">
          {field.fields.map((sub) => (
            <FieldRenderer
              key={sub.key}
              field={sub}
              register={register}
              path={`${path}.${sub.key}`}
            />
          ))}
        </div>
      </div>
    )
  }

  if (field.type === 'select' && field.optionsCode) {
    return <SelectField field={field} register={register} path={path} label={label} />
  }

  if (field.type === 'decimal') {
    return (
      <div className="space-y-1">
        <Label>{label}{field.required && '*'}</Label>
        <Input
          type="number"
          step={field.step ?? 0.01}
          min={field.min ?? undefined}
          max={field.max ?? undefined}
          {...register(path, {
            setValueAs: (v) => (v === '' || v == null ? undefined : Number(v)),
          })}
        />
      </div>
    )
  }

  // default: text
  return (
    <div className="space-y-1">
      <Label>{label}{field.required && '*'}</Label>
      <Input type="text" {...register(path)} />
    </div>
  )
}

function SelectField({
  field,
  register,
  path,
  label,
}: {
  field: FieldDefinition
  register: UseFormRegister<any>
  path: string
  label: string
}) {
  const { data: options, isLoading } = useCommonCodeGroup(field.optionsCode)
  return (
    <div className="space-y-1">
      <Label>{label}{field.required && '*'}</Label>
      <Select {...register(path)} disabled={isLoading}>
        <option value="">{isLoading ? '…' : '—'}</option>
        {options?.map((o) => (
          <option key={o.code} value={o.code}>
            {o.name}
          </option>
        ))}
      </Select>
    </div>
  )
}
