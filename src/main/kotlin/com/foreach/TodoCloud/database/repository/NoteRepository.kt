package com.foreach.TodoCloud.database.repository

import com.foreach.TodoCloud.database.model.Note
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository


interface NoteRepository: MongoRepository<Note, ObjectId> {
    fun findByOwnerId(ownerId: ObjectId): List<Note>
}