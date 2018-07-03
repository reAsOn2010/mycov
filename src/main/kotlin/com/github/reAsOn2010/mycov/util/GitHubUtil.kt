package com.github.reAsOn2010.mycov.util

import com.github.reAsOn2010.mycov.config.*
import com.github.reAsOn2010.mycov.model.GitType
import com.github.reAsOn2010.mycov.model.*
import com.google.gson.JsonObject
import okhttp3.*
import org.json.*
import org.springframework.stereotype.Component

@Component
class GitHubUtil() {
    companion object {
        val client = OkHttpClient()
        val authenticatedBuilder get() = Request.Builder()
            .addHeader("Authorization", "token ${TokenConfig.githubAccessToken}")
        val prefix = "MyCov coverage report"
        val JSON = MediaType.parse("application/json; charset=utf-8")
    }

    fun isCommitOnTargetBranch(owner: String, repo: String, targetBranch: String, sha: String): Boolean {
        val request = authenticatedBuilder
            .url("https://api.github.com/repos/$owner/$repo/compare/$targetBranch...$sha").build()
        val response = client.newCall(request).execute()
        val body = response.body()!!.string()
        val json = JSONObject(body)
        val status = json.getString("status")
        return status == "identical" || status == "behind"
    }

    fun getPullRequestBaseAndNumber(owner: String, repo: String, sha: String): Pair<String, Int> {
        val request = authenticatedBuilder
            .url("https://api.github.com/repos/$owner/$repo/pulls?sort=updated&direction=desc").build()
        val response = client.newCall(request).execute()
        val body = response.body()!!.string()

        val json = JSONArray(body).map { it as JSONObject }
        val targetPullRequest = json.firstOrNull { it.getJSONObject("head").getString("sha").startsWith(sha) }
        val pullRequestNumber = targetPullRequest ?.getInt("number") ?: 0
        val baseBranch = targetPullRequest ?.getJSONObject("base") ?.getString("ref") ?: "unknown"
        return baseBranch to pullRequestNumber
    }

    fun getPullRequestBaseAndHead(owner: String, repo: String, pullRequestNumber: Int): Pair<String, String> {
        val request = authenticatedBuilder
            .url("https://api.github.com/repos/$owner/$repo/pulls/$pullRequestNumber").build()
        val response = client.newCall(request).execute()
        val body = response.body()!!.string()

        val json = JSONObject(body)
        val base = json.getJSONObject("base") ?.getString("ref") ?: "unknown"
        val head = json.getJSONObject("head") ?.getString("sha") ?.substring(0, 7) ?: "0000000"
        return base to head
    }

    fun getDiffOfCommit(owner: String, repo: String, targetBranch: String, sha: String): Pair<List<String>, String> {
        val diffRequest = authenticatedBuilder
            .addHeader("Accept", "application/vnd.github.v3.diff")
            .url("https://api.github.com/repos/$owner/$repo/compare/$targetBranch...$sha").build()
        val diff = client.newCall(diffRequest).execute().body()!!.string()
        val changesRequest = authenticatedBuilder
            .url("https://api.github.com/repos/$owner/$repo/compare/$targetBranch...$sha").build()
        val body = client.newCall(changesRequest).execute().body()!!.string()
        val json = JSONObject(body)
        val changedFiles = json.getJSONArray("files").map { (it as JSONObject).getString("filename") }
        return changedFiles to diff
    }

    fun commentCoverageReport(owner: String, repo: String, branch: String, pullRequestNumber: Int, report: CoverageDiffReport) {
        val listRequest = authenticatedBuilder
            .url("https://api.github.com/repos/$owner/$repo/issues/$pullRequestNumber/comments").build()
        val response = client.newCall(listRequest).execute()
        val body = response.body()!!.string()
        val json = JSONArray(body).map { it as JSONObject }
        val original = json.firstOrNull { it.getString("body").startsWith(prefix) }
        val comment = buildReportContent(owner, repo, branch, pullRequestNumber, report)
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

    fun buildReportContent(owner: String, repo: String, branch: String, pullRequestNumber: Int, report: CoverageDiffReport): String {
        val lines = listOf(
            listOf("@@", "", "Coverage", "Diff", "", "@@"),
            listOf("##", "", "$branch", "#$pullRequestNumber", "+/-", "##"),
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
        val withPaddding = lines.map {
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
[Link](${WebSiteConfig.url}/view/github/$owner/$repo/$pullRequestNumber)

```diff
${withPaddding.joinToString("\n")}
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
