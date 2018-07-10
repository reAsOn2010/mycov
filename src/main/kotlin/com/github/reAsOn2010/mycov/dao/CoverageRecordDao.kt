package com.github.reAsOn2010.mycov.dao

import com.github.reAsOn2010.mycov.model.*
import org.springframework.data.jpa.repository.JpaRepository

interface CoverageRecordDao : JpaRepository<CoverageRecord, Long> {
    fun findByRepoNameAndHashAndGitTypeAndReportType(
        repoName: String, hash: String, gitType: GitType, reportType: ReportType
    ): CoverageRecord?

    fun findByRepoName(repoName: String): List<CoverageRecord>
}