package com.github.reAsOn2010.mycov.util

import com.github.reAsOn2010.mycov.MyCovTest
import com.github.reAsOn2010.mycov.config.GiteaConfig
import com.github.reAsOn2010.mycov.model.GiteaAPICallError
import com.github.reAsOn2010.mycov.model.PullRequestNotFound
import com.github.reAsOn2010.mycov.model.ReportType.JACOCO
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean

class GiteaUtilTest: MyCovTest() {

    @SpyBean
    lateinit var giteaConfig: GiteaConfig

    @Autowired
    lateinit var giteaUtil: GiteaUtil

    @Test
    fun getPullRequestBaseAndNumberTest() {
        val server = MockWebServer()
        val baseURL = server.url("")
        Mockito.reset(giteaConfig)
        Mockito.`when`(giteaConfig.baseUrl).thenReturn(baseURL.toString())

        server.enqueue(MockResponse().setResponseCode(400))
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(listPullRequestResponse))
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(listPullRequestResponse))

        try {
            giteaUtil.getPullRequestBaseAndNumber(owner, repo, "000001")
            Assert.fail()
        } catch (e: Exception) {
            Assertions.assertThat(e).isInstanceOfAny(GiteaAPICallError::class.java)
        }

        try {
            giteaUtil.getPullRequestBaseAndNumber(owner, repo, "0000001")
            Assert.fail()
        } catch (e: Exception) {
            Assertions.assertThat(e).isInstanceOfAny(PullRequestNotFound::class.java)
        }

        val result = giteaUtil.getPullRequestBaseAndNumber(owner, repo, "5bf1fd927dfb8679496a2e6cf00cbe50c1c87145")
        Assertions.assertThat(result.first).isEqualTo("6dcb09b5b57875f334f61aebed695e2e4193db5e")
        Assertions.assertThat(result.second).isEqualTo(1347)
        server.close()
    }

    @Test
    fun getPullRequestBaseAndHeadTest() {
        val server = MockWebServer()
        val baseURL = server.url("")
        Mockito.reset(giteaConfig)
        Mockito.`when`(giteaConfig.baseUrl).thenReturn(baseURL.toString())

        server.enqueue(MockResponse().setResponseCode(400))
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(getPullRequestResponse))

        try {
            giteaUtil.getPullRequestBaseAndHead(owner, repo, 1)
            Assert.fail()
        } catch (e: Exception) {
            Assertions.assertThat(e).isInstanceOfAny(GiteaAPICallError::class.java)
        }

        val result = giteaUtil.getPullRequestBaseAndHead(owner, repo, 1)
        Assertions.assertThat(result.first).isEqualTo("6dcb09b5b57875f334f61aebed695e2e4193db5e")
        Assertions.assertThat(result.second).isEqualTo("5bf1fd927dfb8679496a2e6cf00cbe50c1c87145")
        server.close()
    }

    @Test
    fun getDiffOfCommitTest() {
        val server = MockWebServer()
        val baseURL = server.url("")
        Mockito.reset(giteaConfig)
        Mockito.`when`(giteaConfig.baseUrl).thenReturn(baseURL.toString())

        server.enqueue(MockResponse().setResponseCode(400))
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(rawDiffResponse))
        server.enqueue(MockResponse().setResponseCode(400))
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(rawDiffResponse))
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(giteaFileResponse))

        try {
            giteaUtil.getDiffOfCommit(owner, repo, 1)
            Assert.fail()
        } catch (e: Exception) {
            Assertions.assertThat(e).isInstanceOfAny(GiteaAPICallError::class.java)
        }

        try {
            giteaUtil.getDiffOfCommit(owner, repo, 1)
            Assert.fail()
        } catch (e: Exception) {
            Assertions.assertThat(e).isInstanceOfAny(GiteaAPICallError::class.java)
        }

        val response = giteaUtil.getDiffOfCommit(owner, repo, 1)
        Assertions.assertThat(response.first.size).isEqualTo(1)
        Assertions.assertThat(response.first[0]).isEqualTo("file1.txt")
        Assertions.assertThat(response.second).isEqualTo(rawDiffResponse)

        server.close()
    }

    @Test
    fun commentCoverageReportTest() {
        val server = MockWebServer()
        val baseURL = server.url("")

        Mockito.reset(giteaConfig)
        Mockito.`when`(giteaConfig.baseUrl).thenReturn(baseURL.toString())

        server.enqueue(MockResponse().setResponseCode(400))
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(listCommentsResponse))
        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(listCommentsWithMyCovResponse))
        server.enqueue(MockResponse().setResponseCode(200))

        try {
            giteaUtil.commentCoverageReport(owner, repo, JACOCO, "0000000", 10, coverageDiffReport)
            Assert.fail()
        } catch (e: Exception) {
            Assertions.assertThat(e).isInstanceOfAny(GiteaAPICallError::class.java)
        }
        server.takeRequest()

        giteaUtil.commentCoverageReport(owner, repo, JACOCO, "0000000", 10, coverageDiffReport)
        server.takeRequest()
        val request = server.takeRequest()
        Assertions.assertThat(request.method).isEqualTo("POST")
        Assertions.assertThat(request.requestUrl.toString()).contains("/issues/10/comments")

        giteaUtil.commentCoverageReport(owner, repo, JACOCO, "0000000", 10, coverageDiffReport)
        server.takeRequest()
        val request2 = server.takeRequest()
        Assertions.assertThat(request2.method).isEqualTo("PATCH")
        Assertions.assertThat(request2.requestUrl.toString()).contains("/comments/1000")
        server.close()
    }

    @Test
    fun statusCoverageReportTest() {
        val server = MockWebServer()
        val baseURL = server.url("")

        Mockito.reset(giteaConfig)
        Mockito.`when`(giteaConfig.baseUrl).thenReturn(baseURL.toString())

        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(MockResponse().setResponseCode(200))

        giteaUtil.statusCoverageReport(owner, repo, "0000001", coverageOverview)
        val request = server.takeRequest()
        val body = request.body.readByteString().string(Charsets.UTF_8)
        Assertions.assertThat(JSONObject(body).getString("state")).isEqualTo("success")

        giteaUtil.statusCoverageReport(owner, repo, "0000001", coverageOverview.apply { line.covered = 5 })
        val request2 = server.takeRequest()
        val body2 = request2.body.readByteString().string(Charsets.UTF_8)
        Assertions.assertThat(JSONObject(body2).getString("state")).isEqualTo("failure")

        giteaUtil.statusCoverageReport(owner, repo, "0000001", coverageOverview.apply { line.covered = 30 })
        val request3 = server.takeRequest()
        val body3 = request3.body.readByteString().string(Charsets.UTF_8)
        Assertions.assertThat(JSONObject(body3).getString("state")).isEqualTo("warning")
    }
}
