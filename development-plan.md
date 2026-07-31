# MoeQuick Gate 开发计划文档

## 1. 开发模式

MoeQuick Gate 采用阶段式开发模式。

开发流程：

阶段规划

↓

Codex 实施

↓

检查结果

↓

确认进入下一阶段

每个阶段完成后，Codex 必须汇报：

* 完成内容
* 修改文件
* 测试结果
* 当前问题

未经确认，不自动进入下一阶段。

---

# 2. 开发阶段

## Phase 1：项目初始化

目标：

建立基础开发环境。

任务：

* 创建 JavaFX 项目
* 配置 Maven/Gradle
* 创建项目目录结构
* 创建基础 Package
* 创建 App.java
* 验证程序启动

完成标准：

应用可以正常启动并显示基础窗口。

---

# Phase 2：UI 框架实现

目标：

完成基础界面。

任务：

* 创建 MainScene
* 创建 FXML 文件
* 创建 Controller
* 创建基础 CSS
* 完成主窗口布局

包含：

* 代理列表区域
* 组件状态区域

完成标准：

界面可以正常显示和交互。

---

# Phase 3：代理配置管理

目标：

实现代理列表管理。

任务：

* 创建 MoeProxy Bean
* 集成 SQLite
* 创建 Repository
* 实现代理 CRUD

支持：

* 添加代理
* 编辑代理
* 删除代理
* 选择代理

完成标准：

用户可以管理自己的代理列表。

---

# Phase 4：组件代理实现

目标：

实现核心功能。

任务：

创建：

IProxy

实现：

* APTProxyImpl
* NPMProxyImpl

支持：

* 检测代理状态
* 开启代理
* 关闭代理

完成标准：

APT 和 NPM 可以被 MoeQuick Gate 控制。

---

# Phase 5：系统集成

目标：

完善系统交互。

任务：

* 实现 CommandUtil
* 接入 ProcessBuilder
* 接入 pkexec
* 完善错误处理
* 添加操作日志

日志要求：

* 文本格式
* 最大 200KB

完成标准：

系统操作稳定，并能反馈错误原因。

---

# Phase 6：发布

目标：

生成可安装版本。

任务：

* 创建 deb 软件包
* Ubuntu 24.04 安装测试
* 完善 README
* 创建 GitHub Release

版本：

v0.1.0

完成标准：

用户可以通过 deb 安装 MoeQuick Gate。

---

# 3. 测试要求

第一版加入自动化测试。

使用：

JUnit

测试范围：

* Bean 数据
* Repository
* CommandUtil
* 核心业务逻辑

---

# 4. 代码质量要求

代码遵循 Java 标准规范。

要求：

* 类名使用 PascalCase
* 方法名使用 camelCase
* 常量使用大写

代码注释：

采用中文 + 英文双语。

例如：

中文说明功能。

English description for international contributors.

---

# 5. 异常处理要求

采用完整异常处理。

错误信息需要包含：

* 操作失败
* 失败原因
* 解决建议

例如：

APT 代理配置失败：

原因：

权限不足。

建议：

重新授权。

同时记录日志。

---

# 6. UI 开发要求

第一版：

功能优先。

要求：

* 布局正确
* 操作流程完整
* 状态显示正确

暂不要求：

* 高级动画
* 复杂视觉效果
* 精细主题系统

后续版本再优化 UI。

---

# 7. 开源计划

MoeQuick Gate 第一版目标：

GitHub 开源。

开发过程中：

同步更新：

* README
* 开发进度
* Release

版本规划：

v0.1.0：

第一个可用 MVP。

---

# 8. Codex 开发约束

Codex 开发时：

必须：

* 先阅读项目文档
* 先查看已有代码
* 再进行修改

禁止：

* 自行扩大需求
* 引入未规划框架
* 修改架构设计
* 跳过阶段

每个阶段结束：

等待确认。

