# Fabric 1.20.1 开发约定

- Minecraft 1.20.1 / Fabric / Yarn 1.20.1+build.10 / Java 17，详见 ../docs/FABRIC.md。
- 用 ./scripts/gradle.ps1 build 构建并检查退出码；独立环境可用 JAVA_HOME_17_X64 指定 JDK。
- 客户端专用类在 src/client/java，共用代码不可引用 net.minecraft.client.*。保持元数据、入口、Loom mod ID 与资源命名空间一致。
- API 以固定版本源码为准；不直接套用其他游戏版本方法。
- GameTest: runGameTest -PenableGameTests；之后普通 clean build。不要自动接受 EULA，不把构建通过等同于游戏验收。
- 保留作者已编写的 README 正文，未填写的 README 继续保留大纲和待填内容。缓存、构建输出、日志和世界不提交。
