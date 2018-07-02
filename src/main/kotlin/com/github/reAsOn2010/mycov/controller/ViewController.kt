package com.github.reAsOn2010.mycov.controller

import com.github.reAsOn2010.mycov.model.*
import com.github.reAsOn2010.mycov.store.CoverageStore
import com.github.reAsOn2010.mycov.util.GitHubUtil
import com.google.common.net.UrlEscapers
import com.google.gson.Gson
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/view")
class ViewController(private val gitHubUtil: GitHubUtil,
                     private val coverageStore: CoverageStore) {

    @RequestMapping(value = "/{git_type}/{owner}/{repo}/{commit}")
    fun view(@PathVariable("git_type") gitType: GitType,
             @PathVariable("owner") owner: String,
             @PathVariable("repo") repo: String,
             @PathVariable("commit") commit: String): String {
        val (baseBranch, _) = gitHubUtil.getPullRequestBaseAndNumber(owner, repo, commit)
        val (changedFiles, rawDiff) = gitHubUtil.getDiffOfCommit(owner, repo, baseBranch, commit)
        val diff = UrlEscapers.urlPathSegmentEscaper().escape(rawDiff)
        val (baseCoverages, currentCoverages) = coverageStore.getCoveragesForFiles("$owner/$repo", commit, changedFiles)
        val base = baseCoverages.mapValues { it.value.lineCoverageFile.map { it.toSimple() } }
        val current = currentCoverages.mapValues { it.value.lineCoverageFile.map { it.toSimple() } }
        return """
<!DOCTYPE html>
<html>
<head>
<link rel="stylesheet" type="text/css" href="https://cdnjs.cloudflare.com/ajax/libs/diff2html/2.4.0/diff2html.css">
</head>
<body>
<div id="diff2html"></div>
<script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/diff2html/2.4.0/diff2html.min.js"></script>
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
var current = ${Gson().toJson(current)}
</script>
</body>
</html>
        """.trimIndent()
    }
}