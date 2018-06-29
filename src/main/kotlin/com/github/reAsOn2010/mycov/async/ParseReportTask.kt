package com.github.reAsOn2010.mycov.async

import com.github.reAsOn2010.mycov.controller.ReportController.*
import com.github.reAsOn2010.mycov.store.CoverageStore
import com.github.reAsOn2010.mycov.util.GitHubUtil
import org.dom4j.Document
import org.springframework.scheduling.annotation.Async
import java.util.concurrent.CompletableFuture

class ParseReportTask(private val coverageStore: CoverageStore,
                      private val gitHubUtil: GitHubUtil) {

    @Async
    fun execute(gitType: GitType,
                owner: String,
                repo: String,
                commit: String,
                reportType: ReportType,
                document: Document,
                branch: String?,
                pullRequestNumber: Int?): CompletableFuture<Void> {
        // get protected branch
        val isTarget = gitHubUtil.isCommitOnTargetBranch(owner, repo, "master", commit)
        coverageStore.store(gitType, "$owner/$repo", reportType, commit, isTarget, document)
        if (!isTarget) {
            val diff = coverageStore.diff("$owner/$repo", commit)
        }
        return CompletableFuture.completedFuture(null)
    }

}