package com.labcourse.service.impl;

import com.labcourse.entity.Admin;
import com.labcourse.repository.AdminRepository;
import com.labcourse.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Override
    public Admin login(String username, String password) {
        return adminRepository.findByUsernameAndPassword(username, password).orElse(null);
    }

    @Override
    public List<Admin> list() {
        return adminRepository.findAll();
    }

    @Override
    public boolean save(Admin admin) {
        adminRepository.save(admin);
        return true;
    }

    @Override
    public boolean updateById(Admin admin) {
        adminRepository.save(admin);
        return true;
    }

    @Override
    public boolean removeById(Long id) {
        adminRepository.deleteById(id);
        return true;
    }
}
