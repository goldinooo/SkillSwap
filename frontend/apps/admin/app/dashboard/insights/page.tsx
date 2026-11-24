import { AdminPageLayout } from "@/components/admin-page-layout"
import { AdminResourcePage } from "@/components/admin-resource-page"

export default function InsightsPage() {
  return <>
    <AdminPageLayout>
      <AdminResourcePage 
        eyebrow="Overview / Analytics"
        title="Insights" 
        description="Understand growth, retention, engagement, and exchange outcomes across the community." 
        stats={[
          { label: "DAU / WAU / MAU", detail: "Requires activity events" }, 
          { label: "New versus returning", detail: "Requires user events" }, 
          { label: "Retention", detail: "Requires cohort data" }, 
          { label: "Average session", detail: "Requires session tracking" }
          ]} 
        columns={[
          { label: "Metric", key: "metric" }, 
          { label: "Current period", key: "current" }, 
          { label: "Previous period", key: "previous" }, 
          { label: "Change", key: "change" }, 
          { label: "Source", key: "source" }
          ]} 
        filters={[
          { 
            label: "Date range", value: "30d", options: [
              { label: "Last 24 hours", value: "24h" }, 
              { label: "Last 7 days", value: "7d" }, 
              { label: "Last 30 days", value: "30d" }, 
              { label: "Last 90 days", value: "90d" }, 
              { label: "Custom range", value: "custom" }
              ]
          }
          ]} 
        emptyTitle="No analytics events available" 
        emptyDescription="Charts and retention cohorts will populate after event tracking and an analytics endpoint are implemented." 
        />
    </AdminPageLayout>
  </>
}
  
