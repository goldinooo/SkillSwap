import { AdminPageLayout } from "@/components/admin-page-layout"
import { AdminResourcePage } from "@/components/admin-resource-page"

export default function ReviewPage() {
  return <AdminPageLayout><AdminResourcePage eyebrow="Marketplace / Trust & Safety" title="Review queue" description="Prioritize skill listings and community reports that need an administrator decision." columns={[{ label: "Item", key: "item" }, { label: "Type", key: "type" }, { label: "Priority", key: "priority" }, { label: "Status", key: "status" }, { label: "Reviewer", key: "reviewer" }, { label: "SLA", key: "sla" }]} filters={[{ label: "Queue status", value: "all", options: [{ label: "All items", value: "all" }, { label: "Pending", value: "pending" }, { label: "In progress", value: "progress" }, { label: "Resolved", value: "resolved" }] }]} emptyTitle="Review queue is empty" emptyDescription="Review items will appear when listings and moderation reports are available from the backend." /></AdminPageLayout>
}
