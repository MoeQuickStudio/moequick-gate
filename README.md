# MoeQuick Gate

MoeQuick Gate（萌快网络助手）是一款面向 Linux 开发者的网络代理配置助手。

当前开发进度：Phase 2 UI 框架实现。应用已提供代理列表、代理卡片以及 APT/NPM 状态区域的基础界面交互。

当前代理和组件状态均为内存演示数据，重启应用后会复位。代理 CRUD、数据存储和真实系统代理控制将在后续阶段实现。

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
