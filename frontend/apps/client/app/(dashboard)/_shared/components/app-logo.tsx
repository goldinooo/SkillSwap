import { ShuffleIcon } from "@phosphor-icons/react"
import { SidebarMenuButton } from "@workspace/ui/components/sidebar"
import React from "react"

export default function AppLogo() {
  return (
    <SidebarMenuButton size={"lg"}>
      <div className="flex items-center justify-center gap-2 text-2xl font-black">
        <ShuffleIcon className="size-8! shrink-0 fill-primary group-data-[collapsible=icon]:size-5!" />
        <span className="group-data-[collapsible=icon]:hidden">SKILLSWAP</span>
      </div>
    </SidebarMenuButton>
  )
}
