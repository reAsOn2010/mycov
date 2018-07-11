package com.github.reAsOn2010.mycov.util

import com.github.reAsOn2010.mycov.MyCovTest
import com.github.reAsOn2010.mycov.config.BaseURLConfig
import com.github.reAsOn2010.mycov.model.GithubAPICallError
import okhttp3.mockwebserver.*
import org.assertj.core.api.Assertions
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

        try {
            gitHubUtil.getPullRequestBaseAndNumber("reAsOn2010", "mycov", "000001")
            Assert.fail()
        } catch (e: Exception) {
            Assertions.assertThat(e).isInstanceOfAny(GithubAPICallError::class.java)
        }
    }
}