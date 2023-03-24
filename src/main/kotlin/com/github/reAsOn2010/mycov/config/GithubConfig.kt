package com.github.reAsOn2010.mycov.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "github")
class GithubConfig {
    var baseUrl = "https://api.github.com"
    var token = ""
}