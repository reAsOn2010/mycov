package com.github.reAsOn2010.mycov.store

import com.github.reAsOn2010.mycov.model.*
import org.dom4j.*
import org.springframework.stereotype.Component

@Component
class CoverageStore {

    private val maps: MutableMap<String, CoverageRepo> = mutableMapOf()

    fun getCoveragesForFiles(repoName: String,
                             base: String,
                             head: String,
                             files: List<String>): Pair<Map<String, CoverageFile>, Map<String, CoverageFile>> {
        val entry = maps[repoName] ?: throw RuntimeException("Coverage entry not found.")
        return entry.commits[base]!!.coverageFileMap.mapNotNull { jacocoFile ->
            val changedFile = files.firstOrNull { it.contains(jacocoFile.key) }
            if (changedFile == null) {
                null
            } else {
                changedFile to jacocoFile.value
            }
        }.toMap() to entry.commits[head]!!.coverageFileMap.mapNotNull { jacocoFile ->
            val changedFile = files.firstOrNull { it.contains(jacocoFile.key) }
            if (changedFile == null) {
                null
            } else {
                changedFile to jacocoFile.value
            }
        }.toMap()
    }

    fun diff(repoName: String, base: String, head: String): CoverageDiffReport {
        val entry = maps[repoName] ?: throw RuntimeException("Coverage entry not found.")
        val baseCommit = entry.commits[base]!!
        val baseOverview = baseCommit.commitOverview
        val currentCommit = entry.commits[head]!!
        val currentOverview = currentCommit.commitOverview
        return CoverageDiffReport(
            coverages = DiffTuple(calculateCoverage(baseOverview.line),
                calculateCoverage(currentOverview.line)),
            complexity = DiffTuple(calculateSum(baseOverview.complexity),
                calculateSum(currentOverview.complexity)),
            files = DiffTuple(baseCommit.coverageFileMap.size,
                currentCommit.coverageFileMap.size),
            lines = DiffTuple(calculateSum(baseOverview.line),
                calculateSum(currentOverview.line)),
            branches = DiffTuple(calculateSum(baseOverview.branch),
                calculateSum(currentOverview.branch)),
            hits = DiffTuple(baseOverview.line.covered,
                currentOverview.line.covered),
            misses = DiffTuple(baseOverview.line.missed,
                currentOverview.line.missed),
            partials = DiffTuple(baseOverview.line.partial,
                currentOverview.line.partial)
        )
    }

    private fun calculateCoverage(line: OverviewTuple): Int {
        val result = line.covered.toFloat() * 10000 / (line.covered + line.partial + line.missed)
        return Math.round(result)
    }

    private fun calculateSum(line: OverviewTuple): Int {
        return line.covered + line.partial + line.missed
    }

    fun store(gitType: GitType,
              repoName: String,
              reportType: ReportType,
              commit: String,
              report: Document) {
        val entry = maps[repoName] ?: {
            val tmp = CoverageRepo(repoName, gitType, reportType)
            maps[repoName] = tmp
            tmp
        }()
        if (entry.gitType != gitType || entry.reportType != reportType) {
            throw RuntimeException("Unmatched git type or report type.")
        }
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
        entry.commits[commit] = CoverageCommit(overview, coverageFileMap)
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
        val covered = tuples.sumBy { if (it.missedBranches == 0 && it.missedInstructions == 0) 1 else 0}
        val partial = tuples.sumBy {
            if (it.coveredBranches > 0 && it.missedBranches > 0) 1 else 0
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