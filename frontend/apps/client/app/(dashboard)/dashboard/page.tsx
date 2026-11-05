import { LevelProgressCard } from "../_shared/components/level-progress-card"

export default function Page() {
  return (
    <div className="flex flex-1 flex-col gap-4">
      <div className="grid auto-rows-min gap-4 md:grid-cols-3">
        <LevelProgressCard />
        <div className="aspect-video rounded-xl bg-muted/50" />
        <div className="aspect-video rounded-xl bg-muted/50" />
      </div>
      <div className="min-h-96 flex-1 rounded-xl bg-muted/50">
        <p className="p-6 text-muted-foreground">
          Your learning dashboard will appear here.
        </p>
      </div>
    </div>
  )
}
