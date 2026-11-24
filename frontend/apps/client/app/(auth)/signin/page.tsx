import React from "react"
import AuthLayout from "../_shared/components/auth-layout"
import AuthField from "../_shared/components/auth-field"
import { Field, FieldSeparator } from "@workspace/ui/components/field"
import { Button } from "@workspace/ui/components/button"
import OauthGroup from "../_shared/components/oauth-group"

export default function Page() {
  return (
    <AuthLayout
      image={{
        src: "./signin.jpg",
        alt: "signin image holder",
      }}
    >
      <Field>
        <h1 className="flex items-center gap-1 text-2xl font-black uppercase">
          Sign in to your account.
          <span className="h-8 w-0.5 animate-caret-blink bg-primary" />
        </h1>
      </Field>

      <AuthField
        label="Username/Email"
        id="username-email"
        type="text"
        placeholder="username or email"
        required
      />

      <AuthField
        label="Password"
        id="password"
        type="password"
        placeholder="password"
        required
      />

      <Button type="submit">Signin</Button>
      <FieldSeparator className="*:data-[slot=field-separator-content]:bg-card">
        Or continue with
      </FieldSeparator>
      <OauthGroup />
      <div className="">
        Don't have an account yet?{" "}
        <a href="/signup" className="font-semibold text-primary underline">
          Create one!
        </a>
      </div>
    </AuthLayout>
  )
}
