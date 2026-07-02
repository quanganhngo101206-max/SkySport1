package com.example.skysport1.exception;

public class ResourceNotFoundException extends AppException {

    public ResourceNotFoundException(String resource, String id) {
        super("Không tìm thấy " + resource + " với id: " + id);
    }

    // Thêm constructor cho message tự do
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
