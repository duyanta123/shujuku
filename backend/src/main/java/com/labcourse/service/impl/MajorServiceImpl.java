package com.labcourse.service.impl;

import com.labcourse.entity.Major;
import com.labcourse.repository.MajorRepository;
import com.labcourse.repository.StudentRepository;
import com.labcourse.service.MajorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class MajorServiceImpl implements MajorService {

    private static final Logger logger = LoggerFactory.getLogger(MajorServiceImpl.class);
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "name", "collegeId", "status", "updatedAt");
    private static final Set<String> ALLOWED_STATUS = Set.of("ACTIVE", "INACTIVE", "all");

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Override
    public Map<String, Object> list(String name, Long collegeId, String status, int page, int size, String sortBy, String sortDir) {
        validateListParams(collegeId, status, page, size, sortBy, sortDir);
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        boolean hasName = name != null && !name.isEmpty();
        boolean hasCollegeId = collegeId != null;
        boolean hasStatus = status != null && !status.equals("all");

        Page<Major> majorPage;
        if (hasName && hasCollegeId) {
            majorPage = majorRepository.findByNameContainingAndCollegeId(name, collegeId, pageRequest);
        } else if (hasName) {
            majorPage = majorRepository.findByNameContaining(name, pageRequest);
        } else if (hasCollegeId) {
            if (hasStatus) {
                List<Major> filtered = majorRepository.findByCollegeId(collegeId).stream()
                        .filter(m -> status.equals(m.getStatus()))
                        .collect(Collectors.toList());
                int start = (int) pageRequest.getOffset();
                int end = Math.min((start + pageRequest.getPageSize()), filtered.size());
                Map<String, Object> result = new HashMap<>();
                if (start >= filtered.size()) {
                    result.put("content", List.of());
                } else {
                    result.put("content", filtered.subList(start, end));
                }
                result.put("totalElements", (long) filtered.size());
                result.put("totalPages", Math.max(1, (int) Math.ceil((double) filtered.size() / size)));
                result.put("currentPage", page);
                return result;
            } else {
                majorPage = majorRepository.findByCollegeId(collegeId, pageRequest);
            }
        } else {
            majorPage = majorRepository.findAll(pageRequest);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("content", majorPage.getContent());
        result.put("totalElements", majorPage.getTotalElements());
        result.put("totalPages", majorPage.getTotalPages());
        result.put("currentPage", majorPage.getNumber());
        return result;
    }

    private void validateListParams(Long collegeId, String status, int page, int size, String sortBy, String sortDir) {
        if (collegeId != null && collegeId <= 0) {
            throw new IllegalArgumentException("collegeId 参数非法");
        }
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
    public List<Major> listByCollegeId(Long collegeId) {
        return majorRepository.findByCollegeIdAndStatus(collegeId, "ACTIVE");
    }

    @Override
    public Major getById(Long id) {
        return majorRepository.findById(id).orElse(null);
    }

    @Override
    public boolean save(Major major) {
        if (majorRepository.findByCollegeIdAndName(major.getCollegeId(), major.getName()).isPresent()) {
            return false;
        }
        majorRepository.save(major);
        return true;
    }

    @Override
    public boolean update(Major major) {
        Optional<Major> existingOpt = majorRepository.findById(major.getId());
        if (existingOpt.isPresent()) {
            Optional<Major> duplicate = majorRepository.findByCollegeIdAndName(major.getCollegeId(), major.getName());
            if (duplicate.isPresent() && !duplicate.get().getId().equals(major.getId())) {
                return false;
            }
            Major existing = existingOpt.get();
            if (major.getName() != null && !major.getName().isEmpty()) {
                existing.setName(major.getName());
            }
            if (major.getCollegeId() != null) {
                existing.setCollegeId(major.getCollegeId());
            }
            if (major.getStatus() != null) {
                existing.setStatus(major.getStatus());
            }
            majorRepository.save(existing);
            logger.info("管理员操作 - 更新专业: {} (ID:{})", existing.getName(), existing.getId());
            return true;
        }
        return false;
    }

    @Override
    public Map<String, Object> delete(Long id) {
        Map<String, Object> result = new HashMap<>();
        long studentCount = studentRepository.countByMajorId(id);

        if (studentCount > 0) {
            result.put("success", false);
            result.put("message", String.format("该专业下存在 %d 名学生，无法删除", studentCount));
            return result;
        }

        Optional<Major> majorOpt = majorRepository.findById(id);
        if (majorOpt.isPresent()) {
            Major major = majorOpt.get();
            major.setStatus("INACTIVE");
            majorRepository.save(major);
            logger.info("管理员操作 - 停用专业: {} (ID:{})", major.getName(), id);
            result.put("success", true);
            result.put("message", "专业已停用");
        } else {
            result.put("success", false);
            result.put("message", "专业不存在");
        }
        return result;
    }
}
