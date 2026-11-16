package com.studyloop.review.util;

import java.time.LocalDate;
import java.util.List;

/**
 * 算连续复习天数。
 * <p>
 * 单独抽出来是因为它纯计算、没依赖，能直接写单元测试。
 * 写成 private 方法塞进 Service 里就只能靠"跑起来看"验证了。
 */
public class StreakCalculator {

    private StreakCalculator() {
    }

    /**
     * @param dates 复习日期，必须是倒序（最近的在前）且已去重
     * @param today 今天
     * @return 连续天数
     */
    public static int calculate(List<LocalDate> dates, LocalDate today) {
        if (dates == null || dates.isEmpty()) {
            return 0;
        }

        // 今天还没复习不算断签：从昨天开始数
        LocalDate cursor;
        if (dates.get(0).equals(today)) {
            cursor = today;
        } else if (dates.get(0).equals(today.minusDays(1))) {
            cursor = today.minusDays(1);
        } else {
            return 0;
        }

        int days = 0;
        for (LocalDate date : dates) {
            if (date.equals(cursor)) {
                days++;
                cursor = cursor.minusDays(1);
            } else if (date.isBefore(cursor)) {
                // 中间断了
                break;
            }
        }
        return days;
    }
}
