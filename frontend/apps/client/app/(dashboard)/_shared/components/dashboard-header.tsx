"use client"

import { useCurrentPage } from "@/hooks/use-current-page"
import { Button } from "@workspace/ui/components/button"
import { Badge } from "@workspace/ui/components/badge"
import { SidebarTrigger } from "@workspace/ui/components/sidebar"
import { Bell } from "lucide-react"

export function DashboardHeader() {
  const { title } = useCurrentPage()

  return (
    <header className="flex h-16 shrink-0 items-center justify-between gap-2 px-4">
      <SidebarTrigger />
      <Button size={"icon"} variant={"ghost"} className="relative">
        <Bell className="size-4" />
        <Badge
          variant={"default"}
          className="absolute top-0 right-0 h-3.5 w-3.5"
        >
          1
        </Badge>
      </Button>
    </header>
  )
}
