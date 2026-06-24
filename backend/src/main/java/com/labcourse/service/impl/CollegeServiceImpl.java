package com.labcourse.service.impl;

import com.labcourse.entity.College;
import com.labcourse.repository.CollegeRepository;
import com.labcourse.service.CollegeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class CollegeServiceImpl implements CollegeService {

    private static final Logger logger = LoggerFactory.getLogger(CollegeServiceImpl.class);
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "name", "status", "createdAt", "updatedAt");
    private static final Set<String> ALLOWED_STATUS = Set.of("ACTIVE", "INACTIVE", "all");

    @Autowired
    private CollegeRepository collegeRepository;

    @Override
    public Map<String, Object> list(String name, String status, int page, int size, String sortBy, String sortDir) {
        validateListParams(status, page, size, sortBy, sortDir);
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<College> collegePage;
        boolean hasName = name != null && !name.isEmpty();
        boolean hasStatus = status != null && !status.equals("all");

        if (hasName && hasStatus) {
            collegePage = collegeRepository.findByNameContainingAndStatus(name, status, pageRequest);
        } else if (hasName) {
            collegePage = collegeRepository.findByNameContaining(name, pageRequest);
        } else if (hasStatus) {
            collegePage = collegeRepository.findByStatus(status, pageRequest);
        } else {
            collegePage = collegeRepository.findAll(pageRequest);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("content", collegePage.getContent());
        result.put("totalElements", collegePage.getTotalElements());
        result.put("totalPages", collegePage.getTotalPages());
        result.put("currentPage", collegePage.getNumber());
        return result;
    }

    private void validateListParams(String status, int page, int size, String sortBy, String sortDir) {
        if (page < 0) {
            throw new IllegalArgumentException("page 不能小于 0");
        }
        if (size < 1 || size > 1000) {
            throw new IllegalArgumentException("size 范围为 1-1000");
        }
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("sortBy 参数非法");
        }
        if (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir)) {
            throw new IllegalArgumentException("sortDir 参数非法");
        }
        if (status != null && !ALLOWED_STATUS.contains(status)) {
            throw new IllegalArgumentException("status 参数非法");
        }
    }

    @Override
    public College getById(Long id) {
        return collegeRepository.findById(id).orElse(null);
    }

    @Override
    public boolean save(College college) {
        if (collegeRepository.findByName(college.getName()).isPresent()) {
            return false;
        }
        collegeRepository.save(college);
        return true;
    }

    @Override
    public boolean update(College college) {
        Optional<College> existingOpt = collegeRepository.findById(college.getId());
        if (existingOpt.isPresent()) {
            Optional<College> duplicate = collegeRepository.findByName(college.getName());
            if (duplicate.isPresent() && !duplicate.get().getId().equals(college.getId())) {
                return false;
            }
            College existing = existingOpt.get();
            if (college.getName() != null && !college.getName().isEmpty()) {
                existing.setName(college.getName());
            }
            if (college.getStatus() != null) {
                existing.setStatus(college.getStatus());
            }
            collegeRepository.save(existing);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, Object> delete(Long id) {
        Map<String, Object> result = new HashMap<>();

        Optional<College> collegeOpt = collegeRepository.findById(id);
        if (collegeOpt.isPresent()) {
            College college = collegeOpt.get();
            college.setStatus("INACTIVE");
            collegeRepository.save(college);
            logger.info("管理员操作 - 停用学院: {} (ID:{})", college.getName(), id);
            result.put("success", true);
            result.put("message", "学院已停用");
        } else {
            result.put("success", false);
            result.put("message", "学院不存在");
        }
        return result;
    }
}
