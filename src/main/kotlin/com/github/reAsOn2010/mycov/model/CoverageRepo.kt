package com.github.reAsOn2010.mycov.model

class CoverageRepo(
    val repoName: String,
    val gitType: GitType,
    val reportType: ReportType
) {
    var targetRecord: CoverageCommit? = null
    val diffs: MutableMap<String, CoverageCommit> = mutableMapOf()
}