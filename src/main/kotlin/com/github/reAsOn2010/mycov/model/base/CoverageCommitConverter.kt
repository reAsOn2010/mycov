package com.github.reAsOn2010.mycov.model.base

import com.github.reAsOn2010.mycov.model.CoverageCommit
import com.google.gson.Gson
import javax.persistence.AttributeConverter

class CoverageCommitConverter: AttributeConverter<CoverageCommit, String> {
    override fun convertToDatabaseColumn(attribute: CoverageCommit): String {
        return Gson().toJson(attribute)
    }

    override fun convertToEntityAttribute(dbData: String): CoverageCommit {
        return Gson().fromJson(dbData, CoverageCommit::class.java)
    }
}