package sovok.mcbuildlibrary.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

// Include non-null fields only in JSON output
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaskStatusDto(
        String taskId,
        TaskState status,
        String errorMessage, // Only present if status is FAILED
        String filePath      // Only present if status is COMPLETED (internal use mostly)
) {
    // Convenience constructor for PENDING
    public TaskStatusDto(String taskId, TaskState status) {
        this(taskId, status, null, null);
    }

    // Convenience factory for FAILED status
    public static TaskStatusDto failed(String taskId, String errorMessage) {
        return new TaskStatusDto(taskId, TaskState.FAILED, errorMessage, null);
    }

    // Convenience factory for COMPLETED status
    public static TaskStatusDto completed(String taskId, String filePath) {
        return new TaskStatusDto(taskId, TaskState.COMPLETED, null, filePath);
    }

    // Convenience factory for PENDING status
    public static TaskStatusDto pending(String taskId) {
        return new TaskStatusDto(taskId, TaskState.PENDING, null, null);
    }
}