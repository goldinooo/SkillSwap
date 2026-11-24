import { Avatar, AvatarFallback } from "@workspace/ui/components/avatar"
import { Button } from "@workspace/ui/components/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@workspace/ui/components/card"

import type { Recommendation } from "./home-data"

export function RecommendedCard({
  recommendations,
}: {
  recommendations: Recommendation[]
}) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="font-semibold">Recommended for you</CardTitle>
        <CardDescription className="text-xs">
          Based on what you are learning. You can afford both.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <ul className="divide-y divide-border border-t border-border">
          {recommendations.map((rec) => (
            <li key={rec.id} className="flex items-center gap-3 py-3 last:pb-0">
              <Avatar size="lg">
                <AvatarFallback className="bg-primary/20 text-[11px] font-semibold text-(--chart-3)">
                  {rec.initials}
                </AvatarFallback>
              </Avatar>
              <div className="min-w-0 flex-1">
                <p className="truncate font-medium">{rec.title}</p>
                <p className="truncate text-xs text-muted-foreground">
                  with {rec.teacher} · {rec.cost} pts
                </p>
              </div>
              <Button variant="outline" aria-label={`Request ${rec.title}`}>
                Request
              </Button>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}
