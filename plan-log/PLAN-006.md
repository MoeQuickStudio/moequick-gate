# Phase 6：发布与安装验收实施计划

## 摘要

Phase 6 按开发计划定义为“发布”，“项目初始化”视为标题笔误。目标是生成并验收 Ubuntu 24.04 x86_64 可安装的 v0.1.0 deb，并准备 GitHub Release。

## 发布配置

- 应用版本保持 `0.1.0`，Git 标签固定为 `v0.1.0`。
- 添加 MIT 许可证和 MoeQuickStudio 原创应用图标。
- deb 使用 `moequick-gate` 包名、`net` Section、MoeQuickStudio Vendor 和 `linmo456@hotmmail.com` Maintainer。
- 安装包声明 `policykit-1` 依赖，npm 保持可选；包含桌面入口、图标、许可证及完整 Java/JavaFX/SQLite Runtime。
- 不添加自定义 PolicyKit 规则，安装脚本不修改 APT/NPM 配置或用户 XDG 数据。

## 文档与发布资产

- README 记录下载校验、安装、升级、启动、卸载、数据保留规则和已知限制。
- CHANGELOG 记录 v0.1.0 功能与边界。
- 使用隔离 XDG 目录启动实际应用并采集不含私人数据的 900×600 截图。
- 发布资产为 `moequick-gate_0.1.0_amd64.deb` 及同名 `.sha256` 文件。

## 验收

- 执行 `clean test build`、`jlink`、镜像启动与 `jpackage`。
- 使用 `dpkg-deb` 检查元数据和内容，使用 `desktop-file-validate` 检查桌面入口，并运行 `lintian`。
- 安装、APT 实际配置、卸载等需要 sudo 的步骤由用户执行；实施过程仅提供安全命令。
- NPM 功能验收使用临时 `NPM_CONFIG_USERCONFIG`；APT 验收前备份专用配置，完成后恢复。
- 不执行 `apt update`、npm 网络请求或代理连通性测试。

## 发布

- 全部验收通过后提交并推送 `main`，再创建并推送 annotated tag `v0.1.0`。
- 用户在 GitHub 网页创建公开 Release，上传 deb 和 SHA-256 文件；不使用 GitHub CLI。
- Release 下载校验完成后 Phase 6 才最终完成。
