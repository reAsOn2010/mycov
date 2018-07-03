package com.github.reAsOn2010.mycov.model

data class CoverageFileTuple (
    val lineNumber: Int,
    val missedInstructions: Int,
    val coveredInstructions: Int,
    val missedBranches: Int,
    val coveredBranches: Int
) {
    fun toSimple(): SimpleTuple {
        return SimpleTuple(
            lineNumber,
            missedInstructions,
            coveredInstructions,
            missedBranches,
            coveredBranches
        )
    }
}