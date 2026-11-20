"use client"

import dynamic from "next/dynamic"

const Excalidraw = dynamic(
  () => import("@excalidraw/excalidraw").then((module) => module.Excalidraw),
  {
    ssr: false,
    loading: () => (
      <div className="flex h-full min-h-96 items-center justify-center text-sm text-muted-foreground">
        Loading whiteboard...
      </div>
    ),
  },
)

export function ExcalidrawCanvas() {
  return (
    <div className="h-full min-h-96 w-full">
      <Excalidraw />
    </div>
  )
}