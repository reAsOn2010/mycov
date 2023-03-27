package com.github.reAsOn2010.mycov.util

import com.github.reAsOn2010.mycov.config.WebSiteConfig
import com.github.reAsOn2010.mycov.model.CoverageDiffReport
import com.github.reAsOn2010.mycov.model.DiffTuple
import com.github.reAsOn2010.mycov.model.GitType
import com.github.reAsOn2010.mycov.model.GitType.*
import com.github.reAsOn2010.mycov.model.ReportType
import org.springframework.stereotype.Component


@Component
class Util(private val gitHubUtil: GitHubUtil,
           private val giteaUtil: GiteaUtil) {
    fun get(type: GitType): GitServiceInterface {
        return when(type) {
            GITHUB -> gitHubUtil
            GITEA -> giteaUtil
            else -> throw NotImplementedError()
        }
    }
}

@Component
class CommonUtil(private val webSiteConfig: WebSiteConfig) {

    val reportPrefix = "MyCov coverage report"

    fun buildReportContent(owner: String, repo: String, gitType: GitType, reportType: ReportType, base: String,
                           pullRequestNumber: Int, report: CoverageDiffReport): String {
        val lines = listOf(
            listOf("@@", "", "Coverage", "Diff", "", "@@"),
            listOf("##", "", base.substring(0, 7), "#$pullRequestNumber", "+/-", "##"),
            listOf("", "", "", "", "", ""),
            buildLine(signed = true, reversed = false, percentage = true, name = "Coverage", diffTuple = report.coverages),
            buildLine(signed = true, reversed = true, percentage = false, name = "Complexity", diffTuple = report.complexity),
            listOf("", "", "", "", "", ""),
            buildLine(signed = false, reversed = false, percentage = false, name = "Files", diffTuple = report.files),
            buildLine(signed = false, reversed = false, percentage = false, name = "Lines", diffTuple = report.lines),
            buildLine(signed = false, reversed = false, percentage = false, name = "Branches", diffTuple = report.branches),
            listOf("", "", "", "", "", ""),
            buildLine(signed = true, reversed = false, percentage = false, name = "Hits", diffTuple = report.hits),
            buildLine(signed = true, reversed = true, percentage = false, name = "Misses", diffTuple = report.misses),
            buildLine(signed = true, reversed = true, percentage = false, name = "Partials", diffTuple = report.partials)
        )
        val withPadding = lines.map {
            val line = it.mapIndexed { index, s ->
                val padding = lines.maxOf { line -> line[index].length }
                if (index == 0) {
                    "%-${padding}s".format(s)
                } else {
                    "%${padding}s".format(s)
                }
            }.joinToString("  ")
            if (line.trim().isEmpty()) {
                "=".repeat(line.length)
            } else {
                line
            }
        }
        return """
            $reportPrefix
            [Link](${webSiteConfig.url}/view/${gitType.name.lowercase()}/$owner/$repo/$pullRequestNumber/${reportType.name.lowercase()})

            ```diff
            ${withPadding.joinToString("\n")}
            ```
        """.trimIndent()
    }

    fun buildLine(signed: Boolean, reversed: Boolean, percentage: Boolean, name: String, diffTuple: DiffTuple): List<String> {
        return listOf(
            if (signed) {
                (diffTuple.current - diffTuple.target).let { when {
                    it > 0 -> if (!reversed) "+" else "-"
                    it < 0 -> if (!reversed)"-" else "+"
                    else -> ""
                } }
            } else {
                ""
            },
            name,
            if (percentage) {
                "%.2f%%".format(diffTuple.target / 100.0)
            } else {
                "%d".format(diffTuple.target)
            },
            if (percentage) {
                "%.2f%%".format(diffTuple.current / 100.0)
            } else {
                "%d".format(diffTuple.current)
            },
            if (percentage) {
                "%+.2f%%".format((diffTuple.current - diffTuple.target) / 100.0 )
            } else {
                "%+d".format(diffTuple.current - diffTuple.target)
            },
            ""
        )
    }
}
