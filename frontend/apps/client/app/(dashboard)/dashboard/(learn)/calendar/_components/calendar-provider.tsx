"use client"

import { useState } from "react"
import { Button } from "@workspace/ui/components/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@workspace/ui/components/dialog"
import { Field, FieldGroup, FieldLabel } from "@workspace/ui/components/field"
import { Input } from "@workspace/ui/components/input"
import { cn } from "@/lib/utils"
import { EventCalendar } from "@workspace/ui/components/reui/event-calendar/event-calendar"
import type {
  CalendarEvent,
  EventCalendarSlotDraft,
  EventCalendarSlotInfo,
} from "@workspace/ui/components/reui/event-calendar/event-calendar-types"
import React from "react"

type CalendarSlotType = "availability" | "teaching" | "learning"
type CalendarSlotData = { type: CalendarSlotType; partner?: string }

const SLOT_COLORS: Record<CalendarSlotType, string> = {
  availability: "var(--chart-3)",
  teaching: "var(--chart-1)",
  learning: "var(--chart-2)",
}

function dateInCurrentWeek(dayOffset: number, hour: number, minute = 0) {
  const date = new Date()
  const weekday = (date.getDay() + 6) % 7
  date.setDate(date.getDate() - weekday + dayOffset)
  date.setHours(hour, minute, 0, 0)
  return date
}

function createMockEvents(): CalendarEvent<CalendarSlotData>[] {
  return [
    {
      id: "availability-tuesday",
      title: "Open for mentoring",
      start: dateInCurrentWeek(1, 9),
      end: dateInCurrentWeek(1, 12),
      color: SLOT_COLORS.availability,
      data: { type: "availability" },
    },
    {
      id: "teaching-react",
      title: "React mentoring · Alex",
      start: dateInCurrentWeek(1, 13),
      end: dateInCurrentWeek(1, 14),
      color: SLOT_COLORS.teaching,
      readOnly: true,
      draggable: false,
      resizable: false,
      data: { type: "teaching", partner: "Alex" },
    },
    {
      id: "learning-typescript",
      title: "TypeScript · with Sam",
      start: dateInCurrentWeek(2, 10, 30),
      end: dateInCurrentWeek(2, 11, 30),
      color: SLOT_COLORS.learning,
      readOnly: true,
      draggable: false,
      resizable: false,
      data: { type: "learning", partner: "Sam" },
    },
    {
      id: "availability-thursday",
      title: "Available to learn",
      start: dateInCurrentWeek(3, 15),
      end: dateInCurrentWeek(3, 17),
      color: SLOT_COLORS.availability,
      data: { type: "availability" },
    },
    {
      id: "teaching-node",
      title: "Node.js study group",
      start: dateInCurrentWeek(4, 11),
      end: dateInCurrentWeek(4, 12, 30),
      color: SLOT_COLORS.teaching,
      readOnly: true,
      draggable: false,
      resizable: false,
      data: { type: "teaching", partner: "Study group" },
    },
  ]
}

function getSlotType(event: CalendarEvent<CalendarSlotData>) {
  return event.data?.type ?? "availability"
}

function toDateTimeInput(date: Date) {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
  return localDate.toISOString().slice(0, 16)
}

function getSlotStart(slot: EventCalendarSlotInfo | EventCalendarSlotDraft) {
  const start = new Date("date" in slot ? slot.date : slot.start)
  if (slot.allDay) start.setHours(9, 0, 0, 0)
  return start
}

