"use client"

import * as React from "react"
import { DatabaseIcon, FunnelIcon, MagnifyingGlassIcon, PlusIcon, SlidersHorizontalIcon } from "@phosphor-icons/react"

import { Badge } from "@workspace/ui/components/badge"
import { Button } from "@workspace/ui/components/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@workspace/ui/components/card"
import { Input } from "@workspace/ui/components/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@workspace/ui/components/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@workspace/ui/components/table"

export type ResourceColumn = {
  label: string
  key: string
}

export type ResourceStat = {
  label: string
  detail: string
}

export type ResourceFilter = {
  label: string
  value: string
  options: { label: string; value: string }[]
}

type AdminResourcePageProps = {
  eyebrow: string
  title: string
  description: string
  stats?: ResourceStat[]
  columns: ResourceColumn[]
  filters?: ResourceFilter[]
  emptyTitle: string
  emptyDescription: string
  actionLabel?: string
  children?: React.ReactNode
}

export function AdminResourcePage({
  eyebrow,
  title,
  description,
  stats = [],
  columns,
  filters = [],
  emptyTitle,
  emptyDescription,
  actionLabel,
  children,
}: AdminResourcePageProps) {
  const [query, setQuery] = React.useState("")
  const [activeFilters, setActiveFilters] = React.useState<Record<string, string>>(
    Object.fromEntries(filters.map((filter) => [filter.label, filter.value]))
  )

  return (
    <main className="flex flex-1 flex-col gap-6 bg-background p-4 pt-6 md:p-8 md:pt-7">
      <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
        <div>
          <p className="mb-2 text-[11px] font-semibold tracking-[0.16em] text-[#a5682d] uppercase">{eyebrow}</p>
          <h1 className="text-2xl font-semibold tracking-[-0.04em] text-foreground">{title}</h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-muted-foreground">{description}</p>
        </div>
        {actionLabel && <Button className="w-fit rounded-none" disabled><PlusIcon />{actionLabel}</Button>}
      </div>

      {stats.length > 0 && (
        <div className="grid grid-cols-1 gap-4 @xl/main:grid-cols-2 @5xl/main:grid-cols-4">
          {stats.map((stat) => (
            <Card key={stat.label} className="rounded-none shadow-none">
              <CardHeader className="gap-2">
                <CardDescription>{stat.label}</CardDescription>
                <CardTitle className="text-2xl tabular-nums">—</CardTitle>
                <p className="text-xs text-muted-foreground">{stat.detail}</p>
              </CardHeader>
            </Card>
          ))}
        </div>
      )}

      {children}

      <Card className="rounded-none shadow-none">
        <CardHeader className="gap-4 border-b sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle className="text-base">Directory</CardTitle>
            <CardDescription>Connect an authenticated admin endpoint to load and manage records.</CardDescription>
          </div>
          <Badge variant="outline" className="w-fit gap-1.5 rounded-none"><DatabaseIcon />API not connected</Badge>
        </CardHeader>
        <CardContent className="space-y-4 p-4 md:p-6">
          <div className="flex flex-col gap-3 lg:flex-row">
            <div className="relative min-w-0 flex-1">
              <MagnifyingGlassIcon className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input value={query} onChange={(event) => setQuery(event.target.value)} placeholder={`Search ${title.toLowerCase()}...`} className="h-10 rounded-none pl-9" aria-label={`Search ${title}`} />
            </div>
            {filters.map((filter) => (
              <Select key={filter.label} value={activeFilters[filter.label]} onValueChange={(value) => value && setActiveFilters((current) => ({ ...current, [filter.label]: value }))}>
                <SelectTrigger className="h-10 w-full rounded-none lg:w-44" aria-label={filter.label}><FunnelIcon /><SelectValue placeholder={filter.label} /></SelectTrigger>
                <SelectContent>{filter.options.map((option) => <SelectItem key={option.value} value={option.value}>{option.label}</SelectItem>)}</SelectContent>
              </Select>
            ))}
            <Button variant="outline" className="h-10 rounded-none" disabled><SlidersHorizontalIcon />More filters</Button>
          </div>

          {query && <p className="text-xs text-muted-foreground">Searching for “{query}”. Results will appear when the API is connected.</p>}

          <div className="overflow-hidden border">
            <Table>
              <TableHeader><TableRow>{columns.map((column) => <TableHead key={column.key}>{column.label}</TableHead>)}</TableRow></TableHeader>
              <TableBody>
                <TableRow><TableCell colSpan={columns.length} className="h-48 text-center">
                  <div className="mx-auto flex max-w-sm flex-col items-center gap-2">
                    <DatabaseIcon className="size-6 text-muted-foreground" />
                    <p className="text-sm font-medium text-foreground">{emptyTitle}</p>
                    <p className="text-xs leading-5 text-muted-foreground">{emptyDescription}</p>
                  </div>
                </TableCell></TableRow>
              </TableBody>
            </Table>
          </div>
          <div className="flex items-center justify-between text-xs text-muted-foreground"><span>0 records</span><span>Pagination will be server-side</span></div>
        </CardContent>
      </Card>
    </main>
  )
}
