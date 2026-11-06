# 双版本维护

当前正式版本为 1.0.0 Release。构建环境见 [BUILDING.md](BUILDING.md)。

## 结构

- Fabric1201：Minecraft 1.20.1 / Fabric / Yarn / Java 17。
- NeoForge263：Minecraft 26.3 / NeoForge 26.3.0.95 / Java 25。
- branding/resources：两版共用的原始封面资源。
- scripts：独立构建、发布检查、源码导出。
- docs：平台维护、验证记录、发行说明和 GitHub 上传说明。

每版保留独立 API 适配，不强行共享 Minecraft 实体代码。新功能、参数与通用修复同时评估两版；两份 gradle.properties 的 mod_version 同步。Minecraft、Loader、Java 与依赖分别固定，不能仅修改版本标签即宣称兼容。

## 代码职责

| 类 | 职责 |
| --- | --- |
| MyCompanionMod | 实体、属性、生命周期与菜单注册 |
| CompanionFoxEntity | 主人保存、免伤、交互、Goal 注册和旧实体清理 |
| CompanionState / CompanionSettings | 玩家绑定与行为开关持久化 |
| CompanionManager | 生成、安全落点、远距离与跨维度处理 |
| FollowCompanionOwnerGoal | 基础跟随 |
| CrowdOwnerFeetGoal | 低头时靠近主人脚下 |
| TriggerNearbyTrapsGoal | 主动走向压力板和已连接绊线 |
| CompanionControlHandler / CompanionControlMenu | 服务端菜单校验与属性同步 |
| CompanionControlScreen / MyCompanionClient | 客户端面板和渲染 |

Goal 优先级：游泳 0、脚下 1、机关 2、跟随 3、环顾 4。跟随从 4 格开始、2 格停止；管理每 20 tick 执行，超过 16 格尝试安全召回。低头开始角为 60°、保持角为 50°。机关搜索主人 8 格内及上下 2 格，候选寻路有扫描和冷却限制，实际触碰机关，不直接修改红石。

面板用原版菜单消息。按钮 0 控制脚下行为，按钮 1 控制机关行为；服务器检查主人、权威绑定、世界、存活、非旁观和距离。面板打开暂停调皮行为。设置按玩家保存，替换伙伴后仍生效。

## 存档约束

保持 my_companion、my_companion:companion_fox、Owners、Settings、CompanionOwner 标识稳定。设置字段 CrowdFeet 和 TriggerTraps 缺省为 true。Fabric 状态文件为主世界 data/my_companion_owners.dat，NeoForge 为 data/my_companion/owners.dat。

当前未加载伙伴会被补充，旧实体再次 tick 时检查权威 UUID 并清理。因此添加背包或经验前必须先解决完整状态保存与唯一性。两种游戏版本不提供世界互转；维护存档前停服备份。

无敌覆盖伤害入口，不封锁 discard/remove，以保留归属清理、卸载和跨维度生命周期。0.2.1 起旧存档读取时也启用原版无敌标记。

## 品牌、发布与回归

封面只能使用用户指定的图片，原图及 SHA-256 在 branding 中。Fabric icon、NeoForge iconFile/bannerFile 和 JAR Manifest 均由发布检查验证。不要擅自生成替代图。

发布顺序：修改两版与版本配置 → 相关测试 → clean build → 发布包检查 → 更新 CHANGELOG 和验证记录 → 由作者决定并上传 Release。README 主体由作者编写，保留已填写内容。原创代码采用 MIT，根目录 LICENSE、两版元数据和包内许可证须一致；保留第三方许可。不添加 GitHub Actions。

至少验证：生成和唯一性、跟随和召回、跨维度、多人归属、设置持久化、面板权限、脚下行为、实际压力板/绊线输出、免伤与旧伙伴清理。未实测内容必须明确标记，不能用构建或单一平台结果替代其他验收。
