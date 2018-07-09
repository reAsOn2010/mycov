package com.github.reAsOn2010.mycov.controller

import com.github.reAsOn2010.mycov.MyCovTest
import com.github.reAsOn2010.mycov.model.PullRequestNotFound
import com.github.reAsOn2010.mycov.store.CoverageStore
import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.http.*

class ReportControllerTest: MyCovTest() {

    @Autowired
    lateinit var coverageStore: CoverageStore

    @Test
    fun successfullyWithNoPR() {
        val head = "0000000"
        Mockito.reset(githubUtil)
        Mockito.doThrow(PullRequestNotFound(head)).`when`(githubUtil).getPullRequestBaseAndNumber(any(), any(), any())
        val xml = ClassPathResource("/jacocoTestReport-master.xml").file.readText()
        val headers = HttpHeaders().apply { contentType = MediaType.TEXT_XML }
        val request = HttpEntity<String>(xml, headers)
        val response = restTemplate.postForObject("/report/github/reAsOn2010/mycov/$head/jacoco",
            request, String::class.java)
        val json = JSONObject(response)
        assertThat(json.getString("status")).isEqualTo("ok")
        assertThat(coverageStore.getEntry("reAsOn2010/mycov")).isNotNull
        assertThat(coverageStore.getEntry("reAsOn2010/mycov")!!.commits[head]).isNotNull
    }

    @Test
    fun successfullyWithPRButNoBase() {
        val head = "0000001"
        val base = "0000000"
        Mockito.reset(githubUtil)
        Mockito.doReturn(base to 1).`when`(githubUtil).getPullRequestBaseAndNumber(any(), any(), any())
        val xml = ClassPathResource("/jacocoTestReport-pr.xml").file.readText()
        val headers = HttpHeaders().apply { contentType = MediaType.TEXT_XML }
        val request = HttpEntity<String>(xml, headers)
        val response = restTemplate.postForObject("/report/github/reAsOn2010/mycov/$head/jacoco",
            request, String::class.java)
        val json = JSONObject(response)
        assertThat(json.getString("status")).isEqualTo("ok")
        assertThat(coverageStore.getEntry("reAsOn2010/mycov")).isNotNull
        assertThat(coverageStore.getEntry("reAsOn2010/mycov")!!.commits[head]).isNotNull
    }

    @Test
    fun successfullyWithPRAndBase() {
        val head = "0000001"
        val base = "0000000"
        Mockito.reset(githubUtil)
        Mockito.doThrow(PullRequestNotFound(head)).`when`(githubUtil).getPullRequestBaseAndNumber(any(), any(), eq(base))
        Mockito.doReturn(base to 1).`when`(githubUtil).getPullRequestBaseAndNumber(any(), any(), eq(head))
        val baseXml = ClassPathResource("/jacocoTestReport-master.xml").file.readText()
        val headers = HttpHeaders().apply { contentType = MediaType.TEXT_XML }
        val baseRequest = HttpEntity<String>(baseXml, headers)
        val baseResponse = restTemplate.postForObject("/report/github/reAsOn2010/mycov/$base/jacoco",
            baseRequest, String::class.java)
        val baseJson = JSONObject(baseResponse)
        assertThat(baseJson.getString("status")).isEqualTo("ok")

        val headXml = ClassPathResource("/jacocoTestReport-pr.xml").file.readText()
        val headRequest = HttpEntity<String>(headXml, headers)
        val headResponse = restTemplate.postForObject("/report/github/reAsOn2010/mycov/$head/jacoco",
            headRequest, String::class.java)
        val headJson = JSONObject(headResponse)
        assertThat(headJson.getString("status")).isEqualTo("ok")

        verify(githubUtil, times(1)).commentCoverageReport(any(), any(), eq(base), eq(1), any())

        assertThat(coverageStore.getEntry("reAsOn2010/mycov")).isNotNull
        assertThat(coverageStore.getEntry("reAsOn2010/mycov")!!.commits[base]).isNotNull
        assertThat(coverageStore.getEntry("reAsOn2010/mycov")!!.commits[head]).isNotNull
    }
}