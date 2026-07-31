# Phase 1：项目初始化实施计划

## 摘要

以 Ubuntu 24.04、JDK 21、Gradle 为基线，建立可构建、可测试、可启动、可生成 deb 的 JavaFX 项目骨架。Phase 1 仅显示占位窗口，不实现正式 UI、数据库或代理业务。

## 实施内容

- 配置 Gradle Kotlin DSL 与 Wrapper：
  - Gradle 9.6.1
  - Java Toolchain 21
  - JavaFX 21.0.12，启用 `javafx.controls`、`javafx.fxml`
  - OpenJFX Gradle Plugin 0.1.0
  - Beryx JLink Plugin 4.0.2
  - JUnit Jupiter 5.11.4
- 建立标准源码与资源结构，并添加 `.gitignore`。只创建 Phase 1 实际使用的根包和 `scene` 包，不用空类占位未来的 controller、viewmodel、repository 等目录。
- 创建模块 `moe.div.moequickgate`：
  - `App` 作为唯一 JavaFX 入口，负责加载场景、设置标题与基础窗口尺寸。
  - `MainScene` 封装 FXML 加载并返回基础 `Scene`。
  - `main.fxml` 仅显示 “MoeQuick Gate” 占位内容；正式布局、Controller 和 CSS 留到 Phase 2。
  - FXML 加载失败时抛出包含资源路径和原因的明确启动异常。
- 修正文档中的非法包名约定：
  - `interface` 改为 `moe.div.moequickgate.proxy`
  - 后续实现类统一放入 `moe.div.moequickgate.proxy.impl`
  - 本阶段不创建 `IProxy` 或实现类。
- 添加 README，记录 Ubuntu 24.04/JDK 21 前置条件以及运行、测试、构建和 deb 验证命令。
- 配置 `jlink/jpackage`：
  - 包名 `moequick-gate`
  - 应用名 `MoeQuick Gate`
  - 版本 `0.1.0`
  - Linux 安装器类型 `deb`
  - deb 内置裁剪后的 Java/JavaFX Runtime，不依赖目标机器预装 JRE
  - Phase 1 只生成并检查安装包；安装、桌面集成和卸载测试保留到 Phase 6。

## 接口与结构

- 新增公开启动类 `moe.div.moequickgate.App`。
- 新增 `MainScene#create()` 场景创建入口。
- 新增 JPMS 模块描述，声明 JavaFX Controls/FXML 依赖及必要的 FXML 反射开放规则。
- 不引入 Bean、Repository、SQLite、ViewModel、Controller、系统命令或代理接口。

## 验证与验收

- `./gradlew clean test build` 成功。
- 自动化冒烟测试确认 `main.fxml` 可从类路径解析，模块和入口类配置有效。
- `./gradlew run` 能启动窗口，显示正确标题和占位内容，关闭窗口后进程正常退出。
- `./gradlew jlink` 能生成包含运行时的应用镜像，并通过镜像启动脚本打开窗口。
- `./gradlew jpackage` 能在 Ubuntu 24.04 x86_64 生成 `.deb`。
- 使用 `dpkg-deb --info` 和 `dpkg-deb --contents` 验证包名、版本、启动器及内置运行时；本阶段不执行系统安装。
- 阶段结束时汇报完成内容、修改文件、测试结果和遗留问题，不进入 Phase 2。

## 假设与环境约束

- 当前环境是 Ubuntu 24.04 x86_64，已有 `dpkg-deb` 和 `fakeroot`，但尚未安装 Java/Javac/Gradle/jpackage；实施前需安装 `openjdk-21-jdk`，Gradle 通过 Wrapper 使用。
- 当前 `.git` 目录为空且只读，尚不能进行文档要求的阶段提交；代码实施不依赖 Git，但提交前需要恢复或重新初始化有效的 Git 元数据。
- deb 的正式安装验证、应用图标、桌面菜单质量检查和 Release 发布仍属于 Phase 6。
