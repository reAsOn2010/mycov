package com.github.reAsOn2010.mycov.util

import com.github.reAsOn2010.mycov.MyCovTest
import com.github.reAsOn2010.mycov.config.BaseURLConfig
import com.github.reAsOn2010.mycov.model.*
import com.github.reAsOn2010.mycov.model.GitType.GITHUB
import com.github.reAsOn2010.mycov.model.ReportType.JACOCO
import okhttp3.mockwebserver.*
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.*
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.*

class GitHubUtilTest: MyCovTest() {

    @SpyBean
    lateinit var baseURLConfig: BaseURLConfig

    @Autowired
    lateinit var gitHubUtil: GitHubUtil

    @Test
    fun getPullRequestBaseAndNumberTest() {
        val server = MockWebServer()
        val baseURL = server.url("")
        Mockito.reset(baseURLConfig)
        Mockito.`when`(baseURLConfig.github).thenReturn(baseURL.toString())

        server.enqueue(MockResponse().setResponseCode(400))
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(listPullRequestResponse))
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(listPullRequestResponse))

        try {
            gitHubUtil.getPullRequestBaseAndNumber(owner, repo, "000001")
            Assert.fail()
        } catch (e: Exception) {
            Assertions.assertThat(e).isInstanceOfAny(GithubAPICallError::class.java)
        }

        try {
            gitHubUtil.getPullRequestBaseAndNumber(owner, repo, "0000001")
            Assert.fail()
        } catch (e: Exception) {
            Assertions.assertThat(e).isInstanceOfAny(PullRequestNotFound::class.java)
        }

        val result = gitHubUtil.getPullRequestBaseAndNumber(owner, repo, "5bf1fd927dfb8679496a2e6cf00cbe50c1c87145")
        assertThat(result.first).isEqualTo("6dcb09b5b57875f334f61aebed695e2e4193db5e")
        assertThat(result.second).isEqualTo(1347)
        server.close()
    }

    @Test
    fun getPullRequestBaseAndHeadTest() {
        val server = MockWebServer()
        val baseURL = server.url("")
        Mockito.reset(baseURLConfig)
        Mockito.`when`(baseURLConfig.github).thenReturn(baseURL.toString())

        server.enqueue(MockResponse().setResponseCode(400))
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(getPullRequestResponse))

        try {
            gitHubUtil.getPullRequestBaseAndHead(owner, repo, 1)
            Assert.fail()
        } catch (e: Exception) {
            Assertions.assertThat(e).isInstanceOfAny(GithubAPICallError::class.java)
        }

        val result = gitHubUtil.getPullRequestBaseAndHead(owner, repo, 1)
        assertThat(result.first).isEqualTo("6dcb09b5b57875f334f61aebed695e2e4193db5e")
        assertThat(result.second).isEqualTo("5bf1fd927dfb8679496a2e6cf00cbe50c1c87145")
        server.close()
    }

    @Test
    fun getDiffOfCommitTest() {
        val server = MockWebServer()
        val baseURL = server.url("")
        Mockito.reset(baseURLConfig)
        Mockito.`when`(baseURLConfig.github).thenReturn(baseURL.toString())
        
        server.enqueue(MockResponse().setResponseCode(400))
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(rawDiffResponse))
        server.enqueue(MockResponse().setResponseCode(400))
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(rawDiffResponse))
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(compareResponse))

        try {
            gitHubUtil.getDiffOfCommit(owner, repo, "0000000", "0000001")
            Assert.fail()
        } catch (e: Exception) {
            Assertions.assertThat(e).isInstanceOfAny(GithubAPICallError::class.java)
        }

        try {
            gitHubUtil.getDiffOfCommit(owner, repo, "0000000", "0000001")
            Assert.fail()
        } catch (e: Exception) {
            Assertions.assertThat(e).isInstanceOfAny(GithubAPICallError::class.java)
        }

        val response = gitHubUtil.getDiffOfCommit(owner, repo, "0000000", "0000001")
        assertThat(response.first.size).isEqualTo(1)
        assertThat(response.first[0]).isEqualTo("file1.txt")
        assertThat(response.second).isEqualTo(rawDiffResponse)

        server.close()
    }

    @Test
    fun commentCoverageReportTest() {
        val server = MockWebServer()
        val baseURL = server.url("")

        Mockito.reset(baseURLConfig)
        Mockito.`when`(baseURLConfig.github).thenReturn(baseURL.toString())

        server.enqueue(MockResponse().setResponseCode(400))
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(listCommentsResponse))
        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(listCommentsWithMyCovResponse))
        server.enqueue(MockResponse().setResponseCode(200))

        try {
            gitHubUtil.commentCoverageReport(owner, repo, GITHUB, JACOCO, "0000000", 10, coverageDiffReport)
            Assert.fail()
        } catch (e: Exception) {
            Assertions.assertThat(e).isInstanceOfAny(GithubAPICallError::class.java)
        }
        server.takeRequest()

        gitHubUtil.commentCoverageReport(owner, repo, GITHUB, JACOCO, "0000000", 10, coverageDiffReport)
        server.takeRequest()
        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.requestUrl.toString()).contains("/issues/10/comments")

        gitHubUtil.commentCoverageReport(owner, repo, GITHUB, JACOCO, "0000000", 10, coverageDiffReport)
        server.takeRequest()
        val request2 = server.takeRequest()
        assertThat(request2.method).isEqualTo("POST")
        assertThat(request2.requestUrl.toString()).contains("/comments/1000")
        server.close()
    }

    @Test
    fun statusCoverageReportTest() {
        val server = MockWebServer()
        val baseURL = server.url("")

        Mockito.reset(baseURLConfig)
        Mockito.`when`(baseURLConfig.github).thenReturn(baseURL.toString())

        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(MockResponse().setResponseCode(200))

        gitHubUtil.statusCoverageReport(owner, repo, "0000001", coverageOverview)
        val request = server.takeRequest()
        val body = request.body.readByteString().string(Charsets.UTF_8)
        assertThat(JSONObject(body).getString("state")).isEqualTo("success")

        gitHubUtil.statusCoverageReport(owner, repo, "0000001", coverageOverview.apply { line.covered = 30 })
        val request2 = server.takeRequest()
        val body2 = request2.body.readByteString().string(Charsets.UTF_8)
        assertThat(JSONObject(body2).getString("state")).isEqualTo("failure")
    }
}