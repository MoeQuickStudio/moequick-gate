# MoeQuick Gate

MoeQuick Gate（萌快网络助手）是一款面向 Linux 开发者的网络代理配置助手。

当前开发进度：Phase 3 代理配置管理。应用已支持代理配置的新增、编辑、删除、选择和 SQLite 持久化。

APT/NPM 开关仍为内存界面演示，真实系统代理控制将在 Phase 4 实现。

## 开发环境

- Ubuntu 24.04 x86_64
- OpenJDK 21（需要包含 `javac`、`jlink` 和 `jpackage`）
- `fakeroot`、`binutils` 和 `dpkg-deb`

Ubuntu 安装命令：

```bash
sudo apt-get update
sudo apt-get install -y openjdk-21-jdk fakeroot binutils
```

项目通过 Gradle Wrapper 固定构建工具版本，无需单独安装 Gradle。

## 运行与测试

```bash
./gradlew run
./gradlew clean test build
```

## 数据存储

SQLite 数据库默认保存在：

```text
$XDG_DATA_HOME/moequick-gate/moequick-gate.db
```

未设置有效的绝对 `XDG_DATA_HOME` 时，使用：

```text
~/.local/share/moequick-gate/moequick-gate.db
```

首次建库会创建并选中一条 Clash 本机监听示例。数据库无法使用时，应用会显示警告并降级到不会持久化的内存模式。

开发时可使用临时目录，避免修改真实用户数据：

```bash
XDG_DATA_HOME=/tmp/moequick-gate-dev ./gradlew run
```

## 运行时镜像

生成包含 Java 和 JavaFX Runtime 的裁剪镜像：

```bash
./gradlew jlink
./build/image/bin/moequick-gate
```

## deb 打包验证

```bash
./gradlew jpackage
dpkg-deb --info build/jpackage/moequick-gate_0.1.0_amd64.deb
dpkg-deb --contents build/jpackage/moequick-gate_0.1.0_amd64.deb
```

Phase 1 只验证 deb 可以生成且内容正确。正式安装、卸载、桌面集成和发布测试属于 Phase 6。
