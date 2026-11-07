import CalendarHeader from "./_components/calendar-header"
import CalendarProvider from "./_components/calendar-provider"
import { CalendarSystem } from "./_components/event-calendar"

export default function CalendarPage() {
  return (
    <div className="flex min-h-0 min-w-0 flex-1 flex-col gap-4">
      <div className="flex shrink-0 flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight">Calendar</h1>
        <p className="text-muted-foreground">
          Manage your availability and keep track of your sessions.
        </p>
      </div>
      <div className="flex shrink-0 flex-wrap items-center gap-x-5 gap-y-2 text-sm">
        <span className="flex items-center gap-2">
          <span aria-hidden className="size-2.5 rounded-full bg-(--chart-3)" />
          Availability
          <span className="text-muted-foreground">· editable</span>
        </span>
        <span className="flex items-center gap-2">
          <span aria-hidden className="size-2.5 rounded-full bg-(--chart-1)" />
          Teaching
          <span className="text-muted-foreground">· booked</span>
        </span>
        <span className="flex items-center gap-2">
          <span aria-hidden className="size-2.5 rounded-full bg-(--chart-2)" />
          Learning
          <span className="text-muted-foreground">· booked</span>
        </span>
      </div>
      <div className="flex min-h-0 min-w-0 flex-1 flex-col">
        <CalendarProvider className="min-h-0 flex-1 gap-3">
          <CalendarHeader />
          <CalendarSystem />
        </CalendarProvider>
      </div>
    </div>
  )
}
