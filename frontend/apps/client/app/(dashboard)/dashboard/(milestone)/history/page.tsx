export default function HistoryPage() {
  return (
    <div className="flex flex-1 flex-col gap-4">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight">History</h1>
        <p className="text-muted-foreground">
          View your past swapped skills.
        </p>
      </div>
      <div className="min-h-96 flex-1 rounded-xl border border-dashed p-6 text-muted-foreground">
        History content will appear here.
      </div>
    </div>
  )
}
