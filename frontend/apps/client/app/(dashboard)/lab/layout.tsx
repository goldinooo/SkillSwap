"use client"

import {
  Avatar,
  AvatarFallback,
  AvatarGroup,
} from "@workspace/ui/components/avatar"
import { Badge } from "@workspace/ui/components/badge"
import { Button } from "@workspace/ui/components/button"
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@workspace/ui/components/tabs"
import {
  Chat,
  Code,
  Palette,
  Shuffle,
  SignOut,
  VideoCamera,
} from "@phosphor-icons/react"

export default function LabLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <Tabs
      defaultValue="canvas"
      className="flex h-screen w-full flex-col gap-0 bg-background"
    >
      {/* Header */}
      <header className="flex h-16 shrink-0 items-center justify-between gap-4 border-b border-border px-6">
        {/* Left: logo + session info */}
        <div className="flex items-center gap-4">
          <Shuffle className="size-6 shrink-0 text-primary" weight="bold" />
          <div className="flex items-center gap-2">
            <span className="size-2 rounded-full bg-destructive" />
            <span className="font-heading text-sm font-medium tabular-nums">
              24:10
            </span>
          </div>
          <div className="h-4 w-px bg-border" />
          <div className="flex min-w-0 flex-col">
            <h1 className="truncate text-sm font-semibold">Next.js Q&A</h1>
            <p className="truncate text-xs text-muted-foreground">
              with Youssef B.
            </p>
          </div>
        </div>

        {/* Center: tabs */}
        <TabsList>
          <TabsTrigger
            value="canvas"
            className="gap-2 px-3 data-active:bg-primary data-active:text-primary-foreground"
          >
            <Palette className="size-4" weight="bold" />
            Canvas
          </TabsTrigger>
          <TabsTrigger
            value="video"
            className="gap-2 px-3 data-active:bg-primary data-active:text-primary-foreground"
          >
            <VideoCamera className="size-4" weight="bold" />
            Video call
          </TabsTrigger>
          <TabsTrigger
            value="code"
            className="gap-2 px-3 data-active:bg-primary data-active:text-primary-foreground"
          >
            <Code className="size-4" weight="bold" />
            Code editor
          </TabsTrigger>
        </TabsList>

        {/* Right: participants + actions */}
        <div className="flex items-center gap-3">
          <AvatarGroup>
            <Avatar className="size-8">
              <AvatarFallback className="text-xs font-medium">
                AM
              </AvatarFallback>
            </Avatar>
            <Avatar className="size-8">
              <AvatarFallback className="text-xs font-medium">
                YB
              </AvatarFallback>
            </Avatar>
          </AvatarGroup>

          <Button variant="outline" className="relative gap-2">
            <Chat className="size-4" weight="bold" />
            Chat
            <Badge className="absolute -top-1 -right-1 flex size-5 items-center justify-center rounded-full p-0">
              2
            </Badge>
          </Button>

          <Button variant="destructive" className="gap-2">
            <SignOut className="size-4" weight="bold" />
            End session
          </Button>
        </div>
      </header>

      {/* Body */}
      <div className="flex min-h-0 flex-1">
        <TabsContent value="canvas" className="min-h-0 flex-1">
          {children}
        </TabsContent>
        <TabsContent value="video" className="min-h-0 flex-1">
          <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
            Video call view
          </div>
        </TabsContent>
        <TabsContent value="code" className="min-h-0 flex-1">
          <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
            Code editor view
          </div>
        </TabsContent>

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
                <Chat className="size-4" weight="bold" />
              </Button>
            </div>
          </div>
        </aside>
      </div>
    </Tabs>
  )
}
