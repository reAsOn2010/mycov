package com.github.reAsOn2010.mycov.model

import com.github.reAsOn2010.mycov.model.base.CoverageCommitConverter
import org.hibernate.annotations.*
import java.time.Instant
import javax.persistence.*
import javax.persistence.Entity
import javax.persistence.Index
import javax.persistence.Table

@Entity
@DynamicUpdate
@Table(indexes = [
    Index(name = "idx", columnList = "repoName,hash,gitType,reportType")
])
class CoverageRecord(

    @get:Column(nullable = false)
    var repoName: String,

    @get:Column(nullable = false)
    var hash: String,

    @get:Column(nullable = false)
    @get:Enumerated(EnumType.ORDINAL)
    var gitType: GitType,

    @get:Column(nullable = false)
    @get:Enumerated(EnumType.ORDINAL)
    var reportType: ReportType,

    @get:Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    @get:Convert(converter = CoverageCommitConverter::class)
    var detail: CoverageCommit
) {

    @Transient
    @get:Id
    @get:GeneratedValue
    @set:Deprecated(message = "Never change", level = DeprecationLevel.HIDDEN)
    var id: Long = 0

    @get:CreationTimestamp
    @get:Column(nullable = false, insertable = false, updatable = false,
        columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @set:Deprecated(message = "Never change", level = DeprecationLevel.HIDDEN)
    var createAt: Instant = Instant.EPOCH
}

