# Legado Reader

IDE 小说阅读插件，对接 langge / 大灰狼融合书源接口：书架、目录、正文、进度同步。

本仓库同时包含两个客户端：

| 目录 | 平台 |
|---|---|
| [`IntelliJ/`](IntelliJ/) | IntelliJ IDEA 插件 |
| [`vscode/`](vscode/) | Visual Studio Code 扩展 |

原项目：https://github.com/nancheung/legado-reader

合并到 `master` / `main` 后，GitHub Actions 会覆盖发布 [latest Release](https://github.com/kukume/legado/releases/latest)，同时附上：

- IntelliJ 插件 zip
- VS Code 扩展 vsix

## VS Code 扩展

1. 从 [Releases](https://github.com/kukume/legado/releases/latest) 下载 `legado-reader-*.vsix`（也可在 `vscode/` 下执行 `npm install && npm run package`）。
2. VS Code / Cursor：`Extensions` → `⋯` → **Install from VSIX…**，选中该文件。
3. 打开设置搜索 `Legado Reader`，填入 **API 地址** 与 **Cookie**。
4. 活动栏打开 Legado Reader 侧栏，刷新书架后即可阅读。

行内阅读开启后，正文会显示在光标后面。上下翻段快捷键：

- **上一段**：`Ctrl+Alt+[`（macOS：`Control+Option+[`）
- **下一段**：`Ctrl+Alt+]`（macOS：`Control+Option+]`）

这两个组合避开了 VS Code 默认的缩进（`Ctrl+[` / `Ctrl+]`）和选区扩展（`Shift+Alt+←/→`）。

## IntelliJ 插件

见 [`IntelliJ/README.md`](IntelliJ/README.md)。在 `IntelliJ/` 目录执行 `./gradlew buildPlugin`。
