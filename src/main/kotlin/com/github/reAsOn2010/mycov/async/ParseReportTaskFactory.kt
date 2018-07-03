package com.github.reAsOn2010.mycov.async

import com.github.reAsOn2010.mycov.store.CoverageStore
import com.github.reAsOn2010.mycov.util.GitHubUtil
import org.springframework.stereotype.Component

@Component
class ParseReportTaskFactory(private val coverageStore: CoverageStore,
                             private val gitHubUtil: GitHubUtil) {

    fun genTask(): ParseReportTask {
        return ParseReportTask(coverageStore, gitHubUtil)
    }
}