import { AdminPageLayout } from "@/components/admin-page-layout"
import { AdminResourcePage } from "@/components/admin-resource-page"

export default function SettingsPage() {
  return <AdminPageLayout><AdminResourcePage eyebrow="Settings" title="General settings" description="Review administrative configuration that is supported by the connected application services." columns={[{ label: "Setting", key: "setting" }, { label: "Current value", key: "value" }, { label: "Scope", key: "scope" }, { label: "Updated", key: "updated" }]} emptyTitle="No configurable settings available" emptyDescription="The current repository does not expose backend configuration capabilities, so settings remain read-only until those services are defined." /></AdminPageLayout>
}
