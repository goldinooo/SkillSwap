import { ChatIcon } from "@phosphor-icons/react/dist/ssr"
import { Button } from "@workspace/ui/components/button"
import LabNavbar from "./_components/lab-navbar"

export default function LabLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <div className="flex h-screen w-full flex-col gap-0 bg-background">
      {/* Header */}
      <LabNavbar />

      {/* Body */}
      <div className="flex min-h-0 flex-1">
        <main className="min-h-0 flex-1">{children}</main>
        {/* Chat panel (outline only) */}
        <aside className="hidden w-80 shrink-0 flex-col border-l border-border lg:flex">
          <div className="flex h-16 shrink-0 items-center justify-between border-b border-border px-4">
            <div className="flex flex-col">
              <h2 className="text-sm font-semibold">Chat</h2>
            </div>
          </div>

          <div className="flex min-h-0 flex-1 items-center justify-center p-4">
            <p className="text-center text-sm text-muted-foreground">
              Messages will appear here
            </p>
          </div>

          <div className="shrink-0 border-t border-border p-3">
            <div className="flex items-center gap-2">
              <div className="h-9 flex-1 rounded-md border border-input bg-transparent px-3 text-sm leading-9 text-muted-foreground">
                Message Youssef
              </div>
              <Button size="icon" aria-label="Send message" disabled>
                <ChatIcon className="size-4" weight="bold" />
              </Button>
            </div>
          </div>
        </aside>
      </div>
    </div>
  )
}
