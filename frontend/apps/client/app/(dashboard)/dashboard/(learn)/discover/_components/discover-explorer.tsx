"use client"

import { useMemo, useState } from "react"
import { Button } from "@workspace/ui/components/button"
import { Input } from "@workspace/ui/components/input"
import { cn } from "@workspace/ui/lib/utils"

import {
  CATEGORIES,
  type Category,
  MOCK_AVAILABLE_POINTS,
  type TeacherSkill,
} from "./discover-data"
import { TeacherCard } from "./teacher-card"

const AFFORDABLE = "I can afford" as const

export function DiscoverExplorer({ teachers }: { teachers: TeacherSkill[] }) {
  const [query, setQuery] = useState("")
  const [filter, setFilter] = useState<Category | "All" | typeof AFFORDABLE>(
    "All"
  )

  const results = useMemo(() => {
    const q = query.trim().toLowerCase()
    return teachers.filter((teacher) => {
      if (filter === AFFORDABLE && teacher.points > MOCK_AVAILABLE_POINTS) {
        return false
      }
      if (
        filter !== "All" &&
        filter !== AFFORDABLE &&
        teacher.category !== filter
      ) {
        return false
      }
      if (!q) return true
      return [teacher.title, teacher.name, teacher.category, teacher.description]
        .join(" ")
        .toLowerCase()
        .includes(q)
    })
  }, [query, filter, teachers])

  return (
    <>
      <form
        role="search"
        className="flex gap-2"
        onSubmit={(event) => event.preventDefault()}
      >
        <Input
          type="search"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search skills, e.g. TypeScript, Docker, Figma"
          aria-label="Search skills"
          className="h-11 flex-1 md:text-sm"
        />
        <Button type="submit" size="lg" className="h-11 px-5">
          Search
        </Button>
      </form>

      <div className="flex flex-wrap gap-2">
        {[...CATEGORIES, AFFORDABLE].map((category) => {
          const active = filter === category
          return (
            <Button
              key={category}
              variant={active ? "default" : "outline"}
              className={cn("h-9 rounded-full px-4", !active && "bg-transparent")}
              aria-pressed={active}
              onClick={() => setFilter(category)}
            >
              {category}
            </Button>
          )
        })}
      </div>

      <section className="flex flex-col gap-4">
        <h2 className="font-heading text-lg font-semibold">Popular this week</h2>
        {results.length === 0 ? (
          <p className="rounded-xl border border-dashed p-6 text-muted-foreground">
            No teachers match your search yet.
          </p>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {results.map((teacher) => (
              <TeacherCard key={teacher.id} teacher={teacher} />
            ))}
          </div>
        )}
      </section>
    </>
  )
}
