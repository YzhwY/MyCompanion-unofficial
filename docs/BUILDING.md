# 从源码构建

本仓库独立包含两个 Gradle 工程，封面在 branding/resources 中共用。无需复制公用开发工具或任何 Minecraft 世界。

## 环境

| 工程 | 游戏 | 主 JDK | 辅助 JDK | Gradle Wrapper |
| --- | --- | --- | --- | --- |
| Fabric1201 | 1.20.1 | 17 | 无 | 8.6 |
| NeoForge263 | 26.3 | 25 | 21 | 9.2.1 |

使用已安装的 JDK；本地脚本不会自动安装 JDK。需要 Python 3.11+ 运行发布检查和源码导出脚本；这些 Python 脚本只使用标准库。PowerShell 入口要求 PowerShell 7。

## 独立克隆：PowerShell

将以下环境变量设为你本机实际的 JDK 安装目录，仅需在当前终端设置：

```powershell
$env:JAVA_HOME_17_X64 = '<JDK 17 目录>'
$env:JAVA_HOME_21_X64 = '<JDK 21 目录>'
$env:JAVA_HOME_25_X64 = '<JDK 25 目录>'
.\scripts\build-all.ps1 -Standalone
```

只构建一版：

```powershell
.\scripts\build-all.ps1 -Standalone -Edition Fabric1201
.\scripts\build-all.ps1 -Standalone -Edition NeoForge263
```

初次需要联网下载 Gradle 和依赖。依赖已缓存可添加 -Offline；发布前可添加 -Clean。脚本只对构建进程设置环境，结束后恢复。可通过 GRADLE_USER_HOME 指定已有缓存；未指定时独立模式使用本仓库的 .gradle-user-home（已忽略）。

仓库位于原公用开发工作区时，平台入口仍可复用公用工具和缓存；-Standalone 可强制测试独立模式。

## 直接使用 Wrapper

Windows 在相应平台目录配置好 JAVA_HOME 后运行：

```powershell
.\gradlew.bat build
```

Linux/macOS 在相应平台目录配置 JAVA_HOME 后运行：

```sh
bash ./gradlew build
```

NeoForge 直接 Wrapper 构建还需要把 CODEX_JDK21 和 CODEX_JDK25 分别指向两个 JDK，供固定版本 ModDevGradle 的工具链发现使用。也可以运行 PowerShell 7 的仓库入口自动选择这些变量。

## 测试与交付

```powershell
.\Fabric1201\scripts\gradle.ps1 runGameTest -PenableGameTests
.\NeoForge263\scripts\gradle.ps1 runGameTestServer -PenableGameTests
.\scripts\build-all.ps1 -Clean
python .\scripts\verify-releases.py
```

普通 build 不自动执行 GameTest。测试后须 clean build，避免测试入口进入发布包。安装包与源码包位于每个工程 build/libs，源码包不能安装。检查脚本输出 build/release-manifest.json，可用 --edition 单独验证某一版。

本仓库不使用 GitHub Actions。通过上述命令在本地构建和检查；正式安装包与源码 JAR 均包含根目录的 MIT LICENSE，两版元数据声明同步为 MIT。

构建、引擎测试和客户端人工验收是不同层级。不要自动接受 Minecraft EULA。