export default function CalendarProvider({
  children,
  className,
}: {
  children: React.ReactNode
  className?: string
}) {
  const [events, setEvents] = useState(createMockEvents)
  const [editingEvent, setEditingEvent] =
    useState<CalendarEvent<CalendarSlotData> | null>(null)
  const [editorOpen, setEditorOpen] = useState(false)
  const [title, setTitle] = useState("Open for mentoring")
  const [start, setStart] = useState("")
  const [end, setEnd] = useState("")

  const openEditor = (
    event: CalendarEvent<CalendarSlotData> | null,
    slot?: EventCalendarSlotInfo | EventCalendarSlotDraft
  ) => {
    const slotStart = slot ? getSlotStart(slot) : new Date()
    const nextStart = event?.start ?? slotStart
    const nextEnd = event?.end ?? new Date(nextStart.getTime() + 60 * 60 * 1000)
    setEditingEvent(event)
    setTitle(event?.title ?? "Open for mentoring")
    setStart(toDateTimeInput(nextStart))
    setEnd(toDateTimeInput(nextEnd))
    setEditorOpen(true)
  }

  const closeEditor = () => {
    setEditorOpen(false)
    setEditingEvent(null)
  }

  const saveAvailability = () => {
    const startDate = new Date(start)
    const endDate = new Date(end)
    if (
      !title.trim() ||
      Number.isNaN(startDate.getTime()) ||
      endDate <= startDate
    )
      return

    if (editingEvent) {
      setEvents((current) =>
        current.map((event) =>
          event.id === editingEvent.id
            ? { ...event, title: title.trim(), start: startDate, end: endDate }
            : event
        )
      )
    } else {
      setEvents((current) => [
        ...current,
        {
          id: `availability-${crypto.randomUUID()}`,
          title: title.trim(),
          start: startDate,
          end: endDate,
          color: SLOT_COLORS.availability,
          data: { type: "availability" },
        },
      ])
    }
    closeEditor()
  }

  const deleteAvailability = () => {
    if (!editingEvent || getSlotType(editingEvent) !== "availability") return
    setEvents((current) =>
      current.filter((event) => event.id !== editingEvent.id)
    )
    closeEditor()
  }

  return (
    <EventCalendar<CalendarSlotData>
      className={cn("flex h-full min-h-0 flex-1 flex-col gap-3", className)}
      events={events}
      onEventsChange={(nextEvents) => setEvents(nextEvents)}
      onEventUpdate={({ event }) =>
        getSlotType(event) === "availability" ? true : false
      }
      onEventClick={(occurrence, e) => {
        if (getSlotType(occurrence.event) !== "availability") return
        e.preventDefault()
        openEditor(occurrence.event)
      }}
      onSlotClick={(slot) => openEditor(null, slot)}
      onSelectSlot={(slot) => openEditor(null, slot)}
      defaultDate={new Date()}
      defaultView="week"
      views={["month", "week", "day", "agenda"]}
      defaultViewSettings={{ nowIndicator: true, weekends: true }}
      dayStartHour={7}
      dayEndHour={21}
      slotDuration={30}
      snapDuration={15}
      weekStartsOn={1}
    >
      {children}
      <Dialog
        open={editorOpen}
        onOpenChange={(open) => {
          if (!open) closeEditor()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {editingEvent ? "Edit availability" : "Add availability"}
            </DialogTitle>
            <DialogDescription>
              Set a time when others can book a session with you.
            </DialogDescription>
          </DialogHeader>
          <FieldGroup>
            <Field>
              <FieldLabel htmlFor="availability-title">Title</FieldLabel>
              <Input
                id="availability-title"
                value={title}
                onChange={(event) => setTitle(event.target.value)}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="availability-start">Starts</FieldLabel>
              <Input
                id="availability-start"
                type="datetime-local"
                value={start}
                onChange={(event) => setStart(event.target.value)}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="availability-end">Ends</FieldLabel>
              <Input
                id="availability-end"
                type="datetime-local"
                value={end}
                onChange={(event) => setEnd(event.target.value)}
              />
            </Field>
          </FieldGroup>
          <DialogFooter className="sm:justify-between">
            {editingEvent && (
              <Button variant="destructive" onClick={deleteAvailability}>
                Delete availability
              </Button>
            )}
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={closeEditor}>
                Cancel
              </Button>
              <Button
                onClick={saveAvailability}
                disabled={
                  !title.trim() ||
                  !start ||
                  !end ||
                  new Date(end) <= new Date(start)
                }
              >
                Save availability
              </Button>
            </div>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </EventCalendar>
  )
}
