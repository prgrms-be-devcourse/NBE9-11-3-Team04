import { apiFetch } from "@/lib/api"

export interface DashboardResponse {
    stats: any[]
    recentReports: any[]
    todayActivity: any
}

export function getDashboard() {
    return apiFetch<DashboardResponse>("/admin/dashboard")
}