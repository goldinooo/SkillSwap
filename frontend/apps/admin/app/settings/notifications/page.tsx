import { AdminPageLayout } from "@/components/admin-page-layout"
import { AdminResourcePage } from "@/components/admin-resource-page"

export default function NotificationsPage() {
  return <AdminPageLayout><AdminResourcePage eyebrow="Settings" title="Notifications" description="Review operational alerts and delivery preferences supported by the application." columns={[{ label: "Notification", key: "notification" }, { label: "Channel", key: "channel" }, { label: "Recipients", key: "recipients" }, { label: "Status", key: "status" }, { label: "Updated", key: "updated" }]} emptyTitle="No notification settings available" emptyDescription="Notification preferences are not represented in the current application services." /></AdminPageLayout>
}
