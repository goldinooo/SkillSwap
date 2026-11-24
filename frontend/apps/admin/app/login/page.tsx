import { LoginForm } from "@/components/login-form"

export default function LoginPage() {
  return (
    <div className="min-h-svh overflow-y-auto bg-muted px-4 py-8 sm:px-6 md:px-10">
      <div className="mx-auto flex min-h-[calc(100svh-4rem)] w-full max-w-5xl items-center justify-center">
        <LoginForm />
      </div>
    </div>
  )
}
