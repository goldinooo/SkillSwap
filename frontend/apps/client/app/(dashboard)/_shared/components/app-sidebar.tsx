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

const user = {
  name: "shadcn",
  email: "m@example.com",
  avatar: "/avatars/shadcn.jpg",
}

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  const { pathname } = useCurrentPage()

  return (
    <Sidebar collapsible="offcanvas" {...props}>
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
                className="select-none"
                draggable={false}
              >
                <SidebarMenuButton
                  size="lg"
                  isActive={active}
                  render={<Link href={item.href} />}
                  className={
                    active
                      ? "data-active:bg-primary/10 data-active:font-bold data-active:text-primary data-active:hover:bg-primary"
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
