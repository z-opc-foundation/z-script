# 测试必须跑在 JDK 17 或更低 —— JDK 23 上有 4 条 Groovy 用例必红（2026-10-04 实测）

## 现象

在 **JDK 23** 上跑 `mvn -o clean test`，`ScriptSandboxTest` 里 4 条必红：

    Tests run: 15, Failures: 4, Errors: 0, Skipped: 0

    org.opentest4j.AssertionFailedError: Unexpected exception type thrown,
      expected: <com.zifang.z.script.engine.sandbox.ScriptSecurityException>
      but was: <java.lang.RuntimeException>
    Caused by: java.lang.RuntimeException: BUG! exception in phase 'semantic analysis'
      in source unit 'Script1.groovy' Unsupported class file major version 67
    Caused by: java.lang.IllegalArgumentException: Unsupported class file major version 67
        at org.codehaus.groovy.ast.decompiled.AsmDecompiler.parseClass(AsmDecompiler.java:81)

失败的 4 条是 `groovy_blocks_runtime_exec` / `groovy_blocks_file_import` /
`groovy_blocks_fully_qualified_runtime` / `groovy_blocks_system_exit`
—— 全是**沙箱拦截**用例，也就是这个仓最要紧的那几条。

## 真因：不是代码缺陷，是测试 JVM 太新

`major version 67` = **Java 23** 的 class 文件格式。Groovy 3.0.9（2021 年）里的
`AsmDecompiler` 用的 ASM 读不了那么新的字节码，一进 Groovy 编译器的
"semantic analysis" 阶段就抛 `IllegalArgumentException`。

于是 `ScriptSecurityException` 根本没机会抛出来 —— 失败发生在**脚本还没编译完**的时候。
断言消息看起来像"沙箱没拦住"，实际是"编译器先炸了"，**不要照着断言去改沙箱代码**。

本仓 `z-util-expr-groovy` 传递进来的是 `org.codehaus.groovy:groovy:3.0.9`。
本工程栈是 Spring Boot 2.7 = Java 8 目标（`maven.compiler.source/target = 8`），
本机另备有 Corretto 8 与 Corretto 17，两者都能过。

## 为什么你会莫名其妙地在 23 上跑

本机 `JAVA_HOME` 是**空的**，而 `mvn` 是 Homebrew 的 maven：

    $ echo "JAVA_HOME=$JAVA_HOME"
    JAVA_HOME=
    $ java -version
    openjdk version "17.0.19" 2026-04-21 LTS        ← PATH 上的 /usr/bin/java
    $ mvn -v | grep "Java version"
    Java version: 23.0.2, vendor: Homebrew          ← maven 真正用的

Homebrew 的 maven 在 `JAVA_HOME` 为空时回落到它自带的 openjdk 23。
**`java -version` 和 `mvn -v` 说的不是同一个 JVM** —— 这一点很容易看漏，
而 surefire fork 用的是 maven 那个，于是测试就落在 23 上了。

## 正确跑法

    JAVA_HOME=/Users/zifang/Library/Java/JavaVirtualMachines/corretto-17.0.19/Contents/Home \
      mvn -o clean test

实测：ScriptSandboxTest 15/15 绿，全仓 `Tests run: 15 + 21`，**BUILD SUCCESS**。

换用 Corretto 8 应当同样成立（本工程 target 就是 8），本机路径：

    /Users/zifang/Library/Java/JavaVirtualMachines/corretto-1.8.0_492/Contents/Home

## 排查这个症状的通用判别式

报 `Unsupported class file major version NN` 时，先算一下 NN 对应几：
**NN - 44 = Java 主版本**（67-44=23，61-44=17，52-44=8）。
再 `mvn -v` 确认测试 JVM 到底是几，别看 `java -version`。

同一类"环境制造假缺陷"的兄弟案例见各仓 `_doc/`：
`z-bot/_doc/005_testing/MCP-TEST-ENV.md`（缺 Python MCP SDK）、
`z-skill/_doc/TEST-CORPUS-DEP.md`（缺 playwright-core 语料）。
共同形状：**一批测试集体失败、且失败信息与断言内容无关** ⇒ 先查环境，别改产品代码。
