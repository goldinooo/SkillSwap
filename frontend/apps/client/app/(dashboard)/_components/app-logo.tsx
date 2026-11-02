import { SidebarMenuButton } from "@workspace/ui/components/sidebar"
import { Repeat2 } from "lucide-react"
import React from "react"

export default function AppLogo() {
  return (
    <SidebarMenuButton size={"lg"}>
      <div className="flex items-center justify-start gap-2 text-2xl font-black">
        <Repeat2 className="size-10! stroke-primary" strokeWidth={3} />
        <span>SKILLSWAP</span>
      </div>
    </SidebarMenuButton>
  )
}
