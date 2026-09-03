package com.campus.exam.service.support;

import com.campus.exam.common.enums.QuestionTypeEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

/**
 * 客观题自动判分器（纯函数，可独立单测）。
 *
 * 判分规则（与方案 D5 一致）：
 * - 单选/判断：归一化后精确匹配，全对满分否则 0；
 * - 多选：选项排序后精确匹配，漏选/错选均 0 分（不做半对给分，规则可在此扩展）；
 * - 填空：标准答案支持多空（## 分隔），每空支持多个可接受答案（|| 分隔），
 *         按答对空数比例给分；答案做 trim、大小写、全角半角归一化。
 */
public final class ObjectiveScorer {

    private ObjectiveScorer() {
    }

    public static BigDecimal score(Integer type, String standardAnswer, String userAnswer, int fullScore) {
        QuestionTypeEnum qType = QuestionTypeEnum.of(type);
        String std = standardAnswer == null ? "" : standardAnswer;
        String usr = userAnswer == null ? "" : userAnswer;
        return switch (qType) {
            case SINGLE, JUDGE -> exact(std, usr) ? full(fullScore) : BigDecimal.ZERO;
            case MULTIPLE -> multiMatch(std, usr) ? full(fullScore) : BigDecimal.ZERO;
            case FILL -> fillScore(std, usr, fullScore);
            default -> BigDecimal.ZERO;
        };
    }

    private static boolean exact(String std, String usr) {
        return normalize(std).equals(normalize(usr));
    }

    private static boolean multiMatch(String std, String usr) {
        String s = sortLetters(std);
        String u = sortLetters(usr);
        return s.equals(u) && !s.isEmpty();
    }

    private static BigDecimal fillScore(String std, String usr, int fullScore) {
        String[] stdBlanks = std.split("##", -1);
        String[] usrBlanks = usr.split("##", -1);
        if (stdBlanks.length == 1) {
            return acceptAny(stdBlanks[0], usr) ? full(fullScore) : BigDecimal.ZERO;
        }
        int correct = 0;
        for (int i = 0; i < stdBlanks.length; i++) {
            String one = i < usrBlanks.length ? usrBlanks[i] : "";
            if (acceptAny(stdBlanks[i], one)) {
                correct++;
            }
        }
        if (correct == stdBlanks.length) {
            return full(fullScore);
        }
        return BigDecimal.valueOf(fullScore)
                .multiply(BigDecimal.valueOf(correct))
                .divide(BigDecimal.valueOf(stdBlanks.length), 2, RoundingMode.HALF_UP);
    }

    /** 单个空：|| 分隔的任一可接受答案命中即可 */
    private static boolean acceptAny(String stdBlank, String usr) {
        return Arrays.stream(stdBlank.split("\\|\\|"))
                .anyMatch(a -> normalize(a).equals(normalize(usr)));
    }

    private static String sortLetters(String s) {
        char[] chars = normalize(s).replaceAll("[^A-Za-z]", "").toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }

    /** 归一化：去空白、大写、全角转半角 */
    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        char[] chars = s.trim().toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == 12288) {
                chars[i] = ' ';
            } else if (chars[i] > 65280 && chars[i] < 65375) {
                chars[i] = (char) (chars[i] - 65248);
            }
        }
        return new String(chars).replaceAll("\\s+", "").toUpperCase();
    }

    private static BigDecimal full(int fullScore) {
        return BigDecimal.valueOf(fullScore);
    }
}
