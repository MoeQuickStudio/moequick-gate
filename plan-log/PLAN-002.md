# Phase 2：UI 框架实现计划

## 摘要

在 Phase 1 JavaFX 骨架上完成可交互的主界面。采用上下分区：上方展示代理列表，下方展示 APT/NPM 状态。本阶段使用内存演示数据验证 UI，不接入数据库、ViewModel 或系统代理操作。

## 实施变更

- 将 `main.fxml` 扩展为基础窗口：
  - 顶部应用标题和当前代理摘要。
  - 中上部代理区域，包含新增按钮及可滚动/自适应的代理卡片列表。
  - 下部组件区域，以并排状态卡片展示 APT 和 NPM 的名称、状态及代理开关。
- 新增 `proxy_card.fxml`，显示代理名称、协议、地址端口、选中状态及编辑/删除按钮；启动时加载一个 `Clash 本机监听 / HTTP / 127.0.0.1:7890` 演示卡片并默认选中。
- 新增 `MainController` 和 `ProxyCardController`：
  - 卡片点击更新选中样式和当前代理摘要。
  - APT/NPM 开关只更新内存状态与“已开启/已关闭”文本，重启后复位。
  - 新增、编辑、删除按钮显示“将在 Phase 3 提供”的提示，不执行 CRUD。
  - Controller 只处理 UI 事件，不访问数据库、执行命令或包含代理业务逻辑。
- 新增基础 `style.css`，采用简洁浅色主题，统一标题、分区、卡片、按钮、选中态和开关样式；不引入图标库、动画或主题框架。
- 更新 JPMS 配置，向 `javafx.fxml` 开放 controller 包；更新 README 的开发进度、Phase 2 界面能力与未实现边界。

## 接口与边界

- `MainScene#create()` 保持现有公开接口，增加 CSS 资源加载及缺失资源的明确异常。
- 新增 FXML Controller 类型；`ProxyCardController` 仅暴露配置演示内容、设置选中状态和注册 UI 回调所需的最小方法。
- 不创建 `MoeProxy`、ViewModel、Repository、SQLite、`IProxy` 或系统实现；演示状态不得被后续阶段当作真实代理状态。

## 测试与验收

- 自动化测试确认 `main.fxml`、`proxy_card.fxml`、`style.css` 均可从模块路径解析，Controller 包已正确开放给 FXML。
- `./gradlew clean test build`、`jlink` 和 `jpackage` 回归成功。
- `./gradlew run` 后验证：
  - 900×600 基础窗口正常显示上下分区，无控件溢出。
  - Clash 演示卡片默认选中，点击后当前代理摘要和选中样式一致。
  - APT/NPM 可分别切换，状态文本与样式同步且互不影响。
  - 新增、编辑、删除均给出明确提示，不修改代理列表。
  - CSS、FXML 和 Controller 加载无异常，关闭窗口后进程正常退出。
- 阶段结束时汇报修改文件、测试结果和遗留问题，不进入 Phase 3。

## 默认约定

- Phase 2 按文档定义为“UI 框架实现”，“项目初始化”视为标题笔误。
- 界面功能优先，使用中文文案和浅色桌面布局；不处理响应式窄屏、国际化、高级动画或正式视觉资产。
- 所有演示状态仅存在于当前进程，用于验证 UI 交互。
