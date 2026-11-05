import {
  BookIcon,
  CalendarIcon,
  ClockCounterClockwiseIcon,
  CompassIcon,
  HouseIcon,
  TrophyIcon,
  UserIcon,
  type Icon,
} from "@phosphor-icons/react"

export type DashboardNavItem = {
  href: string
  label: string
  icon: Icon
}

export const dashboardNavigation: DashboardNavItem[] = [
  {
    href: "/dashboard",
    label: "Home",
    icon: HouseIcon,
  },
  {
    href: "/dashboard/discover",
    label: "Discover",
    icon: CompassIcon,
  },
  {
    href: "/dashboard/lessons",
    label: "My lessons",
    icon: BookIcon,
  },
  {
    href: "/dashboard/calendar",
    label: "Calendar",
    icon: CalendarIcon,
  },
  {
    href: "/dashboard/achievements",
    label: "Achievements",
    icon: TrophyIcon,
  },
  {
    href: "/dashboard/history",
    label: "History",
    icon: ClockCounterClockwiseIcon,
  },
  {
    href: "/dashboard/profile",
    label: "Profile",
    icon: UserIcon,
  },
]

export function isCurrentDashboardPage(pathname: string, href: string) {
  if (href === "/dashboard") {
    return pathname === "/dashboard"
  }
  return pathname === href || pathname.startsWith(`${href}/`)
}
