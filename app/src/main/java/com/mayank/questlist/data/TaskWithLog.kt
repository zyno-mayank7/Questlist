package com.mayank.questlist.data

data class TaskWithLog(
    val task: Task,
    val log: TaskLog?
) {
    val status: String
        get() = log?.status ?: TaskLog.STATUS_PENDING

    val isCompleted: Boolean
        get() = status == TaskLog.STATUS_COMPLETED

    val isFailed: Boolean
        get() = status == TaskLog.STATUS_FAILED
}