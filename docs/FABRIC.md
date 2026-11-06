# Fabric 平台维护

固定环境：Minecraft 1.20.1、Yarn 1.20.1+build.10、Fabric Loader 0.16.14、Fabric API 0.92.12+1.20.1、Loom 1.6.12、Java 17。

共用源码在 Fabric1201/src/main，客户端在 src/client，显式 GameTest 在 src/gametest。使用该目录 scripts/gradle.ps1；独立克隆的环境配置见 BUILDING.md。

必须保留 FoxEntity 父类 initGoals 初始化，再清空默认 Goal；父类 NBT 读取和 initialize 会再次添加狩猎目标，两处都要清理。NBT 键和实体注册 ID 保持稳定。damage 和 isInvulnerableTo 覆盖直接免伤，读取存档后重新设置 Invulnerable 标记。

CompanionControlHandler 用 PropertyDelegate 和原版菜单按钮包同步开关；客户端 Screen 不得放入共用源码。元数据为 src/main/resources/fabric.mod.json，icon 为 assets/my_companion/icon.png，图片来自仓库 branding/resources。

测试使用 runGameTest -PenableGameTests，之后普通 clean build。测试源码和测试场地属于可提交的构建输入，但正式 JAR 不得包含它们。不要提交 Minecraft 源码、运行数据或缓存。
