package com.github.reAsOn2010.mycov.util

import com.github.reAsOn2010.mycov.config.*
import com.github.reAsOn2010.mycov.model.*
import okhttp3.*
import org.json.*
import org.springframework.stereotype.Component

@Component
class GitHubUtil {
    companion object {
        val client = OkHttpClient()
        val authenticatedBuilder get() = Request.Builder()
            .addHeader("Authorization", "token ${TokenConfig.github.token}")
        val prefix = "MyCov coverage report"
        val JSON = MediaType.parse("application/json; charset=utf-8")
    }

    fun getPullRequestBaseAndNumber(owner: String, repo: String, head: String): Pair<String, Int> {
        val request = authenticatedBuilder
            .url("https://api.github.com/repos/$owner/$repo/pulls?sort=updated&direction=desc").build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw GithubAPICallError(request.url().toString())
        }
        val body = response.body()!!.string()
        val json = JSONArray(body).map { it as JSONObject }
        val targetPullRequest = json.firstOrNull { it.getJSONObject("head").getString("sha").startsWith(head) }
        if (targetPullRequest == null) {
            throw PullRequestNotFound(head)
        } else {
            val pullRequestNumber = targetPullRequest.getInt("number")
            val baseCommit = targetPullRequest.getJSONObject("base").getString("sha").substring(0, 7)
            return baseCommit to pullRequestNumber
        }
    }

    fun getPullRequestBaseAndHead(owner: String, repo: String, pullRequestNumber: Int): Pair<String, String> {
        val request = authenticatedBuilder
            .url("https://api.github.com/repos/$owner/$repo/pulls/$pullRequestNumber").build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw GithubAPICallError(request.url().toString())
        }
        val body = response.body()!!.string()
        val json = JSONObject(body)
        val base = json.getJSONObject("base").getString("sha").substring(0, 7)
        val head = json.getJSONObject("head").getString("sha").substring(0, 7)
        return base to head
    }

    fun getDiffOfCommit(owner: String, repo: String, base: String, head: String): Pair<List<String>, String> {
        val diffRequest = authenticatedBuilder
            .addHeader("Accept", "application/vnd.github.v3.diff")
            .url("https://api.github.com/repos/$owner/$repo/compare/$base...$head").build()
        val diffResponse = client.newCall(diffRequest).execute()
        if (!diffResponse.isSuccessful) {
            throw GithubAPICallError(diffRequest.url().toString())
        }
        val diff = diffResponse.body()!!.string()
        val changesRequest = authenticatedBuilder
            .url("https://api.github.com/repos/$owner/$repo/compare/$base...$head").build()
        val changesResponse = client.newCall(changesRequest).execute()
        if (!changesResponse.isSuccessful) {
            throw GithubAPICallError(diffRequest.url().toString())
        }
        val body = changesResponse.body()!!.string()
        val json = JSONObject(body)
        val changedFiles = json.getJSONArray("files").map { (it as JSONObject).getString("filename") }
        return changedFiles to diff
    }

    fun commentCoverageReport(owner: String, repo: String, gitType: GitType, reportType: ReportType, base: String,
                              pullRequestNumber: Int, report: CoverageDiffReport) {
        val listRequest = authenticatedBuilder
            .url("https://api.github.com/repos/$owner/$repo/issues/$pullRequestNumber/comments").build()
        val response = client.newCall(listRequest).execute()
        if (!response.isSuccessful) {
            throw GithubAPICallError(listRequest.url().toString())
        }
        val body = response.body()!!.string()
        val json = JSONArray(body).map { it as JSONObject }
        val original = json.firstOrNull { it.getString("body").startsWith(prefix) }
        val comment = buildReportContent(owner, repo, gitType, reportType, base, pullRequestNumber, report)
        if (original == null) {
            val createRequest = authenticatedBuilder
                .post(RequestBody.create(JSON, JSONObject().put("body", comment).toString()))
                .url("https://api.github.com/repos/$owner/$repo/issues/$pullRequestNumber/comments").build()
            client.newCall(createRequest).execute()
        } else {
            val updateRequest = authenticatedBuilder
                .post(RequestBody.create(JSON, JSONObject().put("body", comment).toString()))
                .url("https://api.github.com/repos/$owner/$repo/issues/comments/${original.getInt("id")}").build()
            client.newCall(updateRequest).execute()
        }
    }

    fun buildReportContent(owner: String, repo: String, gitType: GitType, reportType: ReportType, base: String,
                           pullRequestNumber: Int, report: CoverageDiffReport): String {
        val lines = listOf(
            listOf("@@", "", "Coverage", "Diff", "", "@@"),
            listOf("##", "", base, "#$pullRequestNumber", "+/-", "##"),
            listOf("", "", "", "", "", ""),
            buildLine(signed = true, reversed = false, percentage = true, name = "Coverage", diffTuple = report.coverages),
            buildLine(signed = true, reversed = true, percentage = false, name = "Complexity", diffTuple = report.complexity),
            listOf("", "", "", "", "", ""),
            buildLine(signed = false, reversed = false, percentage = false, name = "Files", diffTuple = report.files),
            buildLine(signed = false, reversed = false, percentage = false, name = "Lines", diffTuple = report.lines),
            buildLine(signed = false, reversed = false, percentage = false, name = "Branches", diffTuple = report.branches),
            listOf("", "", "", "", "", ""),
            buildLine(signed = true, reversed = false, percentage = false, name = "Hits", diffTuple = report.hits),
            buildLine(signed = true, reversed = true, percentage = false, name = "Misses", diffTuple = report.misses),
            buildLine(signed = true, reversed = true, percentage = false, name = "Partials", diffTuple = report.partials)
        )
        val withPadding = lines.map {
            val line = it.mapIndexed { index, s ->
                val padding = lines.map { it[index].length }.max()!!
                if (index == 0) {
                    "%-${padding}s".format(s)
                } else {
                    "%${padding}s".format(s)
                }
            }.joinToString("  ")
            if (line.trim().isEmpty()) {
                "=".repeat(line.length)
            } else {
                line
            }
        }
        return """
$prefix
[Link](${WebSiteConfig.url}/view/${gitType.name.toLowerCase()}/$owner/$repo/$pullRequestNumber/${reportType.name.toLowerCase()})

```diff
${withPadding.joinToString("\n")}
```
        """.trimIndent()
    }

    fun buildLine(signed: Boolean, reversed: Boolean, percentage: Boolean, name: String, diffTuple: DiffTuple): List<String> {
        return listOf(
            if (signed) {
                (diffTuple.current - diffTuple.target).let { when {
                    it > 0 -> if (!reversed) "+" else "-"
                    it < 0 -> if (!reversed)"-" else "+"
                    else -> ""
                } }
            } else {
                ""
            },
            name,
            if (percentage) {
                "%.2f%%".format(diffTuple.target / 100.0)
            } else {
                "%d".format(diffTuple.target)
            },
            if (percentage) {
                "%.2f%%".format(diffTuple.current / 100.0)
            } else {
                "%d".format(diffTuple.current)
            },
            if (percentage) {
                "%+.2f%%".format((diffTuple.current - diffTuple.target) / 100.0 )
            } else {
                "%+d".format(diffTuple.current - diffTuple.target)
            },
            ""
        )
    }

}
