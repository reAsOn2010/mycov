package com.github.reAsOn2010.mycov.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "gitea")
class GiteaConfig {
    var baseUrl = "https://gitea.your.host/api/v1/"
    var token = ""
}