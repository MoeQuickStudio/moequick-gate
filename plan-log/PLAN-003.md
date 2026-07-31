# Phase 3：代理配置管理实施计划

## 摘要

将 Phase 2 的内存演示界面接入 MVVM 与 SQLite，实现代理配置的新增、编辑、删除、选择和持久化。数据库不可用时降级到带明显警告的内存模式；APT/NPM 仍不执行真实系统操作。

## 数据与状态

- 使用 sqlite-jdbc 3.53.1.0，并以 JavaFX Property 实现 `MoeProxy`。
- 数据库位于 XDG 用户数据目录，使用 v1 schema 保存代理配置和当前选择。
- 首次建库写入并选中 Clash 示例；空列表新增第一项时自动选中。
- 允许重复名称；删除当前项后清空选择。
- 初始化失败时降级到带示例数据的内存 Repository，并持续提示更改不会保存。

## UI 与边界

- Controller 通过 `ProxyListViewModel` 调用 Repository，不直接访问 SQLite。
- 使用同一模态表单完成新增和编辑，并在保存前校验名称、主机、端口和协议。
- 删除操作需要确认；不进行网络连通性测试。
- 操作历史、文本日志和真实 APT/NPM 代理控制留到后续阶段。

## 验收

- 自动化测试覆盖 Bean、校验、SQLite CRUD、选择持久化和内存降级。
- 使用临时 XDG 目录验证 UI 和首次建库，不污染真实用户数据。
- 构建、jlink、镜像启动和 jpackage 全部通过。
