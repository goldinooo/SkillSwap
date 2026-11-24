import { AdminPageLayout } from "@/components/admin-page-layout"
import { AdminResourcePage } from "@/components/admin-resource-page"

export default async function MemberDetailsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params
  return <AdminPageLayout><AdminResourcePage eyebrow="Community / Members" title={`Member ${id}`} description="Profile, skills, exchange outcomes, activity, and moderation history for a single member." columns={[{ label: "Activity", key: "activity" }, { label: "Category", key: "category" }, { label: "Timestamp", key: "timestamp" }, { label: "Actor", key: "actor" }]} emptyTitle="Member details unavailable" emptyDescription="This profile cannot be loaded until the user service and permission checks are connected." /></AdminPageLayout>
}
