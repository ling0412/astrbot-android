# astrbot (Android)

AstrBot 的非官方 Android 聊天客户端。连接单个自建 AstrBot 实例，使用内置 WebChat Agent 对话，另含 IM 历史浏览与项目/工作区浏览。

![logo](https://raw.githubusercontent.com/AstrBotDevs/AstrBot/master/docs/public/logo.png)

> ⚡ **100% Vibe Coding**
>
> 本项目代码**全部由 AI 生成**。

---

## 这是什么

- 一个**聊天客户端**：连上你的 AstrBot 实例，和内置 WebChat Agent 聊天。
- **不是**管理面板：不做插件 / MCP / Provider / 多实例管理。

## 功能

- **认证**：账号密码登录（dashboard JWT），支持 TOTP 两步验证与「信任此设备」；401 自动重登。
- **聊天**：SSE 流式输出、思考过程（reasoning）、工具调用、引用来源（refs）、断线续传。
- **附件**：图片 / 视频 / 文件上传与展示，引用回复。
- **配置文件**：输入栏「+」里可切换会话使用的 AstrBot 配置文件。
- **会话**：列表 / 新建 / 重命名 / 删除，本地 Room 缓存历史。
- **IM 历史**：浏览 AstrBot 的会话管理器历史（只读）。
- **项目**：Projects + 工作区文件浏览（只读）。
- **界面**：Material 3 Expressive + 动态取色，支持深色模式、正文字号调节、平板/大屏两栏布局。

## 技术栈

Kotlin · Jetpack Compose (Material 3) · Hilt · Retrofit / OkHttp（手写 SSE 解析）· kotlinx.serialization · Room · DataStore(Keystore 加密) · Coil 3

- `minSdk 34` / `targetSdk 37`
- Release 开启 R8 代码/资源裁剪

## 模块结构

```
app                 入口、导航、Hilt 装配、主题
core/
  common model      基础工具与领域模型
  designsystem      M3 主题、色板、加载指示器
  network           API、SSE、认证
  database          Room
  datastore         认证配置 / JWT / 外观设置（加密）
data/
  chat conversations projects     仓库层（网络 + 本地缓存）
feature/
  chat sessions conversations settings     界面层
```

依赖方向：`feature -> data -> core`。

## 构建

```bash
./gradlew :app:assembleDebug     # 日常调试
./gradlew :app:assembleRelease   # R8（未配置签名，产出未签名 APK）
```

## 使用前提

- Android 14（API 34）及以上。
- 一个可用的**自建 AstrBot 实例**（账号密码登录，可能开启 TOTP）。
- 不支持自签名证书；实例证书过期会报 `Chain validation failed`。

## 致谢

- 感谢 [AstrBot](https://github.com/AstrBotDevs/AstrBot)（[AstrBotDevs](https://github.com/AstrBotDevs)）——官方组织。
- 官方文档：[docs.astrbot.app](https://docs.astrbot.app)
- Logo 版权归 AstrBot 官方所有。

## 声明

- 非官方项目，与 AstrBot 官方无关。
- 仅供学习和个人使用。
- 代码 100% 由 AI 生成，请自行评估风险后再使用。

## 许可证

本项目采用 [MIT License](LICENSE)。
