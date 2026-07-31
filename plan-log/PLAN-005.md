# Phase 5：系统集成实施计划

## 摘要

Phase 5 按项目文档定义为“系统集成”，“项目初始化”视为标题笔误。

在 Phase 4 已有 APT/NPM 控制能力上，统一命令执行、完善错误分类，并增加最大 200KB 的文本操作日志。本阶段不修改 SQLite schema、不增加组件、不安装 deb，也不进入 Phase 6 发布工作。

## 命令执行与错误处理

- 在 `utils` 包新增 `CommandExecutor`、`CommandUtil`、`CommandResult` 和 `CommandExecutionException`。
- 命令仅接受参数列表并直接交给 `ProcessBuilder`，不调用 Shell。stdout/stderr 并发、分离读取，每路最多保留 256KB；超出部分继续排空并标记截断。
- 超时或线程中断时终止主进程及子进程；普通命令使用 10 秒超时，APT 授权使用 120 秒超时。
- 移除 Phase 4 临时进程执行类型，APT/NPM 统一注入 `CommandExecutor`。
- APT 的 `pkexec` 退出码 126 归类为用户取消，127 归类为授权失败。
- `ProxyOperationException` 使用结构化错误类型区分工具缺失、配置非法、取消授权、权限不足、超时、进程失败、I/O 失败和中断。
- Controller 仅展示 ViewModel 提供的信息；技术详情限制长度并置于可展开、可复制区域。周期检测失败不反复弹窗。

## 操作日志

- 新增 `OperationLogEntry`、`LogRepository`、`TextLogRepository`、路径解析与创建工厂；SQLite `user_version` 保持为 1。
- 日志路径优先使用 `$XDG_STATE_HOME/moequick-gate/operations.log`，无效时回退到 `~/.local/state/moequick-gate/operations.log`。
- 新目录与新文件权限分别为 `0700`、`0600`；不修改已有路径权限。
- UTF-8 单行记录包含时间、组件、来源、操作、代理摘要、结果、耗时和清理后的原因。
- 记录开启、关闭、自动重应用和回滚；不记录周期检测、纯 CRUD、环境变量、完整命令或原始命令输出。
- 日志上限为 204800 字节。追加使用文件锁，超限删除最旧的完整记录，保证写入完成后不超过上限。
- 字段移除换行、制表符和控制字符，并限制单条记录长度。
- 初始化或运行时写入失败会切换为会话内无日志模式并显示持续警告，不回滚已成功的代理操作。数据库与日志警告可同时显示。

## MVVM、文档与边界

- `MainScene` 创建共享 `CommandUtil` 与日志 Repository，并注入 APT、NPM 和 `MainViewModel`。
- `MainViewModel` 记录组件变更与回滚并公开日志警告属性；`ComponentStatusViewModel` 继续只负责实时状态。
- 保持五秒刷新、HTTP/HTTPS 支持、SOCKS5 限制、先系统后数据库和尽力回滚规则。
- 更新 README，说明日志路径、容量、诊断与安全验收方式。
- 不增加第三方依赖、系统软件要求、日志查看/清空 UI、连通性测试、其他组件或 PolicyKit 规则。

## 测试与验收

- `CommandUtil` 测试 stdout/stderr 分离、UTF-8、非零退出、超时、线程中断、子进程终止、大输出截断和无 Shell 执行。
- APT/NPM 使用伪 `CommandExecutor` 测试参数边界、126/127、工具缺失、超时、配置错误及部分写入恢复。
- 日志使用临时目录测试 XDG 路径、权限、格式清理、多实例并发、200KB 清理和写入失败降级。
- ViewModel 测试组件变更日志、周期检测不记录、日志失败不影响业务结果，以及数据库与日志警告共存。
- 使用临时 `XDG_DATA_HOME`、`XDG_STATE_HOME` 和 `NPM_CONFIG_USERCONFIG` 验收，避免污染真实用户数据。
- 执行 `./gradlew clean test build`、`jlink`、镜像启动与 `jpackage`，检查 deb 内容但不安装。

## 阶段边界

- 不修改 `/etc/apt` 或真实用户 NPM 配置，不执行 `apt update` 或代理网络请求。
- 不安装生成的 deb，不验证卸载、桌面集成或发布流程。
- 阶段完成后等待确认，不进入 Phase 6。
