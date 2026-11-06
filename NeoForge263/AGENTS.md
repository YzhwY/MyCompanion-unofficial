# NeoForge 26.3 开发约定

- Minecraft 26.3 / NeoForge 26.3.0.95 / ModDevGradle 2.0.148 / Java 25，详见 ../docs/NEOFORGE.md。
- 用 ./scripts/gradle.ps1 build 构建并检查退出码。NeoForge 资源工具另需 JDK 21，使用 CODEX_JDK21/CODEX_JDK25 或 JAVA_HOME_21_X64/JAVA_HOME_25_X64 指定。
- 不自动下载本机 JDK，不改全局环境。当前版本 API 不得与 Fabric Yarn 或旧 Forge 混用。
- 共用代码不加载客户端类；客户端逻辑放在 client 包并限制 Dist.CLIENT。元数据模板在 src/main/templates/META-INF/neoforge.mods.toml。
- GameTest: runGameTestServer -PenableGameTests；之后普通 clean build。不要自动接受 EULA，不把构建通过等同于游戏验收。
- 保留作者已编写的 README 正文，未填写的 README 继续保留大纲和待填内容。缓存、构建输出、日志和世界不提交。
