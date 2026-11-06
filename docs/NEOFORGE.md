# NeoForge 平台维护

固定环境：Minecraft 26.3、NeoForge 26.3.0.95、ModDevGradle 2.0.148、Gradle 9.2.1、Java 25；资源下载辅助工具另需 JDK 21。不是 Minecraft 1.20.1。

元数据模板在 NeoForge263/src/main/templates/META-INF/neoforge.mods.toml，属性在 gradle.properties。中文 mod_name 使用 Java Properties Unicode 转义，生成的 TOML 显示中文。

DeferredRegister 注册实体和菜单；属性使用 EntityAttributeCreationEvent；周期管理使用 ServerTickEvent.Post。客户端入口限制 Dist.CLIENT。26.3 UI 使用 GuiGraphicsExtractor/extractRenderState，不套用旧版 renderBg。

实体持久化使用 ValueInput/ValueOutput，玩家状态使用 SavedDataType/Codec。保留父类 registerGoals 初始化，并在读取存档及 finalizeSpawn 后清理默认狩猎目标。hurtServer 与带 ServerLevel 的 isInvulnerableTo 覆盖直接免伤，加载后设置永久无敌标记。

跨维度安全检查使用目标世界中的探测实体，避免拿旧世界的 PathfindingContext 缓存。图标使用 iconFile 和 bannerFile，不使用已弃用的 logoFile。

测试使用 runGameTestServer -PenableGameTests，之后普通 clean build。不要自动接受 EULA，不提交缓存、运行目录和生成的 Minecraft 源码。
