package com.startrails.core.model

/** 任务状态机：DRAFT → VALIDATING → QUEUED → RUNNING → SAVING → SUCCEEDED。 */
enum class JobStatus {
    DRAFT,
    VALIDATING,
    QUEUED,
    RUNNING,
    SAVING,
    SUCCEEDED,
    CANCELLING,
    CANCELLED,
    FAILED;

    val isTerminal: Boolean
        get() = this == SUCCEEDED || this == CANCELLED || this == FAILED

    /**
     * 是否占用唯一的叠加执行通道。
     * VALIDATING 只是“草稿待确认”，不占用通道，否则会让草稿长期阻塞重试与调度。
     */
    val isActive: Boolean
        get() = this == QUEUED || this == RUNNING || this == SAVING || this == CANCELLING
}
