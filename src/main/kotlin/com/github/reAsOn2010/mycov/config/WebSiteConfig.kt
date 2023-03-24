package com.github.reAsOn2010.mycov.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "website")
class WebSiteConfig {
    var url: String = ""
}