import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from "@workspace/ui/components/field"
import { Input } from "@workspace/ui/components/input"
import React from "react"

type AuthFieldProps = React.ComponentProps<typeof Input> & {
  label: string
  description?: string
  error?: string
}

export default function AuthField({
  label,
  description,
  error,
  id,
  ...inputProps
}: AuthFieldProps) {
  const inputId = id || React.useId()
  const errorId = error ? `${inputId}-error` : undefined

  return (
    <Field data-invalid={error ? true : false}>
      <FieldLabel htmlFor={inputId}>{label}</FieldLabel>
      <Input id={inputId} aria-invalid={!!error} {...inputProps} />
      {description && <FieldDescription>{description}</FieldDescription>}
      {error && <FieldError id={errorId}>{error}</FieldError>}
    </Field>
  )
}
