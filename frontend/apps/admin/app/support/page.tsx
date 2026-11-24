import { AdminPageLayout } from "@/components/admin-page-layout"
import { AdminResourcePage } from "@/components/admin-resource-page"

export default function SupportPage() {
  return <AdminPageLayout><AdminResourcePage eyebrow="Operations" title="Support inbox" description="Coordinate member support requests and route issues to the right operational team." columns={[{ label: "Ticket", key: "ticket" }, { label: "Member", key: "member" }, { label: "Topic", key: "topic" }, { label: "Priority", key: "priority" }, { label: "Status", key: "status" }, { label: "Updated", key: "updated" }]} filters={[{ label: "Ticket status", value: "all", options: [{ label: "All tickets", value: "all" }, { label: "Open", value: "open" }, { label: "Pending", value: "pending" }, { label: "Resolved", value: "resolved" }] }]} emptyTitle="No support tickets available" emptyDescription="A support inbox is not represented in the current backend, so no ticket actions are enabled." /></AdminPageLayout>
}
