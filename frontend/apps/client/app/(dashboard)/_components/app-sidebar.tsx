"use client"

import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "@workspace/ui/components/sidebar"
import { Book, Calendar, Compass, Home, Trophy, User } from "lucide-react"
import { LucideIcon } from "lucide-react/types"
import * as React from "react"
import AppLogo from "./app-logo"
import { NavUser } from "./app-sidebar-user"

// This is sample data.
const user = {
  name: "shadcn",
  email: "m@example.com",
  avatar: "/avatars/shadcn.jpg",
}

const sidebar: {
  url: string
  icon: LucideIcon
  label: string
}[] = [
  {
    url: "#",
    icon: Home,
    label: "Home",
  },
  {
    url: "#",
    icon: Compass,
    label: "Discover",
  },
  {
    url: "#",
    icon: Book,
    label: "My lessons",
  },
  {
    url: "#",
    icon: Calendar,
    label: "Calendar",
  },
  {
    url: "#",
    icon: Trophy,
    label: "Achievements",
  },
  {
    url: "#",
    icon: User,
    label: "Profile",
  },
]

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  const { isMobile } = useSidebar()
  return (
    <Sidebar collapsible="none" {...props}>
      <SidebarHeader className="p-0">
        <AppLogo />
      </SidebarHeader>
      <SidebarContent className="pt-14">
        <SidebarMenu>
          {sidebar.map((item, index) => (
            <SidebarMenuItem key={index}>
              <SidebarMenuButton size={"lg"} render={<a href={item.url} />}>
                <item.icon className="size-6!" />
                <span className="text-lg">{item.label}</span>
              </SidebarMenuButton>
            </SidebarMenuItem>
          ))}
        </SidebarMenu>
      </SidebarContent>
      <SidebarFooter>
        <NavUser user={user} />
      </SidebarFooter>
      {/* <SidebarRail /> */}
    </Sidebar>
  )
}
