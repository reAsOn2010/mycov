package com.github.reAsOn2010.mycov.model

class GithubAPICallError(private val url: String): RuntimeException() {
    override val message: String
        get() = "Github api call of '$url' failed."
}

class GiteaAPICallError(private val url: String): RuntimeException() {
    override val message: String
        get() = "Gitea api call of '$url' failed."
}

class PullRequestNotFound(private val sha: String): RuntimeException() {
    override val message: String
        get() = "Pull request with head of '$sha' not found."
}

class CoverageOfCommitNotFound(private val sha: String): RuntimeException() {
    override val message: String
        get() = "Coverage of commit '$sha' not found."
}