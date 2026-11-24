import { AdminPageLayout } from "@/components/admin-page-layout"
import { AdminResourcePage } from "@/components/admin-resource-page"

export default function AuditLogsPage() {
  return <AdminPageLayout><AdminResourcePage eyebrow="Security" title="Audit logs" description="Review sensitive administrative actions with actor, target, timestamp, and relevant metadata." columns={[{ label: "Administrator", key: "administrator" }, { label: "Action", key: "action" }, { label: "Target", key: "target" }, { label: "Timestamp", key: "timestamp" }, { label: "Metadata", key: "metadata" }]} filters={[{ label: "Action type", value: "all", options: [{ label: "All actions", value: "all" }, { label: "User changes", value: "users" }, { label: "Moderation", value: "moderation" }, { label: "Exchange intervention", value: "exchanges" }, { label: "Session revocation", value: "sessions" }] }]} emptyTitle="No audit records available" emptyDescription="Audit events must be emitted by protected backend mutations before they can be displayed here." /></AdminPageLayout>
}
