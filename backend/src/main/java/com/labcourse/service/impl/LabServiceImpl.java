package com.labcourse.service.impl;

import com.labcourse.entity.College;
import com.labcourse.entity.Lab;
import com.labcourse.repository.CollegeRepository;
import com.labcourse.repository.LabRepository;
import com.labcourse.service.LabService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class LabServiceImpl implements LabService {

    @Autowired
    private LabRepository labRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Override
    public List<Lab> list() {
        return labRepository.findAll();
    }

    @Override
    public boolean save(Lab lab) {
        validateLab(lab);
        validateCollege(lab.getCollegeId());
        labRepository.save(lab);
        return true;
    }

    @Override
    public boolean updateById(Lab lab) {
        Optional<Lab> existingOpt = labRepository.findById(lab.getId());
        if (existingOpt.isPresent()) {
            Lab existing = existingOpt.get();
            if (lab.getCapacity() != null) {
                validateLab(lab);
            }
            if (lab.getCollegeId() != null) {
                validateCollege(lab.getCollegeId());
            }
            if (lab.getLabName() != null) { existing.setLabName(lab.getLabName()); }
            if (lab.getLocation() != null) { existing.setLocation(lab.getLocation()); }
            if (lab.getCapacity() != null) { existing.setCapacity(lab.getCapacity()); }
            if (lab.getCollegeId() != null && !lab.getCollegeId().equals(existing.getCollegeId())) {
                existing.setCollegeId(lab.getCollegeId());
            }
            labRepository.save(existing);
            return true;
        }
        return false;
    }

    private void validateLab(Lab lab) {
        if (lab.getCapacity() == null || lab.getCapacity() < 1 || lab.getCapacity() > 200) {
            throw new IllegalArgumentException("实验室容量范围为1-200");
        }
    }

    private void validateCollege(Long collegeId) {
        if (collegeId != null && collegeRepository != null && !collegeRepository.existsById(collegeId)) {
            throw new IllegalArgumentException("学院不存在");
        }
    }

    @Override
    public boolean removeById(Long id) {
        labRepository.deleteById(id);
        return true;
    }
}
