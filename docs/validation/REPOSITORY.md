# GitHub 源码整理验证

日期：2026-11-06（Asia/Shanghai）。模组版本：1.0.0。

## 本次变更

- 两个平台源码、共同封面、Gradle Wrapper、构建与检查脚本组成独立仓库。
- 维护、构建、平台差异和发行记录集中到 docs；保留作者完成的根目录 README，未填写的子目录 README 保留大纲。
- 增加 Git 忽略规则、换行规则和源码导出入口；按作者要求移除 GitHub Actions。
- 原创代码采用 MIT，沿用目标仓库已有 LICENSE；同步两版许可证元数据，安装包和源码 JAR 均附带 LICENSE。
- 玩法源码、原始封面及正式版本号保持原样；没有修改公用开发工具或其他模组。

## 实际检查

- 源码 ZIP 解压到独立目录后，强制 Standalone 模式，使用其自身脚本与 Wrapper 分别 clean build：Fabric1201、NeoForge263 均成功。
- 独立构建显式使用已安装的 JDK 17 / 21 / 25，并复用已有依赖缓存进行离线检查；未调用原公用构建脚本。
- 原工作区入口 scripts/build-all.ps1 -Offline -Clean 退出码 0，两个平台均构建成功，统一发布检查通过。
- 在解压副本初始化临时 Git 仓库检查上传清单：两个 Wrapper JAR 均可提交；build、Gradle 缓存、游戏运行目录和日志均被忽略。
- 发布检查验证图标原图哈希、两版名称与版本、Manifest、Java 字节码和发布包无 GameTest 内容。
- MIT 更新后重新 clean build 两版，检查元数据 license 为 MIT、安装包和源码 JAR 中许可证与根目录相符。
- 导出包检查 ZIP 完整性及 SOURCE-MANIFEST.json 中的逐文件 SHA-256。

## 验证边界

本次验证环境为本地 Windows；离线独立构建复用了已下载的依赖，没有验证首次联网下载或其他操作系统。仓库不使用 GitHub Actions。此次整理没有重新进行游戏行为或客户端界面验收；已有平台记录单独保留。

作者指定目标仓库 YzhwY/MyCompanion-unofficial 并授权上传源码。上传以保留远程现有提交为前提；不自动创建 Release 或版本标签。README 正文仍由作者编写。
