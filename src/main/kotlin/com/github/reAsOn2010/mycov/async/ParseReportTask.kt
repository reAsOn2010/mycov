package com.github.reAsOn2010.mycov.async

import com.github.reAsOn2010.mycov.model.*
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
                document: Document): CompletableFuture<Void> {
        // get protected branch
        val isTarget = gitHubUtil.isCommitOnTargetBranch(owner, repo, "master", commit)
        coverageStore.store(gitType, "$owner/$repo", reportType, commit, isTarget, document)
        if (!isTarget) {
            val diff = coverageStore.diff("$owner/$repo", commit)
            val (baseBranch, pullRequestNumber) = gitHubUtil.getPullRequestBaseAndNumber(owner, repo, commit)
            gitHubUtil.commentCoverageReport(owner, repo, baseBranch, pullRequestNumber, diff)
        }
        return CompletableFuture.completedFuture(null)
    }

}