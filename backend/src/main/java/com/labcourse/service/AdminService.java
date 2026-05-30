package com.labcourse.service;

import com.labcourse.entity.Admin;

import java.util.List;

public interface AdminService {
    Admin login(String username, String password);
    List<Admin> list();
    boolean save(Admin admin);
    boolean updateById(Admin admin);
    boolean removeById(Long id);
}
