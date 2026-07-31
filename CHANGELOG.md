# Changelog

本项目的重要变更记录在此文件中。

## [0.1.0] - 2026-08-01

首个可用 MVP 版本。

### 新增

- 使用 SQLite 持久化多个 HTTP、HTTPS 和 SOCKS5 代理配置。
- 支持代理配置的新增、编辑、删除和当前选择切换。
- 实时检测、开启和关闭 APT 与 NPM 的 HTTP/HTTPS 代理。
- 当前代理变化时自动重应用已开启组件，并在失败时尽力回滚。
- 统一的参数化命令执行、超时控制、结构化错误提示和文本操作日志。
- 自包含 Java/JavaFX Runtime 的 Ubuntu 24.04 amd64 deb 安装包。
- 正式应用图标、桌面菜单入口和 MIT 许可证。

### 已知限制

- SOCKS5 配置不能应用到 APT 或 NPM。
- 不执行代理连通性测试、`apt update` 或 npm 网络请求。
- 不支持 Git、Pip、Docker、Windows 或 macOS。
- NPM 仅管理用户级配置；项目配置、全局配置和环境变量可能覆盖它。
- APT 授权依赖系统提供的 PolicyKit 和 `pkexec`。

[0.1.0]: https://github.com/MoeQuickStudio/moequick-gate/releases/tag/v0.1.0
