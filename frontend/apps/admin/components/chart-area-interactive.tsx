"use client"

import * as React from "react"
import { ChartLineUpIcon, DatabaseIcon } from "@phosphor-icons/react"

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@workspace/ui/components/card"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@workspace/ui/components/select"

const ranges = [
  { label: "Last 24 hours", value: "24h" },
  { label: "Last 7 days", value: "7d" },
  { label: "Last 30 days", value: "30d" },
  { label: "Last 90 days", value: "90d" },
  { label: "Custom range", value: "custom" },
]

export function ChartAreaInteractive() {
  const [timeRange, setTimeRange] = React.useState("30d")
  const selectedRange = ranges.find((range) => range.value === timeRange)

  return (
    <Card className="rounded-none shadow-none">
      <CardHeader className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <CardTitle className="flex items-center gap-2"><ChartLineUpIcon className="size-4 text-[#a5682d]" />Community activity</CardTitle>
          <CardDescription className="mt-1">Registrations, active users, and completed exchanges over time.</CardDescription>
        </div>
        <Select value={timeRange} onValueChange={(value) => value && setTimeRange(value)}>
          <SelectTrigger className="w-full rounded-none sm:w-40" aria-label="Select analytics date range">
            <SelectValue placeholder="Select range" />
          </SelectTrigger>
          <SelectContent>
            {ranges.map((range) => <SelectItem key={range.value} value={range.value}>{range.label}</SelectItem>)}
          </SelectContent>
        </Select>
      </CardHeader>
      <CardContent>
        <div className="flex min-h-56 flex-col items-center justify-center border border-dashed bg-muted/20 px-6 text-center">
          <DatabaseIcon className="mb-3 size-6 text-muted-foreground" />
          <p className="text-sm font-medium text-foreground">No activity data available</p>
          <p className="mt-1 max-w-md text-xs leading-5 text-muted-foreground">The {selectedRange?.label.toLowerCase()} view will populate after event tracking and the admin analytics endpoint are configured.</p>
        </div>
      </CardContent>
    </Card>
  )
}
