package com.github.reAsOn2010.mycov.controller

import com.github.reAsOn2010.mycov.async.*
import com.github.reAsOn2010.mycov.model.*
import org.dom4j.io.SAXReader
import org.json.JSONObject
import org.springframework.core.io.ClassPathResource
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.xml.sax.InputSource
import javax.servlet.ServletRequest

@RestController
@RequestMapping("/report")
class ReportController(private val parseReportTask: ParseReportTask) {

    @RequestMapping(value = ["/{git_type}/{owner}/{repo}/{commit}/{report_type}"], method = [POST],
        consumes = ["text/xml; charset=utf-8"],
        produces = ["application/json; charset=utf-8"])
    fun report(@PathVariable("git_type") gitType: GitType,
               @PathVariable("owner") owner: String,
               @PathVariable("repo") repo: String,
               @PathVariable("commit") commit: String,
               @PathVariable("report_type") reportType: ReportType,
               request: ServletRequest): String {
        val reader = SAXReader()
        reader.setEntityResolver { _, _ ->
            InputSource(ClassPathResource("/report.dtd").inputStream)
        }
        val document = reader.read(request.inputStream)
        parseReportTask.execute(gitType, owner, repo, commit, reportType, document)
        return JSONObject().put("status", "ok").toString()
    }

}