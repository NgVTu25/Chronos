package com.job.distributed_job_scheduler.common;

public enum ExecutionStatus {
    PENDING,        // Đã được tạo, nằm trong Queue chờ Worker nhận
    RUNNING,        // Đang được Worker xử lý
    SUCCESS,        // Thực thi thành công
    FAILED,         // Thực thi thất bại (sẽ chờ retry nếu cấu hình cho phép)
    RETRYING,       // Đang nằm trong Retry Queue chờ chạy lại
    DEAD_LETTER;    // Đã thử lại quá số lần tối đa (Max Retries) và bị đẩy vào DLQ

    /**
     * Kiểm tra xem execution đã kết thúc hoàn toàn vòng đời của nó hay chưa.
     */
    public boolean isFinal() {
        return this == SUCCESS || this == DEAD_LETTER || this == FAILED;
    }
}