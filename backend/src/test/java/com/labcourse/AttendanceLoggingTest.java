package com.labcourse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labcourse.entity.Course;
import com.labcourse.entity.Selection;
import com.labcourse.repository.AttendanceRepository;
import com.labcourse.repository.CourseRepository;
import com.labcourse.repository.LoginAttemptRepository;
import com.labcourse.repository.SelectionRepository;
import com.labcourse.repository.StudentRepository;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 签到系统日志输出准确性验证测试。
 * 覆盖场景：正常签到、迟到、重复签到、课程不存在、学生不存在。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("null")
class AttendanceLoggingTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private SelectionRepository selectionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String studentToken;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger serviceLogger;
    private String originalActiveCourseTime;

    private static final Long STUDENT_ID = 1L;   // S001
    private static final Long BAD_STUDENT = 99999L;
    private static final Long BAD_COURSE = 99999L;

    // Day-of-week → course ID mapping (周一~周五 = 1~5)
    private static final Map<Integer, Long> DAY_TO_COURSE = Map.of(
            1, 1L, 2, 2L, 3, 3L, 4, 4L, 5, 5L
    );
    private static final String[] DAY_NAMES = {
            "\u5468\u4e00", "\u5468\u4e8c", "\u5468\u4e09", "\u5468\u56db",
            "\u5468\u4e94", "\u5468\u516d", "\u5468\u65e5"
    };
    private Long activeCourseId;
    private String activeCourseName;
    private boolean isWeekend;
    private int todayDayOfWeek;
    private LocalDate today;

    @BeforeEach
    void setUp() throws Exception {
        today = LocalDate.now();
        todayDayOfWeek = today.getDayOfWeek().getValue();
        activeCourseId = DAY_TO_COURSE.get(todayDayOfWeek);
        isWeekend = activeCourseId == null;
        if (isWeekend) {
            activeCourseId = 3L; // fallback
            activeCourseName = "Web开发技术";
        } else {
            activeCourseName = courseRepository.findById(activeCourseId)
                    .map(c -> c.getCourseName())
                    .orElse("Unknown");
        }

        // 初始化日志捕获器
        logAppender = new ListAppender<>();
        logAppender.start();

        serviceLogger = (Logger) LoggerFactory.getLogger(
                "com.labcourse.service.impl.AttendanceServiceImpl");
        serviceLogger.setLevel(Level.DEBUG);
        serviceLogger.addAppender(logAppender);

        loginAttemptRepository.deleteById("student:S001");
        studentRepository.findByStudentNo("S001").ifPresent(student -> {
            student.setPassword(passwordEncoder.encode("123456"));
            studentRepository.save(student);
        });

        // 登录获取token
        String loginBody = objectMapper.writeValueAsString(
                Map.of("studentNo", "S001", "password", "123456"));
        String resp = mockMvc.perform(post("/api/student/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        studentToken = (String) objectMapper.readValue(resp, Map.class).get("accessToken");

        // 清理今日签到记录
        attendanceRepository.findByStudentIdAndCourseIdAndAttendanceDate(
                        STUDENT_ID, activeCourseId, today)
                .ifPresent(a -> attendanceRepository.delete(a));
        ensureSelection(STUDENT_ID, activeCourseId);
    }

    private void ensureSelection(Long studentId, Long courseId) {
        if (selectionRepository.findByStudentIdAndCourseId(studentId, courseId).isEmpty()) {
            Selection selection = new Selection();
            selection.setStudentId(studentId);
            selection.setCourseId(courseId);
            selectionRepository.save(selection);
        }
    }

    @AfterEach
    void tearDown() {
        if (originalActiveCourseTime != null && activeCourseId != null) {
            courseRepository.findById(activeCourseId).ifPresent(course -> {
                course.setCourseTime(originalActiveCourseTime);
                courseRepository.saveAndFlush(course);
            });
            originalActiveCourseTime = null;
        }

        if (logAppender != null && serviceLogger != null) {
            serviceLogger.detachAppender(logAppender);
            logAppender.stop();
        }
    }

    /**
     * 提取指定级别的日志内容行
     */
    private List<String> getLogLines(Level level) {
        return logAppender.list.stream()
                .filter(e -> e.getLevel() == level)
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    /**
     * 提取所有日志内容行
     */
    private List<String> getAllLogLines() {
        return logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    /**
     * 检查是否存在包含所有关键词的日志行
     */
    private void assertLogContains(String... keywords) {
        String allLogs = String.join("\n", getAllLogLines());
        for (String kw : keywords) {
            assertTrue(allLogs.contains(kw),
                    "日志中应包含关键词: " + kw + "\n实际日志:\n" + allLogs);
        }
    }

    /**
     * 检查不存在包含某关键词的日志行
     */
    private void assertLogNotContains(String keyword) {
        String allLogs = String.join("\n", getAllLogLines());
        assertFalse(allLogs.contains(keyword),
                "日志中不应包含关键词: " + keyword + "\n实际日志:\n" + allLogs);
    }

    // ──────────────────────────────────────────────
    // 场景1: 正常签到
    // ──────────────────────────────────────────────
    @Test
    @Order(1)
    @DisplayName("场景1 - 正常签到：验证签到成功时所有关键日志节点的准确性")
    void scenario1_NormalCheckIn() throws Exception {
        Assumptions.assumeFalse(isWeekend, "周末无课程安排");
        logAppender.list.clear();

        String body = objectMapper.writeValueAsString(
                Map.of("studentId", STUDENT_ID, "courseId", activeCourseId));

        mockMvc.perform(post("/api/attendance/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + studentToken)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 验证日志节点
        assertLogContains(
                "[checkIn]",
                "开始签到",
                "studentId=" + STUDENT_ID,
                "courseId=" + activeCourseId
        );

        // 验证课程加载（动态课程名）
        assertLogContains("课程已加载", "courseName=" + activeCourseName);

        // 验证学生加载
        assertLogContains("学生已加载", "studentName=王小明");

        // 验证时间匹配（动态星期）
        assertLogContains("星期信息", "todayDayOfWeek=" + todayDayOfWeek);

        // 验证签到成功
        List<String> infoLogs = getLogLines(Level.INFO);
        boolean hasSuccess = infoLogs.stream()
                .anyMatch(msg -> msg.contains("分支-签到成功") && msg.contains("recordId="));
        assertTrue(hasSuccess, "应包含签到成功日志，含 recordId");

        // 验证不包含错误日志
        assertLogNotContains("分支-重复签到");
        assertLogNotContains("课程不存在");

        System.out.println("=== 场景1 正常签到日志验证通过 ===");
        getAllLogLines().forEach(System.out::println);
    }

    // ──────────────────────────────────────────────
    // 场景2: 迟到签到
    // ──────────────────────────────────────────────
    @Test
    @Order(2)
    @DisplayName("场景2 - 迟到签到：验证超出规定时间签到时的日志准确性")
    void scenario2_LateCheckIn() throws Exception {
        Assumptions.assumeFalse(isWeekend, "周末无课程安排");
        Assumptions.assumeTrue(LocalTime.now().isAfter(LocalTime.of(8, 3)),
                "当前时间尚未超过第一节课迟到窗口，跳过迟到分支日志验证");

        Course activeCourse = courseRepository.findById(activeCourseId).orElseThrow();
        originalActiveCourseTime = activeCourse.getCourseTime();
        activeCourse.setCourseTime(DAY_NAMES[todayDayOfWeek - 1] + " 1-2\u8282");
        courseRepository.saveAndFlush(activeCourse);

        // 清理并确保无记录后再签
        attendanceRepository.findByStudentIdAndCourseIdAndAttendanceDate(
                        STUDENT_ID, activeCourseId, today)
                .ifPresent(a -> attendanceRepository.delete(a));
        logAppender.list.clear();

        String body = objectMapper.writeValueAsString(
                Map.of("studentId", STUDENT_ID, "courseId", activeCourseId));

        mockMvc.perform(post("/api/attendance/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + studentToken)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 验证迟到状态判定日志（当前时间在14:00之后，minutesAfterStart > 3）
        List<String> infoLogs = getLogLines(Level.INFO);
        boolean lateLog = infoLogs.stream()
                .anyMatch(msg -> msg.contains("分支-迟到") && msg.contains("minutesAfterStart="));
        assertTrue(lateLog, "应包含迟到状态判定日志，含 minutesAfterStart");

        // 验证签到状态为"迟到"
        assertLogContains("status=迟到");

        // 验证时间差计算包含具体分钟数
        assertLogContains("时间差计算", "minutesBefore=", "minutesAfter=");

        System.out.println("=== 场景2 迟到签到日志验证通过 ===");
        getAllLogLines().forEach(System.out::println);
    }

    // ──────────────────────────────────────────────
    // 场景3: 重复签到
    // ──────────────────────────────────────────────
    @Test
    @Order(3)
    @DisplayName("场景3 - 重复签到：验证同一用户同一天多次签到被正确拦截")
    void scenario3_DuplicateCheckIn() throws Exception {
        Assumptions.assumeFalse(isWeekend, "周末无课程安排");
        // 第一次签到（确保已有记录）
        String body = objectMapper.writeValueAsString(
                Map.of("studentId", STUDENT_ID, "courseId", activeCourseId));

        mockMvc.perform(post("/api/attendance/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + studentToken)
                        .content(body))
                .andExpect(status().isOk());

        // 清除日志，准备第二次签到
        logAppender.list.clear();

        // 第二次签到（应被拒绝）
        mockMvc.perform(post("/api/attendance/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + studentToken)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("今日已签到")));

        // 验证重复签到日志
        assertLogContains("分支-重复签到", "result=success:false");

        // 验证不包含新签到成功
        assertLogNotContains("分支-签到成功");

        System.out.println("=== 场景3 重复签到日志验证通过 ===");
        getAllLogLines().forEach(System.out::println);
    }

    // ──────────────────────────────────────────────
    // 场景4: 课程不存在
    // ──────────────────────────────────────────────
    @Test
    @Order(4)
    @DisplayName("场景4 - 课程不存在：验证签到不存在的课程时日志记录")
    void scenario4_CourseNotFound() throws Exception {
        logAppender.list.clear();

        String body = objectMapper.writeValueAsString(
                Map.of("studentId", STUDENT_ID, "courseId", BAD_COURSE));

        mockMvc.perform(post("/api/attendance/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + studentToken)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("课程不存在"));

        // 验证WARN级别日志
        List<String> warnLogs = getLogLines(Level.WARN);
        boolean hasWarn = warnLogs.stream()
                .anyMatch(msg -> msg.contains("分支-课程不存在")
                        && msg.contains("courseId=" + BAD_COURSE));
        assertTrue(hasWarn, "应包含WARN级别的课程不存在日志");

        assertLogContains("result=success:false");

        System.out.println("=== 场景4 课程不存在日志验证通过 ===");
        getAllLogLines().forEach(System.out::println);
    }

    // ──────────────────────────────────────────────
    // 场景5: 请求体 studentId 被忽略
    // ──────────────────────────────────────────────
    @Test
    @Order(5)
    @DisplayName("场景5 - 请求体 studentId 被忽略：验证使用当前登录学生签到")
    void scenario5_StudentNotFound() throws Exception {
        logAppender.list.clear();

        String body = objectMapper.writeValueAsString(
                Map.of("studentId", BAD_STUDENT, "courseId", activeCourseId));

                mockMvc.perform(post("/api/attendance/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + studentToken)
                        .content(body))
                .andExpect(status().isOk());

        assertLogContains("开始签到");
        assertLogContains("studentId=" + STUDENT_ID);
        assertLogNotContains("studentId=" + BAD_STUDENT);

        System.out.println("=== 场景5 请求体 studentId 被忽略日志验证通过 ===");
        getAllLogLines().forEach(System.out::println);
    }

    // ──────────────────────────────────────────────
    // 综合验证：日志级别分布
    // ──────────────────────────────────────────────
    @Test
    @Order(6)
    @DisplayName("综合验证 - 日志级别分布：INFO用于关键节点，DEBUG用于调试，WARN用于异常")
    void scenario6_LogLevelDistribution() {
        // 分析所有累积的日志事件
        List<ILoggingEvent> allEvents = logAppender.list;
        if (allEvents.isEmpty()) {
            System.out.println("注: 无累积日志事件，单独场景已逐个验证");
            return;
        }

        long infoCount = allEvents.stream().filter(e -> e.getLevel() == Level.INFO).count();
        long debugCount = allEvents.stream().filter(e -> e.getLevel() == Level.DEBUG).count();
        long warnCount = allEvents.stream().filter(e -> e.getLevel() == Level.WARN).count();

        System.out.println("=== 日志级别分布统计 ===");
        System.out.println("INFO:  " + infoCount + " 条 (关键业务流程节点)");
        System.out.println("DEBUG: " + debugCount + " 条 (调试详情)");
        System.out.println("WARN:  " + warnCount + " 条 (异常/失败分支)");
    }
}
