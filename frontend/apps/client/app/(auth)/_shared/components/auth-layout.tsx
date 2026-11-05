import { Card, CardContent } from "@workspace/ui/components/card"
import { FieldDescription, FieldGroup } from "@workspace/ui/components/field"
import { cn } from "@workspace/ui/lib/utils"

type AuthWrapperProps = {
  image?: React.ComponentProps<"img">
  form?: React.ComponentProps<"form">
  children?: React.ReactNode
} & React.ComponentProps<"div">

export default function AuthLayout({
  children,
  className,
  image,
  form,
  ...props
}: AuthWrapperProps) {
  return (
    <div className="flex min-h-svh flex-col items-center justify-center bg-muted p-6 md:p-10">
      <div className="w-full max-w-sm md:max-w-4xl">
        <div className={cn("flex flex-col gap-6", className)} {...props}>
          <Card className="overflow-hidden p-0">
            <CardContent className="grid p-0 md:grid-cols-2">
              <form {...form}>
                <FieldGroup className="p-6 md:p-8">{children}</FieldGroup>
              </form>
              <div className="relative hidden bg-muted md:block">
                <img
                  {...image}
                  className="absolute inset-0 h-full w-full object-cover dark:brightness-[0.2] dark:grayscale"
                />
              </div>
            </CardContent>
          </Card>
          <FieldDescription className="px-6 text-center">
            By continuing, you agree to our <a href="#">Terms of Service</a> and{" "}
            <a href="#">Privacy Policy</a>.
          </FieldDescription>
        </div>
      </div>
    </div>
  )
}
