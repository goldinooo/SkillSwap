import { AdminPageLayout } from "@/components/admin-page-layout"
import { AdminResourcePage } from "@/components/admin-resource-page"

export default function CategoriesPage() {
  return <AdminPageLayout><AdminResourcePage eyebrow="Marketplace" title="Categories" description="Organize the skill ecosystem and monitor which categories are active, growing, or underserved." actionLabel="Create category" columns={[{ label: "Category", key: "category" }, { label: "Published skills", key: "skills" }, { label: "Demand", key: "demand" }, { label: "Status", key: "status" }, { label: "Updated", key: "updated" }]} emptyTitle="No categories available" emptyDescription="Category data will appear when the marketplace taxonomy endpoint is connected." /></AdminPageLayout>
}
