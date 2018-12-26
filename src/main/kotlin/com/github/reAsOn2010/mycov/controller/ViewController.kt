package com.github.reAsOn2010.mycov.controller

import com.github.reAsOn2010.mycov.model.*
import com.github.reAsOn2010.mycov.model.ReportType.JACOCO
import com.github.reAsOn2010.mycov.store.CoverageStore
import com.github.reAsOn2010.mycov.util.GitHubUtil
import com.google.common.net.UrlEscapers
import com.google.gson.Gson
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/view")
class ViewController(private val gitHubUtil: GitHubUtil,
                     private val coverageStore: CoverageStore) {

    @RequestMapping(value = ["/{git_type}/{owner}/{repo}/{pullRequestNumber}/{report_type}"])
    fun view(@PathVariable("git_type") gitType: GitType,
             @PathVariable("owner") owner: String,
             @PathVariable("repo") repo: String,
             @PathVariable("report_type") reportType: ReportType,
             @PathVariable("pullRequestNumber") pullRequestNumber: Int): String {
        val (baseSha, headSha) = gitHubUtil.getPullRequestBaseAndHead(owner, repo, pullRequestNumber)
        val (changedFiles, rawDiff) = gitHubUtil.getDiffOfCommit(owner, repo, baseSha, headSha)
        val diff = UrlEscapers.urlPathSegmentEscaper().escape(rawDiff)
        val (baseCoverages, headCoverages) = coverageStore.getCoveragesForFiles(
            "$owner/$repo", gitType, reportType, baseSha, headSha, changedFiles)
        val base = baseCoverages.mapValues { it.value.lineCoverageFile.map { it.toSimple() } }
        val head = headCoverages.mapValues { it.value.lineCoverageFile.map { it.toSimple() } }
        return """
<!DOCTYPE html>
<html>
<head>
<link rel="stylesheet" type="text/css" href="https://diff2html.xyz/assets/diff2html.min.css">
<style type="text/css">
.covered {
    background-color: #99ff99;
    border-color: #b4e2b4;
}
.partial {
    background-color: #ffff99;
    border-color: #f3f781;
}
.missed {
    background-color: #ff9999;
    border-color: #e9aeae;
}
</style>
</head>
<body>
<div id="diff2html"></div>
<script type="text/javascript" src="https://diff2html.xyz/assets/diff2html.min.js"></script>
<script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
<script>
var diffHtml = Diff2Html.getPrettyHtml(
  decodeURIComponent("$diff"),
  {inputFormat: 'diff', showFiles: true, matching: 'lines', outputFormat: 'side-by-side'}
);
document.getElementById("diff2html").innerHTML = diffHtml;
</script>
<script>
var base = ${Gson().toJson(base)}
var current = ${Gson().toJson(head)}

${'$'}(document).ready(function() {
    for (var item of ${'$'}(".d2h-file-wrapper")) {
        var filename = ${'$'}(item).find(".d2h-file-name").text()
        var baseCoverage = base[filename]
        var currentCoverage = current[filename]
        var baseDiffTable = ${'$'}(item).find(".d2h-diff-table")[0]
        var currentDiffTable = ${'$'}(item).find(".d2h-diff-table")[1]

        var render = (coverage, table) => {
            for (var line of coverage) {
                var state = ""
                var tip = ""
                if (line.mb == 0 && line.mi == 0) {
                    state = "covered"
                }
                else if (line.cb > 0 && line.mb > 0) {
                    state = "partial"
                    tip = `covered ${'$'}{line.cb} branches of ${'$'}{line.cb + line.mb} total`
                } else {
                    state = "missed"
                }
                var td = ${'$'}(table).find("td.d2h-code-side-linenumber").filter(function() {
                    return ${'$'}(this).text().trim() === `${'$'}{line.nr}`
                })[0]
                ${'$'}(td).removeClass()
                ${'$'}(td).addClass(`d2h-code-side-linenumber ${'$'}{state}`)
                if (!!tip) {
                    ${'$'}(td).attr("title", tip)
                }
            }
        }

        if (baseCoverage) {
            console.log(filename)
            console.log(baseCoverage)
            render(baseCoverage, baseDiffTable)
        }
        if (currentCoverage) {
            console.log(filename)
            console.log(currentCoverage)
            render(currentCoverage, currentDiffTable)
        }
    }

})

</script>
</body>
</html>
        """.trimIndent()
    }
}