package com.github.reAsOn2010.mycov.model

data class CoverageOverview (
    var instruction: OverviewTuple = OverviewTuple(),
    var branch: OverviewTuple = OverviewTuple(),
    var line: OverviewTuple = OverviewTuple(),
    var complexity: OverviewTuple = OverviewTuple(),
    var method: OverviewTuple = OverviewTuple(),
    var class_: OverviewTuple = OverviewTuple()
) {
    operator fun plus (another: CoverageOverview): CoverageOverview {
        return CoverageOverview(this.instruction + another.instruction,
            this.branch + another.branch,
            this.line + another.line,
            this.complexity + another.complexity,
            this.method + another.method,
            this.class_ + another.class_)
    }
}