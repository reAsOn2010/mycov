package com.github.reAsOn2010.mycov.store

import com.github.reAsOn2010.mycov.MyCovTest
import com.github.reAsOn2010.mycov.model.GitType.GITHUB
import com.github.reAsOn2010.mycov.model.ReportType.JACOCO
import org.assertj.core.api.Assertions.assertThat
import org.dom4j.io.SAXReader
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.xml.sax.InputSource

class CoverageStoreTest: MyCovTest() {

    @Autowired
    lateinit var coverageStore: CoverageStore

    @Test
    fun getCoveragesForFilesTest() {
        val baseXML = ClassPathResource("/jacocoTestReport-master.xml").file.inputStream()
        val headXML = ClassPathResource("/jacocoTestReport-pr.xml").file.inputStream()
        val reader = SAXReader()
        reader.setEntityResolver { _, _ ->
            InputSource(ClassPathResource("/report.dtd").inputStream)
        }
        coverageStore.store("test", GITHUB, JACOCO, "0000000", reader.read(baseXML))
        coverageStore.store("test", GITHUB, JACOCO, "0000001", reader.read(headXML))
        val filename = "cn/patest/offlinePTA/application.kt"
        val result = coverageStore.getCoveragesForFiles("test", GITHUB, JACOCO, "0000000", "0000001", listOf(filename))
        assertThat(result.first).containsKey(filename)
        assertThat(result.second).containsKey(filename)
    }
}