package com.github.reAsOn2010.mycov.model

data class CoverageCommit (
    val commitOverview: CoverageOverview = CoverageOverview(),
    val coverageFileMap: Map<String, CoverageFile> = emptyMap()
)