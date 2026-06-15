package com.labcourse.service.impl;

import com.labcourse.entity.College;
import com.labcourse.repository.CollegeRepository;
import com.labcourse.repository.MajorRepository;
import com.labcourse.repository.StudentRepository;
import com.labcourse.repository.TeacherRepository;
import com.labcourse.service.CollegeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class CollegeServiceImpl implements CollegeService {

    private static final Logger logger = LoggerFactory.getLogger(CollegeServiceImpl.class);

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Override
    public Map<String, Object> list(String name, String status, int page, int size, String sortBy, String sortDir) {
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
        long majorCount = majorRepository.countByCollegeId(id);
        long studentCount = studentRepository.countByCollegeId(id);
        long teacherCount = teacherRepository.countByCollegeId(id);

        if (majorCount > 0 || studentCount > 0 || teacherCount > 0) {
            result.put("success", false);
            result.put("message", String.format("该学院下存在 %d 个专业、%d 名学生、%d 名教师，无法删除",
                    majorCount, studentCount, teacherCount));
            return result;
        }

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