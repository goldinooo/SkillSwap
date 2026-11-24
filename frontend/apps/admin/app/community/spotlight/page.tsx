import { AdminPageLayout } from "@/components/admin-page-layout"
import { AdminResourcePage } from "@/components/admin-resource-page"

export default function SpotlightPage() {
  return <AdminPageLayout><AdminResourcePage eyebrow="Community" title="Community spotlight" description="Review featured members, trusted contributors, and community stories selected for visibility." actionLabel="Create spotlight" columns={[{ label: "Member", key: "member" }, { label: "Story", key: "story" }, { label: "Status", key: "status" }, { label: "Published", key: "published" }]} emptyTitle="No spotlight records available" emptyDescription="Spotlight content is not available until the community content service is connected." /></AdminPageLayout>
}
