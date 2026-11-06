# GitHub 维护

## 源码内容

仓库：[YzhwY/MyCompanion-unofficial](https://github.com/YzhwY/MyCompanion-unofficial)，默认分支 main。

仓库根目录就是 MyCompanion，包含两个独立平台工程、共同封面、脚本、技术文档和许可证。不要上传整个公用开发工作区。仓库不使用 GitHub Actions，构建与发布检查在本地完成。

导出经过筛选的源码压缩包：

```sh
python scripts/export-source.py
```

输出在 build/github，解压后可作为独立仓库使用。导出包含两个平台的 Gradle Wrapper JAR，不包含编译产物、依赖缓存、世界、日志、凭据或安装包。安装包应作为 GitHub Release 附件，独立于源码提交。

## 维护约定

- README 正文由作者维护，保留已填写内容；未填写的子目录 README 继续保留大纲。
- 原创代码使用根目录的 MIT LICENSE；两版元数据、安装包和源码 JAR 中的 LICENSE 保持一致。
- NeoForge 官方模板许可保留在 NeoForge263/TEMPLATE_LICENSE.txt；该许可说明的是模板部分。
- Minecraft 游戏素材等第三方内容的范围以 README 说明为准。封面继续使用 branding 中用户提供的原图，不重绘或替换。

## 更新源码

在仓库克隆中修改后，先按 BUILDING.md 构建和检查，再查看 git diff 和待提交文件。首次从源码包建立本地仓库时可使用：

```sh
git init -b main
git add .
git commit -m "Prepare 1.0.0 Release source"
git remote add origin https://github.com/YzhwY/MyCompanion-unofficial.git
git push -u origin main
```

已有远程提交时应先克隆或同步仓库，不能用新的独立提交覆盖现有历史。不要强制推送。上传源码不自动代表发布安装包、建立版本标签或发布 Release。

## 本地验证

本仓库不配置自动工作流。使用 scripts/build-all.ps1 和 scripts/verify-releases.py 完成本地构建与发布包检查。导出的 SOURCE-MANIFEST.json 记录逐文件 SHA-256。

本次本地构建与源码整理的检查范围见 [仓库验证记录](validation/REPOSITORY.md)。
