import { Button } from "@workspace/ui/components/button"
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@workspace/ui/components/card"

import type { AttentionItem } from "./home-data"

export function AttentionCard({ items }: { items: AttentionItem[] }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="font-semibold">Needs your attention</CardTitle>
      </CardHeader>
      <CardContent className="gap-0">
        {items.length === 0 ? (
          <p className="text-muted-foreground">You&apos;re all caught up.</p>
        ) : (
          <ul className="divide-y divide-border">
            {items.map((item) => (
              <li
                key={item.id}
                className="flex items-center justify-between gap-3 py-3 first:pt-0 last:pb-0"
              >
                <div className="min-w-0">
                  <p className="truncate">
                    {item.type === "request" ? (
                      <>
                        <span className="font-semibold">{item.person}</span>{" "}
                        {item.title}
                      </>
                    ) : (
                      <>
                        {item.title}{" "}
                        <span className="font-semibold">{item.person}</span>
                      </>
                    )}
                  </p>
                  <p className="truncate text-xs text-muted-foreground">
                    {item.detail}
                  </p>
                </div>
                <div className="flex shrink-0 gap-2">
                  {item.type === "request" ? (
                    <>
                      <Button>Accept</Button>
                      <Button variant="outline">Decline</Button>
                    </>
                  ) : (
                    <Button variant="outline">Confirm</Button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}
