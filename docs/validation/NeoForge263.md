# 1.0.0 Release 交付验证

日期：2026-11-06（Asia/Shanghai）。平台：NeoForge / Minecraft 26.3。

## 正式包

- 模组显示名称：我的伙伴非官方移植版。
- 加载器版本：1.0.0，发布阶段 Release；模组 ID 保持 my_companion。
- 本模组 `scripts/build-all.ps1 -Offline -Clean` 退出码 0；两版 BUILD SUCCESSFUL。
- 安装包及源码包的图片均逐字节匹配用户附件，未重绘、裁剪、缩放或修改。
- 图像为原始 1254×1254 PNG；SHA-256：`1b9fad1d3118e952d03dc4802f59b0d0d220a81b52e9568577dcc51ae4aed7c8`。
- Fabric icon 和 NeoForge 当前 iconFile/bannerFile 均引用 assets/my_companion/icon.png。NeoForge 不再使用弃用的 logoFile 字段。
- JAR Manifest 的 Implementation-Title 与 Implementation-Version 正确。
- 名称、版本、依赖约束、Java 17/25 字节码、入口、语言资源及客户端隔离检查通过。
- 正式包无 GameTest 入口或场地，来源图片资源在源码包中完整保留。

安装包：`NeoForge263\build\libs\[我的伙伴非官方移植版]my-companion-unofficial-1.0.0-NeoForge26.3.jar`

SHA-256：`1def53e16332e2b59a4c7138696c1179219b5e9b15ca5da5476383deb48017ad`

## 引擎回归

`./scripts/gradle.ps1 runGameTestServer -PenableGameTests --offline`：真实退出码 0，4 项（3 项自定义测试 + 1 项原版默认测试）通过。

```text
[21:20:50] [Server thread/INFO] [minecraft/GameTestServer]: All 4 required tests passed :)
```

加载器日志已实际识别 my_companion 1.0.0；NeoForge 同时识别中文名称。本次配置当前封面字段后的 NeoForge 日志不再出现 logoFile 弃用警告。

测试覆盖原有设置、持久化、权限、右键开菜单、低头靠近、压力板与绊线钩实际触发及直接免伤。NeoForge 同时保留自动生成、唯一绑定、召回、旧实体清理等生命周期测试。

## 验证边界

没有启动客户端，模组列表中封面的实际渲染与真实联网多人仍需人工验收；名称/版本由真实加载器日志确认，图片由元数据路径及包内容检查确认。复杂地形、跨维度往返与完整世界重启未重新人工验收。没有向外部平台上传或发布，没有自动接受 Minecraft EULA。

封面源：用户提供的 用户指定的原始 PNG；使用原始文件，未使用生成图。日志为 run-gametest-100release-console.log，构建日志位于本模组 build/release-100-build.log。
