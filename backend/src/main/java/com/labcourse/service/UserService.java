package com.labcourse.service;

import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    String saveAvatar(MultipartFile file);
    Map<String, Object> getProfile();
    boolean changePassword(String oldPassword, String newPassword);
}