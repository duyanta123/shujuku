package com.labcourse.service;

import com.labcourse.entity.Attendance;
import com.labcourse.entity.AttendanceStatus;
import com.labcourse.entity.Course;
import com.labcourse.entity.Student;
import com.labcourse.repository.AttendanceRepository;
import com.labcourse.repository.CourseRepository;
import com.labcourse.repository.SelectionRepository;
import com.labcourse.repository.StudentRepository;
import com.labcourse.service.impl.AttendanceServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttendanceServiceImplTest {

    @InjectMocks
    private AttendanceServiceImpl service;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SelectionRepository selectionRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpClock() {
        useClockAt(LocalDateTime.of(2026, 6, 22, 7, 55));
    }

    @Test
    @DisplayName("checkIn: course not found")
    void checkInCourseNotFound() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());
        Map<String, Object> r = service.checkIn(1L, 99L);
        assertFalse((Boolean) r.get("success"));
    }

    @Test
    @DisplayName("checkIn: student not found")
    void checkInStudentNotFound() {
        Course c = course(10L, "Test", 1L);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(c));
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());
        Map<String, Object> r = service.checkIn(99L, 10L);
        assertFalse((Boolean) r.get("success"));
    }

    @Test
    @DisplayName("checkIn: duplicate reject")
    void checkInDuplicate() {
        Course c = course(10L, "Test", 1L);
        c.setCourseTime("周一 1-2节");
        Student s = student(1L, "Zhang");
        when(courseRepository.findById(10L)).thenReturn(Optional.of(c));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));

        Map<String, Object> procResult = new HashMap<>();
        procResult.put("p_status", "DUPLICATE");
        procResult.put("p_message", "今日已签到");

        try (MockedConstruction<SimpleJdbcCall> mocked = mockConstruction(SimpleJdbcCall.class,
                (mock, ctx) -> {
                    when(mock.withProcedureName(anyString())).thenReturn(mock);
                    when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                    when(mock.execute(any(Map.class))).thenReturn(procResult);
                })) {
            Map<String, Object> r = service.checkIn(1L, 10L);
            assertFalse((Boolean) r.get("success"));
        }
    }

    @Test
    @DisplayName("checkIn: courseTime not set")
    void checkInNoCourseTime() {
        Course c = course(10L, "Test", 1L);
        Student s = student(1L, "Zhang");
        when(courseRepository.findById(10L)).thenReturn(Optional.of(c));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        Map<String, Object> r = service.checkIn(1L, 10L);
        assertFalse((Boolean) r.get("success"));
    }

    @Test
    @DisplayName("checkIn: success")
    void checkInSuccess() {
        Course c = course(10L, "Test", 1L);
        c.setCourseTime("周一 1-2节");
        Student s = student(1L, "Zhang");
        when(courseRepository.findById(10L)).thenReturn(Optional.of(c));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));

        Map<String, Object> procResult = new HashMap<>();
        procResult.put("p_status", "OK");
        procResult.put("p_message", "签到成功");

        try (MockedConstruction<SimpleJdbcCall> mocked = mockConstruction(SimpleJdbcCall.class,
                (mock, ctx) -> {
                    when(mock.withProcedureName(anyString())).thenReturn(mock);
                    when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                    when(mock.execute(any(Map.class))).thenReturn(procResult);
                })) {
            Map<String, Object> r = service.checkIn(1L, 10L);
            assertTrue((Boolean) r.get("success"));
            verify(attendanceRepository).saveAndFlush(any(Attendance.class));
        }
    }

    @Test
    @DisplayName("checkIn: too early reject")
    void checkInTooEarlyRejectsWithoutSaving() {
        useClockAt(LocalDateTime.of(2026, 6, 22, 7, 0));
        Course c = course(10L, "Test", 1L);
        c.setCourseTime("周一 1-2节");
        Student s = student(1L, "Zhang");
        when(courseRepository.findById(10L)).thenReturn(Optional.of(c));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));

        try (MockedConstruction<SimpleJdbcCall> mocked = mockConstruction(SimpleJdbcCall.class)) {
            Map<String, Object> r = service.checkIn(1L, 10L);
            assertFalse((Boolean) r.get("success"));
            assertTrue(mocked.constructed().isEmpty());
            verify(attendanceRepository, never()).saveAndFlush(any(Attendance.class));
        }
    }

    @Test
    @DisplayName("checkIn: stored procedure ERROR status")
    void checkInProcError() {
        Course c = course(10L, "Test", 1L);
        c.setCourseTime("周一 1-2节");
        Student s = student(1L, "Zhang");
        when(courseRepository.findById(10L)).thenReturn(Optional.of(c));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));

        Map<String, Object> procResult = new HashMap<>();
        procResult.put("p_status", "ERROR");
        procResult.put("p_message", "课程时间未设置");

        try (MockedConstruction<SimpleJdbcCall> mocked = mockConstruction(SimpleJdbcCall.class,
                (mock, ctx) -> {
                    when(mock.withProcedureName(anyString())).thenReturn(mock);
                    when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                    when(mock.execute(any(Map.class))).thenReturn(procResult);
                })) {
            Map<String, Object> r = service.checkIn(1L, 10L);
            assertFalse((Boolean) r.get("success"));
        }
    }

    @Test
    @DisplayName("checkIn: unexpected stored procedure return value (null status)")
    void checkInUnexpectedNullStatus() {
        Course c = course(10L, "Test", 1L);
        c.setCourseTime("周一 1-2节");
        Student s = student(1L, "Zhang");
        when(courseRepository.findById(10L)).thenReturn(Optional.of(c));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));

        // Stored procedure returns null — status falls through to save
        Map<String, Object> procResult = new HashMap<>();
        procResult.put("p_status", null);
        procResult.put("p_message", null);

        try (MockedConstruction<SimpleJdbcCall> mocked = mockConstruction(SimpleJdbcCall.class,
                (mock, ctx) -> {
                    when(mock.withProcedureName(anyString())).thenReturn(mock);
                    when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                    when(mock.execute(any(Map.class))).thenReturn(procResult);
                })) {
            Map<String, Object> r = service.checkIn(1L, 10L);
            assertTrue((Boolean) r.get("success"));
            verify(attendanceRepository).saveAndFlush(any(Attendance.class));
        }
    }

    @Test
    @DisplayName("checkIn: unexpected stored procedure return value (UNKNOWN status)")
    void checkInUnexpectedUnknownStatus() {
        Course c = course(10L, "Test", 1L);
        c.setCourseTime("周一 1-2节");
        Student s = student(1L, "Zhang");
        when(courseRepository.findById(10L)).thenReturn(Optional.of(c));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));

        // UNKNOWN is not null and not "OK" → default handler returns false
        Map<String, Object> procResult = new HashMap<>();
        procResult.put("p_status", "UNKNOWN");
        procResult.put("p_message", "未知状态");

        try (MockedConstruction<SimpleJdbcCall> mocked = mockConstruction(SimpleJdbcCall.class,
                (mock, ctx) -> {
                    when(mock.withProcedureName(anyString())).thenReturn(mock);
                    when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                    when(mock.execute(any(Map.class))).thenReturn(procResult);
                })) {
            Map<String, Object> r = service.checkIn(1L, 10L);
            assertFalse((Boolean) r.get("success"));
        }
    }

    @Test
    @DisplayName("addAttendance: new record")
    void addAttendanceNew() {
        when(attendanceRepository.findByStudentIdAndCourseIdAndAttendanceDate(eq(1L), eq(10L), any()))
                .thenReturn(Optional.empty());
        assertTrue(service.addAttendance(1L, 10L, "出勤"));
        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    @DisplayName("addAttendance: update existing")
    void addAttendanceUpdate() {
        Attendance a = new Attendance();
        a.setAttendanceStatus(AttendanceStatus.迟到);
        when(attendanceRepository.findByStudentIdAndCourseIdAndAttendanceDate(eq(1L), eq(10L), any()))
                .thenReturn(Optional.of(a));
        assertTrue(service.addAttendance(1L, 10L, "出勤"));
        verify(attendanceRepository).save(a);
    }

    @Test
    @DisplayName("addAttendance: invalid status returns false")
    void addAttendanceInvalid() {
        assertFalse(service.addAttendance(1L, 10L, "invalid"));
        verify(attendanceRepository, never()).save(any(Attendance.class));
        verify(attendanceRepository, never()).saveAndFlush(any(Attendance.class));
    }

    @Test
    @DisplayName("updateStatus: absent -> leave")
    void updateStatusAbsentToLeave() {
        Attendance a = new Attendance();
        a.setId(1L);
        a.setCourseId(10L);
        a.setAttendanceStatus(AttendanceStatus.缺勤);
        Course c = course(10L, "Test", 1L);
        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(a));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(c));
        Map<String, Object> r = service.updateAttendanceStatus(1L, "请假", 1L, "sick");
        assertTrue((Boolean) r.get("success"));
        assertEquals(AttendanceStatus.请假, a.getAttendanceStatus());
    }

    @Test
    @DisplayName("updateStatus: present -> leave reject")
    void updateStatusPresentToLeave() {
        Attendance a = new Attendance();
        a.setId(1L);
        a.setCourseId(10L);
        a.setAttendanceStatus(AttendanceStatus.出勤);
        Course c = course(10L, "Test", 1L);
        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(a));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(c));
        Map<String, Object> r = service.updateAttendanceStatus(1L, "请假", 1L, "r");
        assertFalse((Boolean) r.get("success"));
    }

    @Test
    @DisplayName("updateStatus: late -> leave reject")
    void updateStatusLateToLeave() {
        Attendance a = new Attendance();
        a.setId(1L);
        a.setCourseId(10L);
        a.setAttendanceStatus(AttendanceStatus.迟到);
        Course c = course(10L, "Test", 1L);
        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(a));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(c));
        Map<String, Object> r = service.updateAttendanceStatus(1L, "请假", 1L, "r");
        assertFalse((Boolean) r.get("success"));
    }

    @Test
    @DisplayName("updateStatus: absent -> present reject")
    void updateStatusAbsentToPresent() {
        Attendance a = new Attendance();
        a.setId(1L);
        a.setCourseId(10L);
        a.setAttendanceStatus(AttendanceStatus.缺勤);
        Course c = course(10L, "Test", 1L);
        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(a));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(c));
        Map<String, Object> r = service.updateAttendanceStatus(1L, "出勤", 1L, "r");
        assertFalse((Boolean) r.get("success"));
    }

    @Test
    @DisplayName("updateStatus: not exists")
    void updateStatusNotExists() {
        when(attendanceRepository.findById(999L)).thenReturn(Optional.empty());
        Map<String, Object> r = service.updateAttendanceStatus(999L, "请假", 1L, "r");
        assertFalse((Boolean) r.get("success"));
    }

    @Test
    @DisplayName("save")
    void saveTest() {
        Attendance a = new Attendance();
        a.setStudentId(1L);
        a.setCourseId(10L);
        assertTrue(service.save(a));
        verify(attendanceRepository).save(a);
    }

    @Test
    @DisplayName("updateById")
    void updateByIdTest() {
        Attendance a = new Attendance();
        a.setId(1L);
        assertTrue(service.updateById(a));
        verify(attendanceRepository).save(a);
    }

    @Test
    @DisplayName("removeById")
    void removeByIdTest() {
        assertTrue(service.removeById(1L));
        verify(attendanceRepository).deleteById(1L);
    }

    @Test
    @DisplayName("list")
    void listTest() {
        when(attendanceRepository.findAll()).thenReturn(Collections.emptyList());
        assertTrue(service.list().isEmpty());
    }

    @Test
    @DisplayName("getServerTime")
    void getServerTimeTest() {
        Map<String, Object> r = service.getServerTime();
        assertNotNull(r.get("timestamp"));
        assertNotNull(r.get("date"));
        assertNotNull(r.get("time"));
    }

    private static Course course(Long id, String name, Long collegeId) {
        Course c = new Course();
        c.setId(id);
        c.setCourseName(name);
        c.setCourseType("ELECTIVE");
        c.setCollegeId(collegeId);
        c.setMaxCount(30);
        c.setTeacherId(1L);
        return c;
    }

    private static Student student(Long id, String name) {
        Student s = new Student();
        s.setId(id);
        s.setName(name);
        s.setStudentNo("S00" + id);
        s.setCollegeId(1L);
        return s;
    }

    private void useClockAt(LocalDateTime dateTime) {
        ZoneId zone = ZoneId.systemDefault();
        Clock fixedClock = Clock.fixed(dateTime.atZone(zone).toInstant(), zone);
        ReflectionTestUtils.setField(service, "clock", fixedClock);
    }
}
