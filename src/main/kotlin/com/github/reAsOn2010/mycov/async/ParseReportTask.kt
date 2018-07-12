package com.github.reAsOn2010.mycov.async

import com.github.reAsOn2010.mycov.model.*
import com.github.reAsOn2010.mycov.store.CoverageStore
import com.github.reAsOn2010.mycov.util.GitHubUtil
import org.dom4j.Document
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class ParseReportTask(private val coverageStore: CoverageStore,
                      private val gitHubUtil: GitHubUtil) {

    @Async
    fun execute(gitType: GitType,
                owner: String,
                repo: String,
                commit: String,
                reportType: ReportType,
                document: Document): CompletableFuture<Void> {
        coverageStore.store("$owner/$repo", gitType, reportType, commit, document)
        val record = coverageStore.get("$owner/$repo", gitType, reportType, commit)
        gitHubUtil.statusCoverageReport(owner, repo, commit, record.detail.commitOverview)
        try {
            val (base, pullRequestNumber) = gitHubUtil.getPullRequestBaseAndNumber(owner, repo, commit)
            val diff = coverageStore.diff("$owner/$repo", gitType, reportType, base, commit)
            gitHubUtil.commentCoverageReport(owner, repo, gitType, reportType, base, pullRequestNumber, diff)
        } catch (e: PullRequestNotFound) {
            println("Commit is not associate with a pr, skip diff and comment")
        } catch (e: CoverageOfCommitNotFound) {
            println("Base coverage report is not found, skip diff and comment")
        }
        return CompletableFuture.completedFuture(null)
    }

}