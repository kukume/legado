# Legado Reader for VS Code

对接 langge / 大灰狼融合书源接口的小说阅读扩展。

## 安装

1. 安装 `legado-reader-1.0.0.vsix`：命令面板 → **Extensions: Install from VSIX…**
2. 设置里搜索 `Legado Reader`，填写：
   - `legado.address`：API 地址，默认 `https://api.langge.cf`
   - `legado.cookie`：登录 Cookie（如 `qttoken=...; deviceId=...`）
3. 活动栏打开 **Legado Reader**，刷新书架后点书进入正文。

## 功能

- 书架、目录、正文、进度同步
- 侧栏阅读：返回、上一章 / 下一章、章节下拉（附近 10 章，可加载更多）
- 行内阅读：开启后，当前段显示在编辑器光标后面

行内翻段快捷键（避开 VS Code 默认绑定）：

| 动作 | Windows / Linux | macOS |
|---|---|---|
| 上一段 | `Ctrl+Alt+[` | `Control+Option+[` |
| 下一段 | `Ctrl+Alt+]` | `Control+Option+]` |

侧栏标题栏可开关行内阅读。也可在命令面板搜索「行内阅读」。

## 开发

```bash
cd vscode
npm install
npm test
npm run package
```
