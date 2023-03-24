package com.github.reAsOn2010.mycov.store

import com.github.reAsOn2010.mycov.dao.CoverageRecordDao
import com.github.reAsOn2010.mycov.model.*
import org.dom4j.*
import org.springframework.stereotype.Component

@Component
class CoverageStore(private val coverageRecordDao: CoverageRecordDao) {

    fun get(repoName: String, gitType: GitType, reportType: ReportType, hash: String): CoverageRecord {
        return coverageRecordDao.findByRepoNameAndHashAndGitTypeAndReportType(repoName, hash, gitType, reportType) ?:
                throw CoverageOfCommitNotFound(hash)
    }

    fun getCoveragesForFiles(repoName: String,
                             gitType: GitType,
                             reportType: ReportType,
                             base: String,
                             head: String,
                             files: List<String>): Pair<Map<String, CoverageFile>, Map<String, CoverageFile>> {
        val baseRecord = coverageRecordDao.findByRepoNameAndHashAndGitTypeAndReportType(repoName, base, gitType, reportType)
                ?: throw CoverageOfCommitNotFound(base)
        val headRecord = coverageRecordDao.findByRepoNameAndHashAndGitTypeAndReportType(repoName, head, gitType, reportType)
                ?: throw CoverageOfCommitNotFound(head)

        return baseRecord.detail.coverageFileMap.mapNotNull { jacocoFile ->
            val changedFile = files.firstOrNull { it.contains(jacocoFile.key) }
            if (changedFile == null) {
                null
            } else {
                changedFile to jacocoFile.value
            }
        }.toMap() to headRecord.detail.coverageFileMap.mapNotNull { jacocoFile ->
            val changedFile = files.firstOrNull { it.contains(jacocoFile.key) }
            if (changedFile == null) {
                null
            } else {
                changedFile to jacocoFile.value
            }
        }.toMap()
    }

    fun diff(repoName: String, gitType: GitType, reportType: ReportType, base: String, head: String): CoverageDiffReport {
        val baseRecord = coverageRecordDao.findByRepoNameAndHashAndGitTypeAndReportType(repoName, base, gitType, reportType)
                ?: throw CoverageOfCommitNotFound(base)
        val baseOverview = baseRecord.detail.commitOverview
        val headRecord = coverageRecordDao.findByRepoNameAndHashAndGitTypeAndReportType(repoName, head, gitType, reportType)
                ?: throw CoverageOfCommitNotFound(head)
        val headOverview = headRecord.detail.commitOverview
        return CoverageDiffReport(
            coverages = DiffTuple(calculateCoverage(baseOverview.line),
                calculateCoverage(headOverview.line)),
            complexity = DiffTuple(calculateSum(baseOverview.complexity),
                calculateSum(headOverview.complexity)),
            files = DiffTuple(baseRecord.detail.coverageFileMap.size,
                headRecord.detail.coverageFileMap.size),
            lines = DiffTuple(calculateSum(baseOverview.line),
                calculateSum(headOverview.line)),
            branches = DiffTuple(calculateSum(baseOverview.branch),
                calculateSum(headOverview.branch)),
            hits = DiffTuple(baseOverview.line.covered,
                headOverview.line.covered),
            misses = DiffTuple(baseOverview.line.missed,
                headOverview.line.missed),
            partials = DiffTuple(baseOverview.line.partial,
                headOverview.line.partial)
        )
    }

    private fun calculateCoverage(line: OverviewTuple): Int {
        val result = line.covered.toFloat() * 10000 / (line.covered + line.partial + line.missed)
        return Math.round(result)
    }

    private fun calculateSum(line: OverviewTuple): Int {
        return line.covered + line.partial + line.missed
    }

    fun store(repoName: String,
              gitType: GitType,
              reportType: ReportType,
              commit: String,
              report: Document) {

        val root = report.rootElement
        /*
        root.elements("counter").map {
            val tuple = OverviewTuple(it.attributeValue("missed").toInt(),
                it.attributeValue("covered").toInt())
            when (it.attributeValue("type")) {
                "INSTRUCTION" -> overview.instruction = tuple
                "BRANCH" -> overview.branch = tuple
                "LINE" -> overview.line = tuple
                "COMPLEXITY" -> overview.complexity = tuple
                "METHOD" -> overview.method = tuple
                "CLASS" -> overview.class_ = tuple
            }
        }
        */
        val fileMaps = root.elements("package").map {
            iteratePackageElement(it)
        }
        val coverageFileMap = fileMaps.reduce { a, b -> a + b }
        val overview = fileMaps.flatMap { it.values.map { it.fileOverview } }.reduce { a, b -> a + b }
        // entry.commits[commit] = CoverageCommit(overview, coverageFileMap)

        val record = coverageRecordDao.findByRepoNameAndHashAndGitTypeAndReportType(
            repoName, commit, gitType, reportType)?.apply {
            detail = CoverageCommit(overview, coverageFileMap) } ?:
                CoverageRecord(
                    repoName = repoName,
                    hash = commit,
                    gitType = gitType,
                    reportType = reportType,
                    detail = CoverageCommit(overview, coverageFileMap)
                )
        coverageRecordDao.saveAndFlush(record)
    }

    fun iteratePackageElement(root: Element): Map<String, CoverageFile> {
        val packageName = root.attributeValue("name")
        return root.elements().mapNotNull {
            val fileName = it.attributeValue("name")
            val fullFileName = "$packageName/$fileName"
            when (it.qualifiedName) {
                "counter" -> null
                "class" -> null
                "sourcefile" -> {
                    val (overview, tuples) = iterateSourceFile(it)
                     fullFileName to CoverageFile(fullFileName, overview, tuples)
                }
                else -> throw RuntimeException("No such type of element.")
            }
        }.toMap()
    }

    fun iterateSourceFile(root: Element): Pair<CoverageOverview, List<CoverageFileTuple>> {
        val fileOverview = CoverageOverview()
        root.elements("counter").map {
            val tuple = OverviewTuple(it.attributeValue("missed").toInt(), 0,
                it.attributeValue("covered").toInt())
            when (it.attributeValue("type")) {
                "INSTRUCTION" -> fileOverview.instruction = tuple
                "BRANCH" -> fileOverview.branch = tuple
                "LINE" -> fileOverview.line = tuple
                "COMPLEXITY" -> fileOverview.complexity = tuple
                "METHOD" -> fileOverview.method = tuple
                "CLASS" -> fileOverview.class_ = tuple
            }
        }
        val tuples = root.elements("line").map{
            buildCoverageTuple(it)
        }
        // We consider partial covered as partial but not covered
        val covered = tuples.sumOf { if (it.missedBranches == 0 && it.missedInstructions == 0) 1 as Int else 0 as Int }
        val partial = tuples.sumOf {
            if (it.coveredBranches > 0 && it.missedBranches > 0) 1 as Int else 0 as Int
        }
        val missed = tuples.size - covered - partial // tuples.sumBy { if (it.coveredInstructions == 0 && it.coveredBranches == 0) 1 else 0 }
        assert(missed + partial + covered == tuples.size)
        fileOverview.line = OverviewTuple(missed, partial, covered)
        return fileOverview to tuples
    }

    fun buildCoverageTuple(element: Element): CoverageFileTuple {
        return CoverageFileTuple(lineNumber = element.attributeValue("nr").toInt(),
            missedInstructions = element.attributeValue("mi").toInt(),
            coveredInstructions = element.attributeValue("ci").toInt(),
            missedBranches = element.attributeValue("mb").toInt(),
            coveredBranches = element.attributeValue("cb").toInt())
    }

}