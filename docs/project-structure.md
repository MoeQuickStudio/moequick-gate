# MoeQuick Gate 项目结构文档

## 1. 项目包名

Java Package：

moe.div.moequickgate

Git Repository：

moequick-gate

---

# 2. 项目结构原则

项目采用 MVVM 架构。

目录职责明确：

* scene：界面定义
* controller：UI事件处理
* viewmodel：页面状态管理
* bean：数据模型
* proxy：代理业务接口
* proxy/impl：代理业务具体实现
* repository：数据访问
* database：数据库管理
* utils：公共工具

---

# 3. Java 代码结构

## App.java

应用入口。

职责：

* 启动 JavaFX
* 初始化主窗口
* 加载 MainScene

不负责：

* 业务逻辑
* 数据处理

---

# scene/

负责 JavaFX 页面。

当前：

MainScene.java

职责：

* 创建 Scene
* 加载 FXML
* 配置页面

不负责：

* 数据处理
* 系统操作

---

# controller/

负责界面事件。

当前：

MainController.java

ProxyCardController.java

职责：

* 响应用户操作
* 调用 ViewModel
* 更新界面状态

不负责：

* 数据库操作
* 系统命令执行

---

# viewmodel/

负责页面数据和状态。

当前：

MainViewModel.java

ProxyListViewModel.java

ComponentStatusViewModel.java

职责：

* 管理 UI 状态
* 调用 Repository
* 调用业务接口

---

# bean/

存放数据模型。

当前：

MoeProxy.java

ComponentStatus.java

采用 JavaFX Property。

作用：

* 保存数据
* 支持 UI 数据绑定

---

# proxy/

存放抽象接口。

当前：

IProxy.java

定义：

* 检测代理状态
* 开启代理
* 关闭代理

---

# proxy/impl/

存放具体业务实现。

当前：

APTProxyImpl.java

NPMProxyImpl.java

负责：

* 修改系统配置
* 执行系统操作

---

# repository/

数据访问层。

当前：

ProxyRepository.java

LogRepository.java

职责：

* 读取数据
* 保存数据
* 隔离数据来源

不直接暴露数据库细节。

---

# database/

数据库相关代码。

当前：

SQLiteHelper.java

负责：

* SQLite 初始化
* 数据库连接
* 表结构管理

---

# utils/

公共工具。

第一版：

CommandUtil.java

负责：

* 执行系统命令
* 获取执行结果
* 统一错误处理

只创建实际需要的工具。

---

# 4. Resources 结构

## fxml/

存放 JavaFX 布局文件。

当前：

main.fxml

proxy_card.fxml

---

## css/

存放界面样式。

当前：

style.css

---

## icon/

存放应用图标和界面资源。

---

# 5. 开发约束

开发过程中：

* 不随意增加目录
* 不创建无实际用途的工具类
* 不将业务逻辑写入 Controller
* 不将数据库逻辑写入 ViewModel
* 不让 UI 直接调用系统命令

保持 MVVM 分层。
