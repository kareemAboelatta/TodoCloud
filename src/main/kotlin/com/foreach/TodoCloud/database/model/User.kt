package com.foreach.TodoCloud.database.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document


@Document("users")
data class User(
    val name: String,
    val email: String,
    val hashedPassword: String,
    @Id val id: ObjectId = ObjectId()
)
