import {
  CalendarBlankIcon,
  ClockIcon,
  VideoCameraIcon,
} from "@phosphor-icons/react/ssr"
import { Avatar, AvatarFallback } from "@workspace/ui/components/avatar"
import { Badge } from "@workspace/ui/components/badge"
import { Button } from "@workspace/ui/components/button"
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@workspace/ui/components/card"

import type { TeacherSkill } from "./discover-data"

export function TeacherCard({ teacher }: { teacher: TeacherSkill }) {
  return (
    <Card>
      <CardHeader className="grid-cols-[auto_1fr_auto] items-center gap-3">
        <Avatar size="lg">
          <AvatarFallback className="bg-primary/20 text-[11px] font-semibold text-(--chart-3)">
            {teacher.initials}
          </AvatarFallback>
        </Avatar>
        <div className="col-start-2 grid">
          <span className="font-medium">{teacher.name}</span>
          <span className="text-xs text-muted-foreground">
            Level {teacher.level}
          </span>
        </div>
        <Badge className="col-start-3 bg-primary/20 px-2.5 text-(--chart-3)">
          {teacher.category}
        </Badge>
        <CardTitle className="col-span-3 mt-2 text-base font-semibold">
          {teacher.title}
        </CardTitle>
      </CardHeader>

      <CardContent className="gap-4">
        <p className="text-sm text-muted-foreground">{teacher.description}</p>
        <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground">
          <span className="inline-flex items-center gap-1.5">
            <ClockIcon className="size-3.5" />
            {teacher.duration}
          </span>
          <span className="inline-flex items-center gap-1.5">
            <VideoCameraIcon className="size-3.5" />
            {teacher.medium}
          </span>
          <span className="inline-flex items-center gap-1.5">
            <CalendarBlankIcon className="size-3.5" />
            {teacher.schedule}
          </span>
        </div>
      </CardContent>

      <CardFooter className="gap-2 border-t border-border pt-(--card-spacing)">
        <span className="font-heading font-semibold">{teacher.points} pts</span>
        <Badge className="h-6 bg-accent px-2.5 text-accent-foreground">
          +{teacher.xp} XP
        </Badge>
        <Button className="ml-auto" aria-label={`Request ${teacher.title}`}>
          Request
        </Button>
      </CardFooter>
    </Card>
  )
}
