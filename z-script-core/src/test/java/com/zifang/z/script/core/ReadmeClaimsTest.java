package com.zifang.z.script.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * README 里每一条定量主张都必须能从源码重算出来 —— 广告出去的话就是承诺。
 *
 * <p>为什么要有这一层：本仓 README 把「17 张表」「z-script-web 下 19 个 Controller、
 * z-script-scene-http 再加 1 个，共 20 个」「当前版本 1.0.1」「6 个引擎 Bean」
 * 写成了硬数字，而每加一张表、每加一个 Controller、每抬一次版本都会漂一次。
 * 没有任何尺子去问代码「现在还是吗」，那这些数字的保质期就是上一次编辑的时刻。</p>
 *
 * <p>口径：缺一条主张 = 红（不许「正则没匹配上就跳过」，那正是空跑）；
 * 数值不等 = 红且把<b>重算值</b>打进消息（红了要知道真数是多少，否则只是又一处猜）。
 * 尺自己的牙口由 {@link #rulerActuallyCondemnsWrongNumbers()} 用合成 README 证明，
 * 而「不许冤枉健康页」由 {@link #readmeClaimsMatchTheCode()} 本身证明。</p>
 *
 * <p>与骨架的一处有意差别：核对结果<b>一次全报</b>（收集齐所有不一致再抛 AssertionError），
 * 而不是撞上第一条就停。理由是本仓 README 已经有存量漂移，撞停就只会报出第一条，
 * 后面的漂移要改一次 README 才能看见下一次 —— 那等于把尺子退化成「一次只说一句」。
 * 牙口测试也因此能证明「改之前这条主张是对得上的」：改之前的消息里不应出现该主张，
 * 改之后才出现 —— 两者一夹，变异到底有没有被尺子抓住就没有解释空间了。</p>
 */
class ReadmeClaimsTest {

    /** 仓根：向上找同时有 README.md 与 pom.xml 的目录（测试跑在模块目录，user.dir 不是仓根）。 */
    private static Path repoRoot() {
        Path p = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (int i = 0; i < 4 && p != null; i++, p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("README.md")) && Files.isRegularFile(p.resolve("pom.xml"))) {
                return p;
            }
        }
        throw new IllegalStateException("定位仓根失败：user.dir=" + System.getProperty("user.dir"));
    }

    private static String read(Path f) {
        try {
            return new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String norm(Path p) {
        return p.toString().replace('\\', '/');
    }

    /** 仓内全部 java 源文件（排除构建产物与参考树）。 */
    private static List<Path> javaSources(Path root) {
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !norm(p).contains("/target/"))
                    .filter(p -> !norm(p).contains("/node_modules/"))
                    .filter(p -> !norm(p).contains("/.git/"))
                    .filter(p -> !norm(p).contains("/_code_temp/"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 某个模块下 src/main 里的 *Controller.java 个数（moduleRelPath 相对仓根，如 {@code z-script-web}）。
     *
     * <p>只数 {@code src/main} —— README 说的「N 个 Controller」指的是生产代码；
     * {@code src/test} 下同名目录里躺的是测试类（守卫自己就躺在 src/test 里）。</p>
     */
    private static long countControllers(Path root, String moduleRelPath) {
        Path base = root.resolve(moduleRelPath);
        if (!Files.isDirectory(base)) {
            throw new IllegalStateException("仓里没有模块目录：" + moduleRelPath);
        }
        try (Stream<Path> s = Files.walk(base)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith("Controller.java"))
                    .filter(p -> norm(p).contains("/src/main/"))
                    .count();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** 全仓 src/main 下的 *Controller.java 个数（README「共 N 个」用的就是这个口径）。 */
    private static long countControllers(Path root) {
        return javaSources(root).stream()
                .filter(p -> p.getFileName().toString().endsWith("Controller.java"))
                .filter(p -> norm(p).contains("/src/main/"))
                .count();
    }

    /**
     * 全部 @TableName 锚定的表名（去重）。
     *
     * <p>只认 {@code src/main} 下的源文件 —— README 说的「N 张表」指的是生产代码的表。
     * 守卫自己就躺在 {@code src/test} 里，源文件里还留着 {@code @TableName} 这个字面量，
     * 不加这道过滤，尺子就会把自己的源码算成一张表。</p>
     */
    private static Set<String> tableNames(List<Path> sources) {
        Pattern p = Pattern.compile("@TableName\\(\"([^\"]+)\"\\)");
        Set<String> names = new TreeSet<>();
        for (Path f : sources) {
            if (!norm(f).contains("/src/main/")) {
                continue;
            }
            Matcher m = p.matcher(read(f));
            while (m.find()) {
                names.add(m.group(1));
            }
        }
        return names;
    }

    /**
     * 从根 pom 里取 <revision> 面值。
     *
     * <p>本仓是 CI-friendly versions：根 pom 的 {@code <version>${revision}</version>}，
     * 真实版本面在 {@code <revision>} 属性里 —— 抬版本只改那一处。</p>
     */
    private static String revision(Path root) {
        Matcher m = Pattern.compile("<revision>([^<]+)</revision>").matcher(read(root.resolve("pom.xml")));
        if (!m.find()) {
            throw new IllegalStateException("根 pom 里没有 <revision>");
        }
        return m.group(1).trim();
    }

    /** 仓内全部 pom.xml 个数（排除构建产物）—— 仓的模块总数（含聚合 pom 本身）。 */
    private static int pomCount(Path root) {
        try (Stream<Path> s = Files.walk(root)) {
            return (int) s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals("pom.xml"))
                    .filter(p -> !norm(p).contains("/target/"))
                    .count();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** 根 pom 的 central profile 里 <excludeArtifact> 条数 —— 这些坐标不发到 Central。 */
    private static int excludedArtifactCount(Path root) {
        Matcher m = Pattern.compile("<excludeArtifact>([^<]+)</excludeArtifact>")
                .matcher(read(root.resolve("pom.xml")));
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    /** 某个模块 src/test 里的 {@code @Test} 方法数（与 surefire 的 Tests run 同口径，仓内无参数化用例）。 */
    private static long testMethodCount(Path root, String moduleRelPath) {
        Path base = root.resolve(moduleRelPath);
        if (!Files.isDirectory(base)) {
            throw new IllegalStateException("仓里没有模块目录：" + moduleRelPath);
        }
        try (Stream<Path> s = Files.walk(base)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .filter(p -> norm(p).contains("/src/test/"))
                    .mapToLong(p -> {
                        Matcher m = Pattern.compile("(?m)^\\s*@Test\\b(?!\\w)").matcher(read(p));
                        long n = 0;
                        while (m.find()) {
                            n++;
                        }
                        return n;
                    })
                    .sum();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** 某个类里 {@code @Bean} 方法的个数（数方法级注解，注释里的 @Bean 不算）。 */
    private static long beanCount(Path root, String classFileName) {        Path src;
        try (Stream<Path> s = Files.walk(root)) {
            src = s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(classFileName))
                    .filter(p -> norm(p).contains("/src/main/"))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (src == null) {
            throw new IllegalStateException("仓里找不到源文件：" + classFileName);
        }
        Matcher m = Pattern.compile("(?m)^\\s*@Bean\\s*$").matcher(read(src));
        long n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    /** 一次重算出的全部数字。 */
    private static final class Counts {
        final int tables;
        final long webControllers;
        final long sceneHttpControllers;
        final long allControllers;
        final long engineBeans;
        final int publishedCoordinates;
        final long engineTests;
        final long sceneHttpTests;
        final String revision;

        Counts(int tables, long webControllers, long sceneHttpControllers, long allControllers,
               long engineBeans, int publishedCoordinates, long engineTests, long sceneHttpTests,
               String revision) {
            this.tables = tables;
            this.webControllers = webControllers;
            this.sceneHttpControllers = sceneHttpControllers;
            this.allControllers = allControllers;
            this.engineBeans = engineBeans;
            this.publishedCoordinates = publishedCoordinates;
            this.engineTests = engineTests;
            this.sceneHttpTests = sceneHttpTests;
            this.revision = revision;
        }
    }

    private static Counts recomputed() {
        Path root = repoRoot();
        List<Path> src = javaSources(root);
        return new Counts(
                tableNames(src).size(),
                countControllers(root, "z-script-web"),
                countControllers(root, "z-script-scene/z-script-scene-http"),
                countControllers(root),
                beanCount(root, "ZScriptWebAutoConfiguration.java"),
                pomCount(root) - excludedArtifactCount(root),
                testMethodCount(root, "z-script-engine"),
                testMethodCount(root, "z-script-scene/z-script-scene-http"),
                revision(root));
    }

    /**
     * README 里逐条抠出数字并与重算值对账；任何一条对不上就抛 AssertionError，
     * 并把<b>所有</b>不一致连同真数一起打进去（撞停只会报出第一条，后面的漂移看不见）。
     */
    private static void verifyClaims(String readme, Counts c) {
        List<String> bad = new ArrayList<>();
        Matcher m;

        // 主张 1：基本信息表「当前版本 | `X.Y.Z`（根 POM `<revision>`…）」
        m = Pattern.compile("\\*\\*当前版本\\*\\*\\s*\\|\\s*`([^`]+)`").matcher(readme);
        if (!m.find()) {
            bad.add("README 里找不到「**当前版本** | `X.Y.Z`」这条版本主张");
        } else if (!c.revision.equals(m.group(1))) {
            bad.add("版本号对不上：README 写 " + m.group(1) + "，根 pom <revision> 是 " + c.revision);
        }

        // 主张 2：项目结构块「<revision>=X.Y.Z 统一版本」
        m = Pattern.compile("<revision>=([^\\s]+)\\s+统一版本").matcher(readme);
        if (!m.find()) {
            bad.add("README 里找不到「<revision>=X.Y.Z 统一版本」这条版本主张");
        } else if (!c.revision.equals(m.group(1))) {
            bad.add("版本号对不上：项目结构块写 " + m.group(1) + "，根 pom <revision> 是 " + c.revision);
        }

        // 主张 3：「`z-script-web` 下实测 N 个 Controller，`z-script-scene-http` 再加 N 个，共 N 个」
        m = Pattern.compile("`z-script-web` 下实测 (\\d+) 个 Controller，`z-script-scene-http` 再加 (\\d+) 个，共 (\\d+) 个")
                .matcher(readme);
        if (!m.find()) {
            bad.add("README 里找不到「z-script-web 下 N 个 Controller，z-script-scene-http 再加 N 个，共 N 个」这条主张");
        } else {
            if (c.webControllers != Long.parseLong(m.group(1))) {
                bad.add("Controller 数对不上：README 写 z-script-web 有 " + m.group(1)
                        + " 个，src/main 下实数是 " + c.webControllers);
            }
            if (c.sceneHttpControllers != Long.parseLong(m.group(2))) {
                bad.add("Controller 数对不上：README 写 z-script-scene-http 有 " + m.group(2)
                        + " 个，src/main 下实数是 " + c.sceneHttpControllers);
            }
            if (c.allControllers != Long.parseLong(m.group(3))) {
                bad.add("Controller 总数对不上：README 写共 " + m.group(3)
                        + " 个，全仓 src/main 下实数是 " + c.allControllers);
            }
        }

        // 主张 4：项目结构块「z-script-web/ # Starter 层：N 个 Controller」
        m = Pattern.compile("z-script-web/\\s*#\\s*Starter 层：(\\d+) 个 Controller").matcher(readme);
        if (!m.find()) {
            bad.add("README 里找不到「z-script-web/ # Starter 层：N 个 Controller」这条主张");
        } else if (c.webControllers != Long.parseLong(m.group(1))) {
            bad.add("Controller 数对不上：项目结构块写 " + m.group(1)
                    + " 个，z-script-web/src/main 下实数是 " + c.webControllers);
        }

        // 主张 5：项目结构块「z-script-core/ # 领域层：N 张表的实体」
        m = Pattern.compile("z-script-core/\\s*#\\s*领域层：(\\d+) 张表的实体").matcher(readme);
        if (!m.find()) {
            bad.add("README 里找不到「z-script-core/ # 领域层：N 张表的实体」这条主张");
        } else if (c.tables != Integer.parseInt(m.group(1))) {
            bad.add("表数对不上：README 写 " + m.group(1)
                    + " 张，@TableName 唯一值实数是 " + c.tables);
        }

        // 主张 6：建库段「幂等：N 张表 + M 条演示 seed」（演示 seed 那条数不在盘面口径内，见交付说明）
        m = Pattern.compile("幂等：(\\d+) 张表").matcher(readme);
        if (!m.find()) {
            bad.add("README 里找不到「幂等：N 张表」这条主张");
        } else if (c.tables != Integer.parseInt(m.group(1))) {
            bad.add("表数对不上：建库段写 " + m.group(1) + " 张，@TableName 唯一值实数是 " + c.tables);
        }

        // 主张 7：「ZScriptWebAutoConfiguration（… + N 个引擎 Bean）」
        m = Pattern.compile("ZScriptWebAutoConfiguration`（`@ComponentScan\\(\"com\\.zifang\\.z\\.script\"\\)` \\+ (\\d+) 个引擎 Bean）")
                .matcher(readme);
        if (!m.find()) {
            bad.add("README 里找不到「+ N 个引擎 Bean」这条主张");
        } else if (c.engineBeans != Long.parseLong(m.group(1))) {
            bad.add("引擎 Bean 数对不上：README 写 " + m.group(1)
                    + " 个，ZScriptWebAutoConfiguration 里 @Bean 方法实数是 " + c.engineBeans);
        }

        // 主张 8：「上述 N 个库坐标」—— 仓内 pom 数减去 central profile 里被排除的坐标数
        m = Pattern.compile("上述 (\\d+) 个库坐标").matcher(readme);
        if (!m.find()) {
            bad.add("README 里找不到「上述 N 个库坐标」这条主张");
        } else if (c.publishedCoordinates != Integer.parseInt(m.group(1))) {
            bad.add("发布坐标数对不上：README 写 " + m.group(1)
                    + " 个，仓内 pom 数减去 central profile 排除项实数是 " + c.publishedCoordinates);
        }

        // 主张 9：「实测 N 项：engine N + scene-http N」
        // 总项数只核 README 自己的算术（分项之和），不核全仓 @Test 总数 ——
        // 守卫自己就住在 z-script-core，它自己的 3 个用例会把全仓总数顶上去，
        // 拿全仓总数去对 README 写的 36 就是自指，必然漂。
        // 「实测 36 项：\n> engine 15 + scene-http 21」—— 分项之间隔着换行 + 引用符 "> "，
        // 正则不能假设它们挨在一起。
        m = Pattern.compile("实测 (\\d+) 项：[^0-9]*?engine (\\d+) \\+ scene-http (\\d+)").matcher(readme);
        if (!m.find()) {
            bad.add("README 里找不到「实测 N 项：engine N + scene-http N」这条主张");
        } else {
            long saidTotal = Long.parseLong(m.group(1));
            long saidEngine = Long.parseLong(m.group(2));
            long saidSceneHttp = Long.parseLong(m.group(3));
            if (c.engineTests != saidEngine) {
                bad.add("测试用例数对不上：README 写 engine " + saidEngine
                        + " 项，z-script-engine/src/test 下 @Test 方法实数是 " + c.engineTests);
            }
            if (c.sceneHttpTests != saidSceneHttp) {
                bad.add("测试用例数对不上：README 写 scene-http " + saidSceneHttp
                        + " 项，z-script-scene-http/src/test 下 @Test 方法实数是 " + c.sceneHttpTests);
            }
            if (saidTotal != saidEngine + saidSceneHttp) {
                bad.add("README 内部不自洽：总项数写 " + saidTotal + " 项，但 engine " + saidEngine
                        + " + scene-http " + saidSceneHttp + " = " + (saidEngine + saidSceneHttp));
            }
        }

        if (!bad.isEmpty()) {
            throw new AssertionError("README 的定量主张与盘面对不上（" + bad.size() + " 条）：\n  - "
                    + String.join("\n  - ", bad));
        }
    }

    /** 跑一遍核对，把它抛出的 AssertionError 消息取出来（没抛就返回空串）。 */
    private static String messageOf(String readme, Counts c) {
        try {
            verifyClaims(readme, c);
            return "";
        } catch (AssertionError e) {
            return e.getMessage() == null ? "" : e.getMessage();
        }
    }

    @Test
    @DisplayName("README 的定量主张与源码重算值逐条对账")
    void readmeClaimsMatchTheCode() {
        Path root = repoRoot();
        Counts c = recomputed();
        verifyClaims(read(root.resolve("README.md")), c);
    }

    @Test
    @DisplayName("尺子的牙口：把 README 的数改错，判定必须判红")
    void rulerActuallyCondemnsWrongNumbers() {
        String readme = read(repoRoot().resolve("README.md"));
        Counts c = recomputed();

        // 挑一条**本来就对得上**的主张来改：改之前它的报错消息里不该出现它
        String before = messageOf(readme, c);
        assertFalse(before.contains("表数对不上"),
                "改之前「表数」这条就不对上了（README " + before + "）—— 换一条本来对得上的主张来做变异");

        String tampered = readme.replaceFirst("17 张表", "16 张表");
        assertNotEquals(readme, tampered, "篡改失败：README 里找不到「17 张表」字样（写法变了？）");

        AssertionError err = assertThrows(AssertionError.class,
                () -> verifyClaims(tampered, c));
        assertTrue(err.getMessage().contains("表数对不上"), "报错要指向真正对不上的那条主张，实际是：" + err.getMessage());
    }

    @Test
    @DisplayName("不许冤枉健康页：真实 README 不得被判红")
    void healthyReadmeIsNotCondemned() {
        String readme = read(repoRoot().resolve("README.md"));
        verifyClaims(readme, recomputed());
    }
}
