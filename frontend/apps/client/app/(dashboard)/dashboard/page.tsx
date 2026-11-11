import { AttentionCard } from "./_components/attention-card"
import { ComingUpCard } from "./_components/coming-up-card"
import {
  MOCK_ATTENTION,
  MOCK_NEXT_SESSION,
  MOCK_RECOMMENDATIONS,
  MOCK_STATS,
  MOCK_UPCOMING,
  MOCK_USER,
} from "./_components/home-data"
import { NextSessionCard } from "./_components/next-session-card"
import { RecommendedCard } from "./_components/recommended-card"
import { StatCards } from "./_components/stat-cards"

function summary(sessionsToday: number, requests: number) {
  const s = sessionsToday === 1 ? "one session" : `${sessionsToday} sessions`
  const r = requests === 1 ? "one request" : `${requests} requests`
  return `${s.charAt(0).toUpperCase()}${s.slice(1)} today and ${r} waiting for you.`
}

export default function Page() {
  const pendingRequests = MOCK_ATTENTION.filter(
    (item) => item.type === "request"
  ).length

  return (
    <div className="flex flex-1 flex-col gap-4">
      <div className="flex flex-col gap-1">
        <h1 className="text-3xl font-bold tracking-tight">
          Welcome back, {MOCK_USER.firstName}
        </h1>
        <p className="text-muted-foreground">{summary(1, pendingRequests)}</p>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <NextSessionCard session={MOCK_NEXT_SESSION} />
        {/* <NextSessionCard /> */}
        <AttentionCard items={MOCK_ATTENTION} />
      </div>

      <StatCards stats={MOCK_STATS} />

      <div className="grid items-start gap-4 lg:grid-cols-2">
        <ComingUpCard sessions={MOCK_UPCOMING} />
        <RecommendedCard recommendations={MOCK_RECOMMENDATIONS} />
      </div>
    </div>
  )
}
