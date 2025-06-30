package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable


@Serializable
data class Task(val id: Int, val content: String, val isDone: Boolean)

@Serializable
data class TaskRequest(val content: String, val isDone: Boolean)


object TaskRepository {
    private val tasks = mutableListOf<Task>(
        Task(id = 1, content = "Learn Ktor", isDone = true),
        Task(id = 2, content = "Build a REST API", isDone = false),
        Task(id = 3, content = "Write Unit Tests", isDone = false)
    )

    private var nextId = 4 // เริ่มจาก 4 เพราะมี task 3 ตัวแล้ว

    // ดึงข้อมูล tasks ทั้งหมด
    fun getAll(): List<Task> = tasks.toList()

    // ดึงข้อมูล task ตาม id
    fun getById(id: Int): Task? = tasks.find { it.id == id }

    // เพิ่มข้อมูล task เข้าไป
    fun add(taskRequest: TaskRequest): Task {
        val newTask = Task(
            id = nextId++,
            content = taskRequest.content,
            isDone = taskRequest.isDone
        )
        tasks.add(newTask)
        return newTask
    }

    // update ข้อมูล task ตาม id
    fun update(id: Int, taskRequest: TaskRequest): Task? {
        val index = tasks.indexOfFirst { it.id == id }
        return if (index != -1) {
            val updatedTask = Task(id, taskRequest.content, taskRequest.isDone)
            tasks[index] = updatedTask
            updatedTask
        } else {
            null
        }
    }

    // ลบข้อมูล task จาก id
    fun delete(id: Int): Boolean {
        return tasks.removeIf { it.id == id }
    }
}


fun Application.configureRouting() {
    routing {
        // GET /tasks: คืนค่า task ทั้งหมด
        get("/tasks") {
            val tasks = TaskRepository.getAll()
            call.respond(HttpStatusCode.OK, tasks)
        }

        // GET /tasks/{id}: ค้นหาและคืนค่า task เพียงตัวเดียว
        get("/tasks/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@get
            }

            val task = TaskRepository.getById(id)
            if (task != null) {
                call.respond(HttpStatusCode.OK, task)
            } else {
                call.respond(HttpStatusCode.NotFound, "Task not found")
            }
        }

        // POST /tasks: เพิ่ม task ใหม่
        post("/tasks") {
            try {
                val taskRequest = call.receive<TaskRequest>()
                val newTask = TaskRepository.add(taskRequest)
                call.respond(HttpStatusCode.Created, newTask)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid request body")
            }
        }

        // PUT /tasks/{id}: อัปเดต task
        put("/tasks/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@put
            }

            try {
                val taskRequest = call.receive<TaskRequest>()
                val updatedTask = TaskRepository.update(id, taskRequest)
                if (updatedTask != null) {
                    call.respond(HttpStatusCode.OK, updatedTask)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Task not found")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid request body")
            }
        }

        // DELETE /tasks/{id}: ลบ task
        delete("/tasks/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@delete
            }

            val deleted = TaskRepository.delete(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, "Task not found")
            }
        }
    }
}