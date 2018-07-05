package com.github.reAsOn2010.mycov.model

class CoverageRepo(
    val repoName: String,
    val gitType: GitType,
    val reportType: ReportType
) {
    val commits: MutableMap<String, CoverageCommit> = mutableMapOf()
}