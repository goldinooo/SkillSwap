import { Button } from "@workspace/ui/components/button"
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@workspace/ui/components/card"

import type { UpcomingSession } from "./home-data"
import { SessionBadge } from "./session-badge"
import { Separator } from "@workspace/ui/components/separator"

export function ComingUpCard({ sessions }: { sessions: UpcomingSession[] }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="font-semibold">Coming up</CardTitle>
      </CardHeader>
      <Separator />
      <CardContent>
        {sessions.length === 0 ? (
          <p className="text-muted-foreground">No upcoming sessions.</p>
        ) : (
          <ul className="divide-y divide-border">
            {sessions.map((session) => (
              <li
                key={session.id}
                className="flex items-center gap-4 py-3 first:pt-0 last:pb-0"
              >
                <div className="w-10 shrink-0">
                  <p className="font-heading font-semibold">{session.day}</p>
                  <p className="text-[11px] text-muted-foreground">
                    {session.time}
                  </p>
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate font-medium">{session.title}</p>
                  <p className="truncate text-xs text-muted-foreground">
                    with {session.partner}
                  </p>
                </div>
                <SessionBadge kind={session.kind} points={session.points} />
                <Button variant="outline" aria-label={`Open ${session.title}`}>
                  Open
                </Button>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}
