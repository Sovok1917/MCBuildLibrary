package sovok.mcbuildlibrary.dto;

import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaskStatusDto(
        String taskId,
        TaskState status,
        String errorMessage,
        String filePath
) {

    public static TaskStatusDto failed(String taskId, String errorMessage) {
        return new TaskStatusDto(taskId, TaskState.FAILED, errorMessage, null);
    }


    public static TaskStatusDto completed(String taskId, String filePath) {
        return new TaskStatusDto(taskId, TaskState.COMPLETED, null, filePath);
    }


    public static TaskStatusDto pending(String taskId) {
        return new TaskStatusDto(taskId, TaskState.PENDING, null, null);
    }
}