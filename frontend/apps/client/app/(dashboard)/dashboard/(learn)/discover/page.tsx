import { MOCK_AVAILABLE_POINTS, MOCK_TEACHERS } from "./_components/discover-data"
import { DiscoverExplorer } from "./_components/discover-explorer"

export default function DiscoverPage() {
  return (
    <div className="flex flex-1 flex-col gap-4">
      <div className="flex flex-col gap-1">
        <h1 className="text-3xl font-bold tracking-tight">Discover</h1>
        <p className="text-muted-foreground">
          Find someone to learn from. You have {MOCK_AVAILABLE_POINTS} points.
        </p>
      </div>
      <DiscoverExplorer teachers={MOCK_TEACHERS} />
    </div>
  )
}
