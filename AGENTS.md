# MyCompanion 仓库开发约定

- 本仓库只包含“我的伙伴非官方移植版”，同时维护 Fabric1201 和 NeoForge263。先读 docs/BUILDING.md 与 docs/MAINTENANCE.md。
- 两版功能、设置及 mod_version 默认同步；Minecraft、Loader、Java 与 API 版本独立，不得混用。
- 使用各平台 scripts/gradle.ps1 或 scripts/build-all.ps1。位于原公用工作区时复用公用工具；独立克隆时使用本仓库 Wrapper 与已安装的 JDK。不得写死作者电脑路径、自动下载本机 JDK 或修改全局 Java/Gradle 设置。
- 修改后构建并检查真实退出码。GameTest 显式开启；测试后 clean build，正式包不能含测试类或测试场地。不自动接受 EULA。
- 保留 mod ID my_companion、实体 ID 与旧存档兼容。客户端代码按平台侧别隔离。
- 封面使用 branding/resources/assets/my_companion/icon.png 中用户提供的原图。未经要求不重绘、替换或修改图片；发布校验比较原图 SHA-256。
- README 正文由作者编写。保留作者已填写内容，不得恢复为大纲或擅自改写；未填写的子目录 README 继续保留待填提示。技术说明写入 docs。
- 原创代码采用根目录 LICENSE 的 MIT 许可证；两版元数据和发布包同步。保留第三方许可；封面继续使用用户提供的原图。不添加 GitHub Actions。
- build、缓存、run、日志、存档、凭据和发布 JAR 不提交；Gradle Wrapper JAR 与测试场地 NBT 属于可提交的构建输入。
- 源码上传包由 scripts/export-source.py 生成，不能直接压缩整个工作目录。不得擅自推送 GitHub 或发布 Release。
