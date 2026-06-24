package com.labcourse.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PasswordPolicy {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGIT = "0123456789";
    private static final String SPECIAL = "!@#$%^&*";
    private static final String ALL = LOWER + UPPER + DIGIT + SPECIAL;

    private PasswordPolicy() {
    }

    public static void requireValid(String password) {
        String error = getValidationError(password);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
    }

    public static String getValidationError(String password) {
        if (password == null || password.isEmpty()) {
            return "密码不能为空";
        }
        if (password.contains(" ")) {
            return "密码中不能包含空格";
        }
        if (password.length() < 8) {
            return "密码长度不足，至少需要8个字符";
        }
        if (password.length() > 20) {
            return "密码长度过长，最多20个字符";
        }
        if (complexity(password) < 3) {
            List<String> missing = new ArrayList<>();
            if (!password.matches(".*[a-z].*")) missing.add("小写字母");
            if (!password.matches(".*[A-Z].*")) missing.add("大写字母");
            if (!password.matches(".*[0-9].*")) missing.add("数字");
            if (!password.matches(".*[^a-zA-Z0-9].*")) missing.add("特殊符号");
            return "密码复杂度不足，缺少：" + String.join("、", missing) + "（需包含至少三种）";
        }
        return null;
    }

    public static String generateTemporaryPassword() {
        List<Character> chars = new ArrayList<>();
        chars.add(randomChar(LOWER));
        chars.add(randomChar(UPPER));
        chars.add(randomChar(DIGIT));
        chars.add(randomChar(SPECIAL));
        for (int i = chars.size(); i < 12; i++) {
            chars.add(randomChar(ALL));
        }
        Collections.shuffle(chars, RANDOM);

        StringBuilder password = new StringBuilder(chars.size());
        for (Character ch : chars) {
            password.append(ch);
        }
        return password.toString();
    }

    private static int complexity(String password) {
        int complexity = 0;
        if (password.matches(".*[a-z].*")) complexity++;
        if (password.matches(".*[A-Z].*")) complexity++;
        if (password.matches(".*[0-9].*")) complexity++;
        if (password.matches(".*[^a-zA-Z0-9].*")) complexity++;
        return complexity;
    }

    private static char randomChar(String chars) {
        return chars.charAt(RANDOM.nextInt(chars.length()));
    }
}
