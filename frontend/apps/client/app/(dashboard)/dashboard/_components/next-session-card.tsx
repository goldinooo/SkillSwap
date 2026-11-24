"use client"

import { EmptyIcon } from "@phosphor-icons/react"
import { Button } from "@workspace/ui/components/button"
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@workspace/ui/components/card"
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@workspace/ui/components/empty"
import type { NextSession } from "./home-data"
import { SessionBadge, XpBadge } from "./session-badge"

export function NextSessionCard({ session }: { session?: NextSession }) {
  if (session === undefined) {
    return (
      <Empty className="border border-dashed bg-card/50">
        <EmptyHeader>
          <EmptyMedia variant="icon">
            <EmptyIcon />
          </EmptyMedia>
          <EmptyTitle>No upcomming sessions</EmptyTitle>
          <EmptyDescription>Want to learn or teach?</EmptyDescription>
        </EmptyHeader>
        <EmptyContent>
          <Button>Swap Skills</Button>
        </EmptyContent>
      </Empty>
    )
  }
  return (
    <Card className="bg-primary/6 ring-primary/35">
      <CardHeader>
        <p className="font-medium text-primary uppercase">
          Next session · Today
        </p>
        <CardTitle className="text-xl font-semibold">{session.title}</CardTitle>
      </CardHeader>
      <CardContent className="gap-2">
        <p className="text-muted-foreground">
          {session.startTime} to {session.endTime} · with {session.partner.name}{" "}
          · {session.medium}
        </p>
        <div className="flex flex-wrap gap-1.5">
          <SessionBadge kind={session.kind} points={session.points} showUnit />
          <XpBadge xp={session.xp} />
        </div>
      </CardContent>
      <CardFooter className="flex-wrap gap-2">
        <Button>Open session</Button>
        <Button variant="outline">Message {session.partner.firstName}</Button>
        <Button variant="outline">Reschedule</Button>
      </CardFooter>
    </Card>
  )
}
