package com.github.reAsOn2010.mycov

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class MyCovTestApplication

fun main(args: Array<String>) {
    SpringApplication.run(MyCovTestApplication::class.java, *args)
}