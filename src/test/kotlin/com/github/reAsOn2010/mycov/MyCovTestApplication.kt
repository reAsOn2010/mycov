package com.github.reAsOn2010.mycov

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.*
import org.springframework.core.task.SyncTaskExecutor
import org.springframework.core.task.TaskExecutor

@SpringBootApplication
class MyCovTestApplication {

    @Bean
    @Primary
    fun taskExecutor(): TaskExecutor {
        return SyncTaskExecutor()
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(MyCovTestApplication::class.java, *args)
}