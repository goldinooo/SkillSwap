import { ExcalidrawCanvas } from "@/components/whiteboard/excalidraw-canvas"

export default function WhiteboardPage() {
  return (
    <main className="flex min-h-svh flex-col bg-background">
      <header className="flex h-16 shrink-0 items-center justify-between gap-3 border-b px-4 md:px-8">
        <div className="min-w-0">
          <p className="hidden font-sans text-xs text-muted-foreground sm:block">SkillSwap / Playground</p>
          <h1 className="whitespace-nowrap font-sans text-xs font-semibold sm:text-sm">Whiteboard sandbox</h1>
        </div>
        <span className="shrink-0 whitespace-nowrap rounded border px-2 py-1 font-sans text-xs text-muted-foreground">
          Local only
        </span>
      </header>
      <section className="min-h-0 flex-1 p-3 md:p-5">
        <div className="h-[calc(100svh-5.5rem)] min-h-96 overflow-hidden rounded border bg-white">
          <ExcalidrawCanvas />
        </div>
      </section>
    </main>
  )
}