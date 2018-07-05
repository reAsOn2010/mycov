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
        coverageStore.store(gitType, "$owner/$repo", reportType, commit, document)
        try {
            val (base, pullRequestNumber) = gitHubUtil.getPullRequestBaseAndNumber(owner, repo, commit)
            val diff = coverageStore.diff("$owner/$repo", base, commit)
            gitHubUtil.commentCoverageReport(owner, repo, base, pullRequestNumber, diff)
        } catch (e: PullRequestNotFound) {
            println("Commit is not associate with a pr, skip diff and comment")
        }
        return CompletableFuture.completedFuture(null)
    }

}