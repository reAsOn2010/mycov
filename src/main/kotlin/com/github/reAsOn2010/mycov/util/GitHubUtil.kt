package com.github.reAsOn2010.mycov.util

import com.github.reAsOn2010.mycov.controller.ReportController.GitType
import okhttp3.*
import org.json.JSONObject
import org.springframework.stereotype.Component

@Component
class GitHubUtil {
    companion object {
        val client = OkHttpClient()
    }

    fun isCommitOnTargetBranch(owner: String, repo: String, targetBranch: String, sha: String): Boolean {
        val request = Request.Builder()
            .addHeader("Authorization", "token 4af7be3d1b46316793945d88cf59dbcd4b4c8664")
            .url("https://api.github.com/repos/$owner/$repo/compare/$sha...$targetBranch").build()
        val response = client.newCall(request).execute()
        val body = response.body()!!.string()
        val json = JSONObject(body)
        val status = json.getString("status")
        return status == "identical" || status == "ahead"
    }
}

fun main(args: Array<String>) {
    val util = GitHubUtil()
    GitType.valueOf("GITHUB")
    println(util.isCommitOnTargetBranch("pintia", "inside-identity", "master", "909b76d"))
}