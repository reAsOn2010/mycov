package com.github.reAsOn2010.mycov.util

import com.github.reAsOn2010.mycov.model.CoverageDiffReport
import com.github.reAsOn2010.mycov.model.CoverageOverview
import com.github.reAsOn2010.mycov.model.ReportType

interface GitServiceInterface {

    fun getPullRequestBaseAndNumber(owner: String, repo: String, head: String): Pair<String, Int>
    fun getPullRequestBaseAndHead(owner: String, repo: String, pullRequestNumber: Int): Pair<String, String>
    fun getDiffOfCommit(owner: String, repo: String, pullRequestNumber: Int): Pair<List<String>, String>
    fun commentCoverageReport(owner: String, repo: String, reportType: ReportType, base: String,
                              pullRequestNumber: Int, report: CoverageDiffReport)
    fun statusCoverageReport(owner: String, repo: String, head: String, overview: CoverageOverview)

}