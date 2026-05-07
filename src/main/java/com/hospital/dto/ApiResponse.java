package com.hospital.dto;

/** Generic success/failure response envelope. */
public class ApiResponse {
    private boolean success;
    private String message;
    private Object data;

    public ApiResponse() {}

    public ApiResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public static ApiResponse ok(String message, Object data) {
        return new ApiResponse(true, message, data);
    }

    public static ApiResponse ok(String message) {
        return new ApiResponse(true, message, null);
    }
}
