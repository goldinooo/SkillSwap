import { AdminPageLayout } from "@/components/admin-page-layout"
import { AdminResourcePage } from "@/components/admin-resource-page"

export default function ListingsPage() {
  return <AdminPageLayout><AdminResourcePage eyebrow="Marketplace" title="Skill listings" description="Manage the skills members offer and want to learn, including category, visibility, and review status." actionLabel="Create skill" columns={[{ label: "Skill", key: "skill" }, { label: "Category", key: "category" }, { label: "Offers", key: "offers" }, { label: "Wants", key: "wants" }, { label: "Status", key: "status" }]} filters={[{ label: "Listing status", value: "all", options: [{ label: "All listings", value: "all" }, { label: "Published", value: "published" }, { label: "Archived", value: "archived" }, { label: "Needs review", value: "review" }] }]} emptyTitle="No skill listings available" emptyDescription="Skill supply and demand records will appear when the skills service is connected." /></AdminPageLayout>
}
