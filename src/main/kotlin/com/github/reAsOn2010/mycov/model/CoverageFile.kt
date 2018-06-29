package com.github.reAsOn2010.mycov.model

data class CoverageFile (
    val fileName: String,
    val fileOverview: CoverageOverview,
    val lineCoverageFile: List<CoverageFileTuple>
)