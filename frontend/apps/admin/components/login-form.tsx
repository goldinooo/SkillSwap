"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { ArrowRightIcon, CheckCircleIcon, LockKeyIcon } from "@phosphor-icons/react"
import * as React from "react"
import { useForm } from "react-hook-form"
import { z } from "zod"

import { cn } from "@workspace/ui/lib/utils"

import { Button } from "@workspace/ui/components/button"
import { Card, CardContent } from "@workspace/ui/components/card"
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@workspace/ui/components/field"
import { Input } from "@workspace/ui/components/input"

const loginSchema = z.object({
  email: z.string().email("Enter a valid work email."),
  password: z.string().min(8, "Password must be at least 8 characters."),
})

type LoginValues = z.infer<typeof loginSchema>

export function LoginForm({
  className,
  ...props
}: React.ComponentProps<"div">) {
  const [authError, setAuthError] = React.useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
    },
  })

  function onSubmit(_values: LoginValues) {
    setAuthError("Authentication service is not connected. No session was created.")
  }

  return (
    <div className={cn("flex w-full flex-col gap-6", className)} {...props}>
      <Card className="overflow-hidden border-border/60 bg-card p-0 shadow-2xl shadow-black/10 dark:shadow-black/40">
        <CardContent className="grid p-0 md:grid-cols-2 md:items-stretch">
          <form className="flex items-center p-7 md:p-10" onSubmit={handleSubmit(onSubmit)} noValidate>
            <FieldGroup className="w-full">
              <div className="flex flex-col items-center gap-2 text-center">
                <div className="mb-3 flex size-12 items-center justify-center bg-[#e6a65d] text-lg font-bold text-[#153c35]">S</div>
                <p className="text-xs font-semibold tracking-[0.18em] text-[#a5682d] uppercase">Admin access</p>
                <h1 className="text-3xl font-bold tracking-[-0.03em]">Welcome back</h1>
                <p className="max-w-xs text-sm leading-6 text-balance text-muted-foreground">
                  Sign in to keep the SkillSwap community healthy.
                </p>
              </div>
              <Field>
                <FieldLabel htmlFor="email">Email</FieldLabel>
                <Input
                  id="email"
                  type="email"
                  placeholder="you@skillswap.com"
                  className="h-11 text-sm"
                  aria-invalid={Boolean(errors.email)}
                  {...register("email")}
                />
                {errors.email && <FieldDescription className="text-destructive">{errors.email.message}</FieldDescription>}
              </Field>
              <Field>
                <div className="flex items-center">
                  <FieldLabel htmlFor="password">Password</FieldLabel>
                  <a
                    href="mailto:ops@skillswap.com?subject=Password%20reset"
                    className="ml-auto text-sm underline-offset-2 hover:underline"
                  >
                    Forgot password?
                  </a>
                </div>
                <Input id="password" type="password" className="h-11 text-sm" aria-invalid={Boolean(errors.password)} {...register("password")} />
                {errors.password && <FieldDescription className="text-destructive">{errors.password.message}</FieldDescription>}
              </Field>
              <Field>
                <Button type="submit" disabled={isSubmitting} className="h-11 text-sm bg-[#153c35] text-white hover:bg-[#24564b] dark:bg-[#e6a65d] dark:text-[#153c35] dark:hover:bg-[#f1bf7f]">
                  {isSubmitting ? "Signing in..." : "Sign in"}
                  <ArrowRightIcon className="size-4" />
                </Button>
                {authError && <FieldDescription className="text-destructive">{authError}</FieldDescription>}
              </Field>
              <FieldDescription className="flex items-center justify-center gap-2 text-center"><LockKeyIcon className="size-3.5" />Admin access is restricted to SkillSwap staff.</FieldDescription>
            </FieldGroup>
          </form>
          <div className="relative hidden min-h-[30rem] overflow-hidden bg-[#153c35] p-10 text-[#f3f5ec] md:flex md:flex-col md:justify-between dark:bg-[#102923] lg:p-12">
            <div className="relative z-10 flex items-center gap-3 text-base font-semibold tracking-[0.18em] uppercase"><span className="flex size-10 items-center justify-center bg-[#e6a65d] text-lg text-[#153c35]">S</span>SkillSwap</div>
            <div className="relative z-10"><p className="mb-5 text-xs font-semibold tracking-[0.18em] text-[#e6a65d] uppercase">Operations console</p><h2 className="text-4xl leading-tight font-semibold tracking-[-0.04em]">Keep useful skills moving.</h2><p className="mt-6 max-w-sm text-base leading-7 text-[#c2d1c8]">A clear view of the people, listings, and conversations that make SkillSwap work.</p></div>
            <div className="relative z-10 flex items-center gap-2 text-xs text-[#c2d1c8]"><CheckCircleIcon className="size-4 text-[#e6a65d]" weight="fill" />Trusted by the SkillSwap team</div>
            <div className="absolute -right-28 -bottom-32 size-80 rounded-full border border-[#d6e3cf]/15" />
          </div>
        </CardContent>
      </Card>
      <FieldDescription className="px-6 text-center">
        By clicking continue, you agree to our <a href="#">Terms of Service</a>{" "}
        and <a href="#">Privacy Policy</a>.
      </FieldDescription>
    </div>
  )
}
