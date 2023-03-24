package com.github.reAsOn2010.mycov.config

import com.github.reAsOn2010.mycov.model.*
import org.dom4j.Document
import org.dom4j.io.SAXReader
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.format.support.FormattingConversionService
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport
import java.io.InputStream
import java.util.*

@Configuration
class WebMvcConfig: WebMvcConfigurationSupport() {

    class GitTypeConverter: Converter<String, GitType> {
        override fun convert(source: String): GitType {
            return GitType.valueOf(source.uppercase())
        }
    }

    class ReportTypeConverter: Converter<String, ReportType> {
        override fun convert(source: String): ReportType {
            return ReportType.valueOf(source.uppercase())
        }
    }

    class XMLConverter: Converter<InputStream, Document> {
        override fun convert(source: InputStream): Document {
            val reader = SAXReader()
            return reader.read(source)
        }
    }

    override fun mvcConversionService(): FormattingConversionService {
        val f = super.mvcConversionService()
        f.addConverter(GitTypeConverter())
        f.addConverter(ReportTypeConverter())
        f.addConverter(XMLConverter())
        return f
    }
}