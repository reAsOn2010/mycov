package com.github.reAsOn2010.mycov.controller

import com.github.reAsOn2010.mycov.async.ParseReportTaskFactory
import com.github.reAsOn2010.mycov.store.CoverageStore
import com.github.reAsOn2010.mycov.util.GitHubUtil
import org.dom4j.io.SAXReader
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.xml.sax.InputSource
import javax.servlet.ServletRequest

@RestController
@RequestMapping("/report")
class ReportController(private val parseReportTaskFactory: ParseReportTaskFactory) {

    enum class GitType {
        GITHUB,
        GITLAB
    }

    enum class ReportType {
        JACOCO
    }

    @RequestMapping(value = "/{git_type}/{owner}/{repo}/{commit}/{report_type}", method = [POST],
        consumes = ["text/xml; charset=utf-8"],
        produces = ["application/json; charset=utf-8"])
    fun report(@PathVariable("git_type") gitType: GitType,
               @PathVariable("owner") owner: String,
               @PathVariable("repo") repo: String,
               @PathVariable("commit") commit: String,
               @PathVariable("report_type") reportType: ReportType,
               @RequestParam(value = "branch", required = false) branch: String?,
               @RequestParam(value = "pull", required = false) pullRequestNumber: Int?,
               request: ServletRequest) {
        val reader = SAXReader()
        reader.setEntityResolver { _, _ ->
            InputSource(System::class.java.getResourceAsStream("/report.dtd"))
        }
        val document = reader.read(request.inputStream)
        val task = parseReportTaskFactory.genTask()
        task.execute(gitType, owner, repo, commit, reportType, document, branch, pullRequestNumber)
    }
}