package com.zifang.z.script.engine;

import com.zifang.util.core.lang.RandomUtil;
import com.zifang.util.core.time.DateUtil;
import com.zifang.util.json.JsonUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mock 模板渲染引擎
 * <p>
 * 支持模板表达式:
 * - @pick('a', 'b', 'c')  - 随机选取一个值
 * - @pick(1, 2, 3)        - 随机选取一个数字
 * - @sequence(start)       - 自增序列
 * - @datetime              - 当前日期时间
 * - @date                  - 当前日期
 * - @uuid                  - 随机UUID
 * - @int(min, max)         - 随机整数
 * - @sentence(min, max)    - 随机中文句子 (min-max个字)
 * - @name                  - 随机姓名
 * - @email                 - 随机邮箱
 * - @phone                 - 随机手机号
 * - @boolean               - 随机布尔值
 */
public class MockTemplateEngine {

    private static final Logger log = LogManager.getLogger(MockTemplateEngine.class);
    private static final Pattern EXPR_PATTERN = Pattern.compile("@(\\w+)(?:\\(([^)]*)\\))?");
    private static final AtomicLong SEQUENCE = new AtomicLong(0);
    private static final Random RANDOM = new Random();

    private static final String[] SAMPLE_NAMES = {
            "张三", "李四", "王五", "赵六", "钱七", "孙八", "周九", "吴十",
            "郑冬", "陈明", "林芳", "黄伟", "刘洋", "杨帆", "马超", "许晴"
    };

    private static final String[] DOMAINS = {
            "example.com", "test.org", "demo.io", "sample.cn"
    };

    private static final String SAMPLE_CHARS = "的一是了我不在人有大为上个国我以发他工时要动就出年会也到说成里用水种过对方行所日之与对看于并自车";

    /**
     * 渲染模板
     *
     * @param template JSON字符串模板 (可包含 @expressions)
     * @param params   外部参数
     * @return 渲染后的JSON对象
     */
    public Object render(String template, Map<String, Object> params) {
        try {
            // Try parsing as JSON first
            String processed = processTemplateString(template);
            Object parsed = JsonUtil.parseObject(processed);
            return parsed;
        } catch (Exception e) {
            // If not valid JSON, return as processed string
            return processTemplateString(template);
        }
    }

    /**
     * 处理模板字符串，替换所有 @expression
     */
    private String processTemplateString(String template) {
        Matcher matcher = EXPR_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String replacement = evaluateExpression(matcher.group(1), matcher.group(2));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 评估单个表达式
     */
    private String evaluateExpression(String funcName, String args) {
        switch (funcName) {
            case "pick":
                return evalPick(args);
            case "sequence":
                long start = 1;
                if (args != null && !args.trim().isEmpty()) {
                    start = Long.parseLong(args.trim());
                }
                return String.valueOf(SEQUENCE.incrementAndGet() + start - 1);
            case "datetime":
                return DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");
            case "date":
                return DateUtil.format(new Date(), "yyyy-MM-dd");
            case "uuid":
                return RandomUtil.uuidCompact();
            case "int":
                return evalInt(args);
            case "sentence":
                return evalSentence(args);
            case "name":
                return SAMPLE_NAMES[RANDOM.nextInt(SAMPLE_NAMES.length)];
            case "email":
                return SAMPLE_NAMES[RANDOM.nextInt(SAMPLE_NAMES.length)].toLowerCase()
                        + RANDOM.nextInt(999) + "@" + DOMAINS[RANDOM.nextInt(DOMAINS.length)];
            case "phone":
                return "1" + (3 + RANDOM.nextInt(7)) + String.format("%09d", RANDOM.nextInt(1000000000));
            case "boolean":
                return String.valueOf(RANDOM.nextBoolean());
            default:
                return "@" + funcName + (args != null ? "(" + args + ")" : "");
        }
    }

    private String evalPick(String args) {
        if (args == null || args.trim().isEmpty()) {
            return "";
        }
        String[] options = args.split(",");
        String picked = options[RANDOM.nextInt(options.length)].trim();
        // Remove surrounding quotes if present
        if (picked.startsWith("'") && picked.endsWith("'")) {
            picked = picked.substring(1, picked.length() - 1);
        }
        if (picked.startsWith("\"") && picked.endsWith("\"")) {
            picked = picked.substring(1, picked.length() - 1);
        }
        return picked;
    }

    private String evalInt(String args) {
        int min = 0, max = 100;
        if (args != null) {
            String[] parts = args.split(",");
            if (parts.length >= 1) {
                min = Integer.parseInt(parts[0].trim());
            }

            if (parts.length >= 2) {
                max = Integer.parseInt(parts[1].trim());
            }

        }
        return String.valueOf(min + RANDOM.nextInt(Math.max(1, max - min + 1)));
    }

    private String evalSentence(String args) {
        int min = 5, max = 10;
        if (args != null) {
            String[] parts = args.split(",");
            if (parts.length >= 1) {
                min = Integer.parseInt(parts[0].trim());
            }

            if (parts.length >= 2) {
                max = Integer.parseInt(parts[1].trim());
            }

        }
        int len = min + RANDOM.nextInt(Math.max(1, max - min + 1));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(SAMPLE_CHARS.charAt(RANDOM.nextInt(SAMPLE_CHARS.length())));
        }
        return sb.toString();
    }
}
