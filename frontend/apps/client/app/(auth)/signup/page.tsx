import { Button } from "@workspace/ui/components/button"
import { Field, FieldSeparator } from "@workspace/ui/components/field"
import AuthField from "../_shared/auth-field"
import AuthLayout from "../_shared/auth-layout"
import OauthGroup from "../_shared/oauth-group"

export default function Page() {
  return (
    <AuthLayout
      image={{
        src: "./signup.jpg",
        alt: "signup image",
      }}
      form={{}}
    >
      <Field>
        <h1 className="flex items-center gap-1 text-3xl font-black uppercase">
          Create an account.
          <span className="h-8 w-0.5 animate-caret-blink bg-primary" />
        </h1>
        <span>Fill out the fields bellow to create an account.</span>
      </Field>
      <AuthField
        label="Email"
        id="email"
        type="email"
        placeholder="me@example.com"
        required
      />
      <AuthField
        label="Username"
        id="username"
        type="text"
        placeholder="crazyname"
        required
      />
      <AuthField
        label="Password"
        id="password"
        type="password"
        placeholder="Password"
        required
      />
      <AuthField
        label="Confirm password"
        id="confirm-password"
        type="password"
        placeholder="Confirm password"
        required
      />
      <Button type="submit">Create account</Button>
      <FieldSeparator className="*:data-[slot=field-separator-content]:bg-card">
        Or continue with
      </FieldSeparator>
      <OauthGroup />
      <div className="">
        Already have an account?{" "}
        <a href="/signin" className="font-semibold text-primary underline">
          Signin
        </a>
      </div>
    </AuthLayout>
  )
}
