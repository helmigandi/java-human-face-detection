package com.face_detectin.face_detection;

public class AttendanceRequest {
    private String username;
    private String departmentName;
    private String employeeId;

    public AttendanceRequest() {
    }

    public AttendanceRequest(String username, String departmentName, String employeeId) {
        this.username = username;
        this.departmentName = departmentName;
        this.employeeId = employeeId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
}
