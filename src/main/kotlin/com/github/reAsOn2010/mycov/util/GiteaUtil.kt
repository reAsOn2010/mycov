package com.github.reAsOn2010.mycov.util

import com.github.reAsOn2010.mycov.config.GiteaConfig
import com.github.reAsOn2010.mycov.model.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.springframework.stereotype.Component

@Component
class GiteaUtil(private val giteaConfig: GiteaConfig,
                private val commonUtil: CommonUtil) : GitServiceInterface {

    val client = OkHttpClient()
    val JSON = "application/json; charset=utf-8".toMediaType()
    val authenticatedBuilder get() = Request.Builder()
        .addHeader("Authorization", "token ${giteaConfig.token}")

    override fun getPullRequestBaseAndNumber(owner: String, repo: String, head: String): Pair<String, Int> {
        val request = authenticatedBuilder
            .url("${giteaConfig.baseUrl}/repos/$owner/$repo/pulls?sort=recentupdate").build()
        val response = client.newCall(request).execute()
        response.use {
            if (!response.isSuccessful) {
                throw GiteaAPICallError(request.url.toString())
            }
            val body = response.body!!.string()
            val json = JSONArray(body).map { it as JSONObject }
            val targetPullRequest = json.firstOrNull { it.getJSONObject("head").getString("sha") == head }
            if (targetPullRequest == null) {
                throw PullRequestNotFound(head)
            } else {
                val pullRequestNumber = targetPullRequest.getInt("number")
                val baseCommit = targetPullRequest.getJSONObject("base").getString("sha")
                return baseCommit to pullRequestNumber
            }
        }
    }

    override fun getPullRequestBaseAndHead(owner: String, repo: String, pullRequestNumber: Int): Pair<String, String> {
        val request = authenticatedBuilder
            .url("${giteaConfig.baseUrl}/repos/$owner/$repo/pulls/$pullRequestNumber").build()
        val response = client.newCall(request).execute()
        response.use {
            if (!response.isSuccessful) {
                throw GiteaAPICallError(request.url.toString())
            }
            val body = response.body!!.string()
            val json = JSONObject(body)
            val base = json.getJSONObject("base").getString("sha")
            val head = json.getJSONObject("head").getString("sha")
            return base to head
        }
    }

    override fun getDiffOfCommit(owner: String, repo: String, pullRequestNumber: Int): Pair<List<String>, String> {
        val diffRequest = authenticatedBuilder
            .url("${giteaConfig.baseUrl}/repos/$owner/$repo/pulls/$pullRequestNumber.diff").build()
        val diffResponse = client.newCall(diffRequest).execute()
        val diff = diffResponse.use {
            if (!diffResponse.isSuccessful) {
                throw GiteaAPICallError(diffRequest.url.toString())
            }
            diffResponse.body!!.string()
        }
        val filesRequest = authenticatedBuilder
            .url("${giteaConfig.baseUrl}/repos/$owner/$repo/pulls/$pullRequestNumber/files?limit=100").build()
        val filesResponse = client.newCall(filesRequest).execute()
        val files = filesResponse.use {
            if (!filesResponse.isSuccessful) {
                throw GiteaAPICallError(diffRequest.url.toString())
            }
            val body = filesResponse.body!!.string()
            JSONArray(body).map { (it as JSONObject).getString("filename") }
        }
        return files to diff
    }

    override fun commentCoverageReport(owner: String, repo: String, reportType: ReportType, base: String,
                                       pullRequestNumber: Int, report: CoverageDiffReport) {
        val listRequest = authenticatedBuilder
            .url("${giteaConfig.baseUrl}/repos/$owner/$repo/issues/$pullRequestNumber/comments").build()
        val response = client.newCall(listRequest).execute()
        response.use {
            if (!response.isSuccessful) {
                throw GiteaAPICallError(listRequest.url.toString())
            }
            val body = response.body!!.string()
            val json = JSONArray(body).map { it as JSONObject }
            val original = json.firstOrNull { it.getString("body").startsWith(commonUtil.reportPrefix) }
            val comment = commonUtil.buildReportContent(owner, repo, GitType.GITEA, reportType, base, pullRequestNumber, report)
            if (original == null) {
                val createRequest = authenticatedBuilder
                    .post(JSONObject().put("body", comment).toString().toRequestBody(JSON))
                    .url("${giteaConfig.baseUrl}/repos/$owner/$repo/issues/$pullRequestNumber/comments").build()
                client.newCall(createRequest).execute().close()
            } else {
                val updateRequest = authenticatedBuilder
                    .patch(JSONObject().put("body", comment).toString().toRequestBody(JSON))
                    .url("${giteaConfig.baseUrl}/repos/$owner/$repo/issues/comments/${original.getInt("id")}").build()
                client.newCall(updateRequest).execute().close()
            }
        }
    }

    override fun statusCoverageReport(owner: String, repo: String, head: String, overview: CoverageOverview) {
        val value = 100.0 * overview.line.covered / (overview.line.covered + overview.line.missed + overview.line.partial)
        val percent = "%.2f%%".format(value)
        val status = mapOf("state" to if (value > 80) "success" else if (value > 60) "warning" else "failure",
            "description" to "Coverage of this commit is $percent",
            "context" to "ci/mycov")
        val request = authenticatedBuilder
            .post(JSONObject(status).toString().toRequestBody(JSON))
            .url("${giteaConfig.baseUrl}/repos/$owner/$repo/statuses/$head").build()
        client.newCall(request).execute().close()
    }

}
