import { AdminPageLayout } from "@/components/admin-page-layout"
import { AdminResourcePage } from "@/components/admin-resource-page"

export default function ActivityPage() {
  return <AdminPageLayout><AdminResourcePage eyebrow="Operations" title="Live activity" description="Follow registrations, exchange events, reviews, and administrative actions as they happen." columns={[{ label: "Event", key: "event" }, { label: "Actor", key: "actor" }, { label: "Target", key: "target" }, { label: "Occurred", key: "occurred" }, { label: "Source", key: "source" }]} filters={[{ label: "Event type", value: "all", options: [{ label: "All events", value: "all" }, { label: "Registration", value: "registration" }, { label: "Exchange", value: "exchange" }, { label: "Review", value: "review" }, { label: "Moderation", value: "moderation" }] }]} emptyTitle="No live activity available" emptyDescription="The activity feed will use real-time infrastructure or polling once event sources are available." /></AdminPageLayout>
}
