package com.github.reAsOn2010.mycov.config

import com.github.reAsOn2010.mycov.controller.ReportController.*
import org.dom4j.Document
import org.dom4j.io.SAXReader
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.format.support.FormattingConversionService
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport
import java.io.InputStream

@Configuration
class WebMvcConfig: WebMvcConfigurationSupport() {

    class GitTypeConverter: Converter<String, GitType> {
        override fun convert(source: String): GitType {
            return GitType.valueOf(source.toUpperCase())
        }
    }

    class ReportTypeConverter: Converter<String, ReportType> {
        override fun convert(source: String): ReportType {
            return ReportType.valueOf(source.toUpperCase())
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