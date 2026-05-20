package com.back.devc.global.swagger

import com.back.devc.domain.admin.dashboard.controller.AdminDashboardController
import com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto
import com.back.devc.domain.interaction.report.controller.AdminReportController
import com.back.devc.domain.interaction.report.controller.AdminReportGroupController
import com.back.devc.domain.interaction.report.controller.UserReportController
import com.back.devc.domain.interaction.report.dto.AdminReportRequestDTO
import com.back.devc.domain.interaction.report.dto.ApproveReportGroupRequest
import com.back.devc.domain.interaction.report.dto.RejectReportGroupRequest
import com.back.devc.domain.interaction.report.dto.ReportGroupResponseDTO
import com.back.devc.domain.interaction.report.dto.ReportRequestDTO
import com.back.devc.domain.interaction.report.dto.ReportResponseDTO
import com.back.devc.domain.member.member.controller.AdmMemberController
import com.back.devc.domain.member.member.dto.AdmMemberDetailResponse
import com.back.devc.domain.member.member.dto.AdmMemberListResponse
import com.back.devc.domain.member.member.dto.AdmMemberStatusUpdateRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.media.Schema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class OpenApiAnnotationTest {

    @Test
    @DisplayName("관리자/신고/대시보드 컨트롤러는 Swagger 태그와 한글 API 설명을 가진다")
    fun controllersHaveKoreanOpenApiDescriptions() {
        val controllerMethods = mapOf(
            UserReportController::class.java to listOf("reportPost", "reportComment"),
            AdminReportController::class.java to listOf("getReports", "getGrouped", "approveGroup", "rejectGroup"),
            AdminReportGroupController::class.java to listOf("approveReportGroup", "rejectReportGroup"),
            AdminDashboardController::class.java to listOf("getDashboard"),
            AdmMemberController::class.java to listOf("getMembers", "getMemberDetail", "updateMemberStatus"),
        )

        controllerMethods.forEach { (controller, methodNames) ->
            assertThat(controller.getAnnotation(Tag::class.java))
                .withFailMessage("${controller.simpleName}에 @Tag가 필요합니다.")
                .isNotNull

            methodNames.forEach { methodName ->
                val operation = controller.declaredMethods
                    .first { it.name == methodName }
                    .getAnnotation(Operation::class.java)

                assertThat(operation)
                    .withFailMessage("${controller.simpleName}.$methodName 에 @Operation이 필요합니다.")
                    .isNotNull
                assertThat(operation.summary)
                    .withFailMessage("${controller.simpleName}.$methodName summary는 한글로 작성해야 합니다.")
                    .containsAnyOf("신고", "관리자", "대시보드", "회원")
            }
        }
    }

    @Test
    @DisplayName("Swagger 명세에 노출되는 DTO는 한글 Schema 설명을 가진다")
    fun dtoFieldsHaveKoreanSchemaDescriptions() {
        val dtoClasses: List<Class<*>> = listOf(
            ReportRequestDTO::class.java,
            AdminReportRequestDTO::class.java,
            ApproveReportGroupRequest::class.java,
            RejectReportGroupRequest::class.java,
            ReportResponseDTO::class.java,
            ReportGroupResponseDTO::class.java,
            DashboardResponseDto::class.java,
            DashboardResponseDto.SummaryStats::class.java,
            DashboardResponseDto.TodayReportStats::class.java,
            DashboardResponseDto.ReportCategory::class.java,
            DashboardResponseDto.ReportReasonCount::class.java,
            DashboardResponseDto.TodayActivity::class.java,
            AdmMemberListResponse::class.java,
            AdmMemberDetailResponse::class.java,
            AdmMemberStatusUpdateRequest::class.java,
        )

        dtoClasses.forEach { dtoClass ->
            assertThat(dtoClass.getAnnotation(Schema::class.java))
                .withFailMessage("${dtoClass.simpleName}에 @Schema가 필요합니다.")
                .isNotNull

            dtoClass.declaredFields
                .filterNot { it.name == "Companion" }
                .forEach { field ->
                    val schema = field.getAnnotation(Schema::class.java)

                    assertThat(schema)
                        .withFailMessage("${dtoClass.simpleName}.${field.name}에 @field:Schema가 필요합니다.")
                        .isNotNull
                    assertThat(schema.description)
                        .withFailMessage("${dtoClass.simpleName}.${field.name} description은 한글로 작성해야 합니다.")
                        .isNotBlank
                }
        }
    }
}
