package com.github.reAsOn2010.mycov.model

data class OverviewTuple (
    var missed: Int = 0,
    var partial: Int = 0,
    var covered: Int = 0
) {
    operator fun plus (another: OverviewTuple): OverviewTuple {
        return OverviewTuple(this.missed + another.missed,
            this.partial + another.partial,
            this.covered + another.covered)
    }
}