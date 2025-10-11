package com.foreach.TodoCloud

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TodoCloudApplication

fun main(args: Array<String>) {
	runApplication<TodoCloudApplication>(*args)
}
