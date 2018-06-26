package com.github.reAsOn2010.mycov

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.support.SpringBootServletInitializer

@SpringBootApplication
class MyCovApplication : SpringBootServletInitializer() {

    override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
        return application.sources(MyCovApplication::class.java)
    }

}

fun main(args: Array<String>) {
    SpringApplication.run(MyCovApplication::class.java, *args)
}