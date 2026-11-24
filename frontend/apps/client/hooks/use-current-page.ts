"use client"

import {
  dashboardNavigation,
  isCurrentDashboardPage,
} from "@/lib/dashboard-navigation"
import { usePathname } from "next/navigation"

export function useCurrentPage() {
  const pathname = usePathname()
  const page = dashboardNavigation.find((item) =>
    isCurrentDashboardPage(pathname, item.href)
  )

  return {
    pathname,
    title:
      page?.label ?? (pathname === "/lab" ? "Collaborative workspace" : ""),
  }
}
