"use client"

import { currentTab, LabNavigationItems } from "@/lib/lab-navigation"
import {
  ChatIcon,
  PaletteIcon,
  ShuffleIcon,
  SignOutIcon,
  VideoCameraIcon,
} from "@phosphor-icons/react"
import {
  Avatar,
  AvatarFallback,
  AvatarGroup,
} from "@workspace/ui/components/avatar"
import { Badge } from "@workspace/ui/components/badge"
import { Button } from "@workspace/ui/components/button"
import { ButtonGroup } from "@workspace/ui/components/button-group"
import Link from "next/link"
import { usePathname } from "next/navigation"

export default function LabNavbar() {
  const pathname = usePathname()

  return (
    <header className="flex h-16 shrink-0 items-center justify-between gap-4 border-b border-border px-6">
      {/* Left: logo + session info */}
      <div className="flex items-center gap-4">
        <ShuffleIcon className="size-6 shrink-0 text-primary" weight="bold" />
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
      <ButtonGroup className="rounded-lg border border-border">
        {LabNavigationItems.map((item) => {
          const isActive = currentTab(pathname, item.href)

          return (
            <Button
              key={item.href}
              variant={isActive ? "secondary" : "ghost"}
              render={<Link href={item.href} />}
              nativeButton={false}
              aria-current={isActive ? "page" : undefined}
            >
              <item.icon className="size-4" weight="bold" />
              {item.label}
            </Button>
          )
        })}
      </ButtonGroup>

      {/* Right: participants + actions */}
      <div className="flex items-center gap-3">
        <AvatarGroup>
          <Avatar className="size-8">
            <AvatarFallback className="text-xs font-medium">AM</AvatarFallback>
          </Avatar>
          <Avatar className="size-8">
            <AvatarFallback className="text-xs font-medium">YB</AvatarFallback>
          </Avatar>
        </AvatarGroup>

        <Button variant="outline" className="relative gap-2">
          <ChatIcon className="size-4" weight="bold" />
          Chat
          <Badge className="absolute -top-1 -right-1 flex size-5 items-center justify-center rounded-full p-0">
            2
          </Badge>
        </Button>

        <Button variant="destructive" className="gap-2">
          <SignOutIcon className="size-4" weight="bold" />
          End session
        </Button>
      </div>
    </header>
  )
}
