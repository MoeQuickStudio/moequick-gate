# MoeQuick Gate

MoeQuick Gate（萌快网络助手）是一款面向 Linux 开发者的网络代理配置助手。

当前开发进度：Phase 4 组件代理实现。应用已支持代理配置持久化，以及 APT/NPM 代理的实时检测、开启、关闭和当前代理切换联动。

组件控制支持 HTTP、HTTPS 代理；SOCKS5 配置可以保存，但暂不能应用到 APT/NPM。

## 开发环境

- Ubuntu 24.04 x86_64
- OpenJDK 21（需要包含 `javac`、`jlink` 和 `jpackage`）
- `fakeroot`、`binutils` 和 `dpkg-deb`
- `policykit-1`（APT 修改时按需授权）
- `npm`（仅使用 NPM 组件时需要）

Ubuntu 安装命令：

```bash
sudo apt-get update
sudo apt-get install -y openjdk-21-jdk fakeroot binutils policykit-1 npm
command -v pkexec apt-config npm
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

## 组件代理控制

APT 使用应用专属配置文件：

```text
/etc/apt/apt.conf.d/99zz-moequick-gate
```

开启 APT 代理时会通过 `pkexec` 弹出系统授权；应用不会以 root 身份启动，也不会执行 `apt update`。关闭时保留该文件并为 HTTP/HTTPS 写入 `DIRECT`，从而保持直连。

NPM 通过官方的用户级 `npm config --location=user` 管理 `proxy` 和 `https-proxy`。关闭时将两项设置为 `null`，不会恢复启用前的旧代理值，也不会修改项目级 `.npmrc`、全局配置或环境变量。如果环境变量中仍有代理，界面会显示“其他代理正在生效”。

查看实际状态：

```bash
apt-config dump | grep -i 'Acquire::.*::Proxy'
npm config get proxy --location=user
npm config get https-proxy --location=user
```

切换当前代理时，已经开启的组件会先应用新配置，成功后再保存选择。编辑当前代理采用同样规则；删除当前代理前会先关闭已开启组件。任一步失败时，应用会尽力恢复原代理并显示原因与处理建议。

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

当前阶段持续验证 deb 可以生成且内容正确。正式安装、卸载、桌面集成和 GitHub Release 仍属于 Phase 6。

## 当前边界

- 不进行代理网络连通性测试。
- 不支持使用 SOCKS5 控制 APT/NPM。
- 不包含操作日志和通用命令执行框架；这些内容属于 Phase 5。
- 不支持 Git、Pip、Docker 或其他组件。
