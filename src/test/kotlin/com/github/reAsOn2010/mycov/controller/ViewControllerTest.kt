package com.github.reAsOn2010.mycov.controller

import com.github.reAsOn2010.mycov.MyCovTest
import com.github.reAsOn2010.mycov.dao.CoverageRecordDao
import com.github.reAsOn2010.mycov.model.CoverageFile
import com.github.reAsOn2010.mycov.store.CoverageStore
import com.github.reAsOn2010.mycov.util.GitHubUtil
import com.nhaarman.mockito_kotlin.any
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean

class ViewControllerTest: MyCovTest() {
    @MockBean
    lateinit var githubUtil: GitHubUtil

    @MockBean
    lateinit var coverageStore: CoverageStore

    @Autowired
    lateinit var coverageRecordDao: CoverageRecordDao

    @Test
    fun successfully() {
        Mockito.reset(githubUtil)
        Mockito.reset(coverageStore)
        Mockito.doReturn("0000000" to "0000001").`when`(githubUtil).getPullRequestBaseAndHead(any(), any(), any())
        Mockito.doReturn(emptyList<String>() to rawDiffResponse).`when`(githubUtil).getDiffOfCommit(any(), any(), any(), any())
        Mockito.doReturn(emptyMap<String, CoverageFile>() to emptyMap<String, CoverageFile>())
            .`when`(coverageStore).getCoveragesForFiles(any(), any(), any(), any(), any(), any())
        val response = restTemplate.getForObject("/view/github/reAsOn2010/mycov/1/jacoco", String::class.java)
    }
}