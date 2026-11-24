"use client"

import * as React from "react"

import { NavMain } from "@/components/nav-main"
import { NavProjects } from "@/components/nav-projects"
import { NavUser } from "@/components/nav-user"
import { TeamSwitcher } from "@/components/team-switcher"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarRail,
} from "@workspace/ui/components/sidebar"
import { RowsIcon, WaveformIcon, CommandIcon, UsersThreeIcon, FlagIcon, BookOpenIcon, GearIcon, ChartPieIcon, ChartLineUpIcon, ArrowsLeftRightIcon, BroadcastIcon, ShieldCheckIcon, ClipboardTextIcon } from "@phosphor-icons/react"

const data = {
  user: {
    name: "Maya Chen",
    email: "maya@skillswap.com",
    avatar: "/avatars/shadcn.jpg",
  },
  teams: [
    {
      name: "SkillSwap Admin",
      logo: <RowsIcon />,
      plan: "Operations",
    },
    {
      name: "Trust & Safety",
      logo: <WaveformIcon />,
      plan: "Moderation",
    },
    {
      name: "Community",
      logo: <CommandIcon />,
      plan: "Engagement",
    },
  ],
  navMain: [
    {
      title: "Overview",
      url: "/dashboard",
      icon: <ChartPieIcon />,
      isActive: true,
      items: [
        {
          title: "Dashboard",
          url: "/dashboard",
        },
        {
          title: "Insights",
          url: "/dashboard/insights",
        },
      ],
    },
    {
      title: "Community",
      url: "/community",
      icon: <UsersThreeIcon />,
      items: [
        {
          title: "Members",
          url: "/community/members",
        },
        {
          title: "Reports",
          url: "/community/reports",
        },
        {
          title: "Spotlight",
          url: "/community/spotlight",
        },
      ],
    },
    {
      title: "Exchanges",
      url: "/marketplace/exchanges",
      icon: <ArrowsLeftRightIcon />,
      items: [
        { title: "Skill exchanges", url: "/marketplace/exchanges" },
        { title: "Learning sessions", url: "/learning-sessions" },
      ],
    },
    {
      title: "Marketplace",
      url: "/marketplace/listings",
      icon: <BookOpenIcon />,
      items: [
        {
          title: "Skill listings",
          url: "/marketplace/listings",
        },
        {
          title: "Categories",
          url: "/marketplace/categories",
        },
        {
          title: "Review queue",
          url: "/marketplace/review",
        },
      ],
    },
    {
      title: "Operations",
      url: "/activity",
      icon: <BroadcastIcon />,
      items: [
        { title: "Live activity", url: "/activity" },
        { title: "Support inbox", url: "/support" },
      ],
    },
    {
      title: "Security",
      url: "/sessions",
      icon: <ShieldCheckIcon />,
      items: [
        { title: "Authentication sessions", url: "/sessions" },
        { title: "Audit logs", url: "/audit-logs" },
      ],
    },
    {
      title: "Settings",
      url: "/settings",
      icon: <GearIcon />,
      items: [
        {
          title: "General",
          url: "/settings",
        },
        {
          title: "Staff access",
          url: "/settings/staff",
        },
        {
          title: "Notifications",
          url: "/settings/notifications",
        },
      ],
    },
  ],
  projects: [
    {
      name: "Review queue",
      url: "/marketplace/review",
      icon: <FlagIcon />,
    },
    {
      name: "Growth report",
      url: "/dashboard/insights",
      icon: <ChartLineUpIcon />,
    },
    {
      name: "Audit logs",
      url: "/audit-logs",
      icon: <ClipboardTextIcon />,
    },
  ],
}

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  return (
    <Sidebar collapsible="icon" {...props}>
      <SidebarHeader>
        <TeamSwitcher teams={data.teams} />
      </SidebarHeader>
      <SidebarContent>
        <NavMain items={data.navMain} />
        <NavProjects projects={data.projects} />
      </SidebarContent>
      <SidebarFooter>
        <NavUser user={data.user} />
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  )
}
