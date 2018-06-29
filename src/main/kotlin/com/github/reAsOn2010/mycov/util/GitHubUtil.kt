package com.github.reAsOn2010.mycov.util

import com.github.reAsOn2010.mycov.controller.ReportController.GitType
import okhttp3.*
import org.json.*
import org.springframework.stereotype.Component

@Component
class GitHubUtil {
    companion object {
        val client = OkHttpClient()
    }

    fun isCommitOnTargetBranch(owner: String, repo: String, targetBranch: String, sha: String): Boolean {
        val request = Request.Builder()
            .addHeader("Authorization", "token e80fa2cf5dc259036f751afb645d4d041dabed4e")
            .url("https://api.github.com/repos/$owner/$repo/compare/$sha...$targetBranch").build()
        val response = client.newCall(request).execute()
        val body = response.body()!!.string()
        val json = JSONObject(body)
        val status = json.getString("status")
        return status == "identical" || status == "ahead"
    }

    fun getPullRequestNumber(owner: String, repo: String, sha: String): Int {
        val request = Request.Builder()
            .addHeader("Authorization", "token e80fa2cf5dc259036f751afb645d4d041dabed4e")
            .url("https://api.github.com/repos/$owner/$repo/pulls?sort=updated&direction=desc").build()
        val response = client.newCall(request).execute()
        val body = response.body()!!.string()
        val json = JSONArray(body).map { it as JSONObject }
        return json.firstOrNull { it.getJSONObject("head").getString("sha").startsWith(sha) }
            ?.getInt("number") ?: 0
    }
}

fun main(args: Array<String>) {
    val util = GitHubUtil()
    GitType.valueOf("GITHUB")
    println(util.getPullRequestNumber("pintia", "offline-PTA", "2e6a303"))
}