"use client"

import { AppSidebar } from "@/components/app-sidebar"
import { ChartAreaInteractive } from "@/components/chart-area-interactive"
import { SectionCards } from "@/components/section-cards"
import { SiteHeader } from "@/components/site-header"
import { ArrowUpRightIcon, DatabaseIcon, ShieldCheckIcon } from "@phosphor-icons/react"
import { Button } from "@workspace/ui/components/button"
import { Card, CardContent } from "@workspace/ui/components/card"
import { SidebarInset, SidebarProvider } from "@workspace/ui/components/sidebar"

export default function Page() {
  return (
    <SidebarProvider>
      <AppSidebar />
      <SidebarInset>
        <SiteHeader />
        <main className="flex flex-1 flex-col gap-6 bg-background p-4 pt-6 md:p-8 md:pt-7">
          <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
            <div>
              <p className="mb-2 text-[11px] font-semibold tracking-[0.16em] text-[#a5682d] uppercase">Operations overview</p>
              <h1 className="text-2xl font-semibold tracking-[-0.04em] text-foreground">Good morning, Maya.</h1>
              <p className="mt-2 text-sm text-muted-foreground">Your control center for the SkillSwap community.</p>
            </div>
            <Button variant="outline" className="w-fit rounded-none bg-card" disabled>
              <ArrowUpRightIcon />View live marketplace
            </Button>
          </div>

          <Card className="rounded-none border-[#e6a65d]/40 bg-[#fff9ef] shadow-none dark:bg-[#2b2419]">
            <CardContent className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between">
              <div className="flex gap-3">
                <DatabaseIcon className="mt-0.5 size-5 shrink-0 text-[#a5682d]" />
                <div>
                  <p className="font-medium text-foreground">Connect your platform data</p>
                  <p className="mt-1 text-sm leading-6 text-muted-foreground">Metrics and moderation records will appear here once an authenticated admin API is connected. No placeholder values are shown.</p>
                </div>
              </div>
              <Button variant="outline" className="w-full shrink-0 rounded-none bg-background sm:w-auto" disabled>Configure data source</Button>
            </CardContent>
          </Card>

          <SectionCards />

          <ChartAreaInteractive />

          <section className="space-y-3">
            <div className="flex items-start gap-3">
              <ShieldCheckIcon className="mt-0.5 size-5 text-muted-foreground" />
              <div><h2 className="text-sm font-semibold text-foreground">Moderation queue</h2><p className="mt-1 text-xs text-muted-foreground">Reports and listings requiring an administrator decision.</p></div>
            </div>
            <Card className="rounded-none shadow-none">
              <CardContent className="flex min-h-40 flex-col items-center justify-center px-6 text-center">
                <p className="text-sm font-medium text-foreground">No moderation records available</p>
                <p className="mt-1 text-xs text-muted-foreground">Reports and review items will appear here when the moderation service is connected.</p>
              </CardContent>
            </Card>
          </section>
        </main>
      </SidebarInset>
    </SidebarProvider>
  )
}
