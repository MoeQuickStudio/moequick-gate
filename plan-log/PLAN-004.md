# Phase 4：组件代理实现计划

## 摘要

在 Phase 3 的代理配置管理基础上，实现 APT 与 NPM 的真实代理检测、开启、关闭及当前代理切换联动。“项目初始化”视为标题笔误。

本阶段完成最小可用的系统控制闭环：允许组件实现直接使用 `ProcessBuilder`，APT 通过 `pkexec` 按需授权；通用 `CommandUtil`、操作日志和完整系统集成仍保留到 Phase 5。

## 核心架构与接口

- 新增 `IProxy`，同步提供 `check()`、`enable(MoeProxy)`、`disable()`；失败统一抛出包含组件、原因和建议的 `ProxyOperationException`。
- 新增 `ComponentStatus` JavaFX Property 模型及状态枚举：`DISABLED`、`ENABLED_CURRENT`、`ENABLED_OTHER`、`UNAVAILABLE`、`ERROR`、`BUSY`。
- 新增 `ComponentStatusViewModel`，在后台线程执行组件检测和操作；新增 `MainViewModel`，组合现有 `ProxyListViewModel` 与 APT/NPM 状态并协调代理切换。
- `MainScene#create()` 保持公开签名不变，改为创建并注入 `MainViewModel`；窗口关闭时停止状态刷新线程。
- Controller 只绑定状态、转发操作和显示错误，不读取配置文件或启动进程。
- 不修改 SQLite schema，不保存组件实际状态；启动时必须重新检测。
- 不引入第三方依赖。组件内暂用可注入的窄范围进程执行适配器，Phase 5 再统一收敛为 `CommandUtil`。

## APT 与 NPM 行为

- APT 使用 `/etc/apt/apt.conf.d/99zz-moequick-gate`：
  - HTTP/HTTPS 代理均写入 `Acquire::http::Proxy` 和 `Acquire::https::Proxy`，URI 协议取自当前代理类型。
  - 关闭时写入 `DIRECT`，确保应用托管配置范围内保持直连；APT 官方支持该配置语义。[APT transport 文档](https://manpages.debian.org/unstable/apt/apt-transport-http.1.en.html)
  - 先生成权限受限的临时文件，再以参数数组执行 `pkexec /usr/bin/install` 原子替换目标文件；不拼接 Shell 命令。
  - 使用 `/usr/bin/apt-config dump` 检测最终有效配置；授权取消、工具缺失和写入失败分别反馈。
- NPM 使用官方用户级 `npm config`：
  - 开启时将 `proxy`、`https-proxy` 同时覆盖为当前 URI。
  - 关闭时将两项设为 `null`，不恢复接管前的代理值；保留其他 npm 配置。
  - 使用 `--location=user`，并尊重有效的 `NPM_CONFIG_USERCONFIG`。
  - 环境变量和项目级配置优先级更高；若它们仍使代理生效，状态显示为“其他配置正在生效”并提示用户处理，而不声称已强制修改终端环境。[npm 配置来源与代理选项](https://docs.npmjs.com/cli/using-npm/config/)
  - 未安装 npm 时显示“不可用”并禁用开关。
- 只允许 `HTTP`、`HTTPS` 应用于组件；SOCKS5 配置仍可保存和选择，但组件开关不可开启。
- 对用于系统配置的主机再次进行安全校验，拒绝引号、分号、控制字符或无法构成合法代理 URI的历史数据，防止配置注入。
- 普通检测超时为 10 秒；`pkexec` 授权最多等待 120 秒。所有操作均不阻塞 JavaFX 线程。

## 状态联动与 UI

- 启动、操作完成、窗口重新获得焦点以及每 5 秒刷新一次实际状态；忙碌时跳过重复刷新，关闭应用后停止执行器。
- 没有当前代理、协议为 SOCKS5、组件不可用或正在操作时禁用相应开关。
- 系统配置与当前代理一致时显示“已开启”；存在其他代理或 HTTP/HTTPS 值不一致时显示“其他代理正在生效”及实际值。
- 切换当前代理时，先向所有已开启组件应用新代理，全部成功后再持久化选择。
- 编辑当前代理时先重应用所有已开启组件，再保存数据库；删除当前代理时先关闭所有已开启组件，再删除。
- 已开启组件存在时，选择或把当前配置编辑为 SOCKS5 将被拒绝，数据库和系统配置保持原状。
- 跨组件操作采用尽力原子回滚：
  - 任一步失败时不提交数据库变更。
  - 已修改组件恢复到原当前代理；原来没有当前代理时恢复为关闭。
  - 回滚失败时刷新实际状态，并在同一错误对话框列出原始失败和回滚失败。
- 成功关闭采用用户选定的强制直连策略，不恢复接管前的代理值；事务失败触发的恢复仅用于保持操作原子性。
- 更新组件卡片文案、忙碌状态、错误详情和开关样式，移除 Phase 2/3 的演示提示。
- README 与 `PLAN-004` 记录功能、限制和以下系统准备命令；实施过程不代替用户安装系统软件：

```bash
sudo apt-get update
sudo apt-get install -y policykit-1 npm
command -v pkexec apt-config npm
```

## 测试与验收

- 单元测试覆盖状态模型、无当前代理、HTTP/HTTPS URI、SOCKS5 拒绝、启停、其他代理识别、超时和工具缺失。
- 使用临时目录和伪进程执行器测试 APT 文件内容、`DIRECT`、pkexec 参数边界、授权取消及配置注入防护；自动化测试绝不写入 `/etc` 或弹出授权窗口。
- 测试 NPM 用户级覆盖、关闭写入 `null`、环境覆盖提示、部分命令失败恢复及无关配置不受影响。
- MainViewModel 测试代理选择、当前项编辑、删除前关闭、多组件中途失败、数据库失败和回滚失败。
- UI 手工验收：
  - 启动后 APT/NPM 状态与实际配置一致。
  - APT 首次修改按需弹出授权，取消授权时保持原状态。
  - NPM 无需管理员权限即可启停。
  - 切换 HTTP/HTTPS 代理时，所有已开启组件自动更新。
  - 外部修改配置后最多 5 秒内更新界面。
  - SOCKS5、无 npm、无 pkexec、权限不足均显示明确原因和建议。
- 回归执行 `./gradlew clean test build`、`jlink`、镜像启动和 `jpackage`，确认 deb 仍可生成。
- 阶段结束时汇报修改文件、测试结果、真实系统验收结果和遗留问题，不实现 Phase 5 的日志与通用命令框架。

## 假设与边界

- 目标平台固定为 Ubuntu 24.04 x86_64，使用系统提供的 `apt-config`、`pkexec`、`install` 和可选的 `npm`。
- APT 只管理专用高优先级文件，不删除其他 APT 配置；关闭后该文件保留 `DIRECT`。
- NPM 控制范围为用户级配置，不修改项目 `.npmrc`、全局配置或环境变量。
- 不执行 `apt update`、npm 网络请求或代理连通性测试。
- 不增加操作历史、文本日志、通用 `CommandUtil`、后台常驻服务或其他组件支持。
