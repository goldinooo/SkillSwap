"use client"

import Link from "next/link"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "@workspace/ui/components/sidebar"
import * as React from "react"
import AppLogo from "./app-logo"
import { NavUser } from "./app-sidebar-user"

import { useCurrentPage } from "@/hooks/use-current-page"
import {
  dashboardNavigation,
  isCurrentDashboardPage,
  type DashboardNavItem,
} from "@/lib/dashboard-navigation"
import { cn } from "@/lib/utils"

const user = {
  name: "shadcn",
  email: "m@example.com",
  avatar: "/avatars/shadcn.jpg",
}

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  const { pathname } = useCurrentPage()

  return (
    <Sidebar collapsible="icon" className="data-collapsed" {...props}>
      <SidebarHeader className="p-2">
        <AppLogo />
      </SidebarHeader>
      <SidebarContent className="pt-14">
        <SidebarMenu className="p-2">
          {dashboardNavigation.map((item: DashboardNavItem) => {
            const NavIcon = item.icon
            const active = isCurrentDashboardPage(pathname, item.href)
            return (
              <SidebarMenuItem
                key={item.href}
                className="relative select-none"
                draggable={false}
              >
                {active && (
                  <span className="absolute top-1/2 -left-4 h-8 w-2.5 -translate-y-1/2 rounded-r bg-primary" />
                )}
                <SidebarMenuButton
                  size="lg"
                  isActive={active}
                  render={<Link href={item.href} />}
                  className={
                    active
                      ? cn(
                          "data-active:bg-primary/10 data-active:hover:bg-primary",
                          "data-active:font-bold data-active:text-primary"
                        )
                      : undefined
                  }
                >
                  {NavIcon && (
                    <NavIcon
                      weight={active ? "fill" : "regular"}
                      className="size-6!"
                    />
                  )}
                  <span className="text-lg">{item.label}</span>
                </SidebarMenuButton>
              </SidebarMenuItem>
            )
          })}
        </SidebarMenu>
      </SidebarContent>
      <SidebarFooter>
        <NavUser user={user} />
      </SidebarFooter>
      {/* <SidebarRail /> */}
    </Sidebar>
  )
}
