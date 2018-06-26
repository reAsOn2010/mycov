package com.github.reAsOn2010.mycov.store

import com.github.reAsOn2010.mycov.controller.ReportController.*
import org.dom4j.Document
import org.springframework.stereotype.Component

@Component
class CoverageStore {

    private val maps: Map<String, CoverageEntry> = emptyMap()

    class CoverageEntry(
        private val gitType: GitType,
        private val reportType: ReportType
    ) {
        private val targetRecord: CoverageRecord = CoverageRecord()
        private val diffs: Map<String, CoverageRecord> = emptyMap()
    }

    class CoverageRecord {
        private val files: Map<String, List<CoverageTuple>> = emptyMap()
    }

    data class CoverageTuple (
        val lineNumber: Int,
        val missedInstructions: Int,
        val coveredInstructions: Int,
        val missedBranches: Int,
        val coveredBranches: Int
    )

    fun store(gitType: GitType, repoName: String, reportType: ReportType, commit: String, isTarget: Boolean, report: Document) {

    }

}