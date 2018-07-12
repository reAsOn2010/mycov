package com.github.reAsOn2010.mycov

import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = [MyCovTestApplication::class])
@Transactional
abstract class MyCovTest : TestConstant {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

}