import {
  EventCalendarNavNext,
  EventCalendarNavPrev,
  EventCalendarNavToday,
  EventCalendarTitle,
  EventCalendarViewSwitcher,
} from "@workspace/ui/components/reui/event-calendar/event-calendar-nav"

import {
  ToggleGroup,
  ToggleGroupItem,
} from "@workspace/ui/components/toggle-group"

import {
  ButtonGroup,
  ButtonGroupSeparator,
} from "@workspace/ui/components/button-group"
import { Button } from "@workspace/ui/components/button"
import { Separator } from "@workspace/ui/components/separator"

export default function CalendarHeader() {
  return (
    <div className="flex w-full shrink-0 flex-wrap items-center justify-between gap-3 rounded-md border border-border bg-card p-3">
      <div className="flex min-w-0 items-center gap-4">
        <div className="flex min-w-0 items-center gap-4">
          <EventCalendarTitle className="text-lg font-bold" />
          <Separator orientation="vertical" />

          <ButtonGroup>
            <Button variant="outline" render={<EventCalendarNavPrev />} />
            <ButtonGroupSeparator />
            <Button variant="outline" render={<EventCalendarNavToday />} />
            <ButtonGroupSeparator />
            <Button variant="outline" render={<EventCalendarNavNext />} />
          </ButtonGroup>
        </div>
      </div>
      <EventCalendarViewSwitcher />
    </div>
  )
}
