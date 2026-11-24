import { AdminPageLayout } from "@/components/admin-page-layout"
import { AdminResourcePage } from "@/components/admin-resource-page"

export default function StaffPage() {
  return <AdminPageLayout><AdminResourcePage eyebrow="Settings / Access" title="Staff access" description="Manage administrator roles and granular permissions once server-side authorization is available." actionLabel="Invite staff" columns={[{ label: "Administrator", key: "administrator" }, { label: "Role", key: "role" }, { label: "Permissions", key: "permissions" }, { label: "Last login", key: "login" }, { label: "Status", key: "status" }]} emptyTitle="No staff access records available" emptyDescription="Role and permission management requires a protected authentication and authorization service." /></AdminPageLayout>
}
