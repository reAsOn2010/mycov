package com.github.reAsOn2010.mycov.model

data class CoverageDiffReport (
    val coverages: DiffTuple,
    val complexity: DiffTuple,
    val files: DiffTuple,
    val lines: DiffTuple,
    val branches: DiffTuple,
    val hits: DiffTuple,
    val misses : DiffTuple,
    val partials: DiffTuple
)