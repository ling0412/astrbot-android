# AGENTS.md

本文件是本仓库（AstrBot 安卓客户端）的工程约定与设计契约，供 AI agent 与开发者阅读。
开始任何改动前请先读完本文件。沟通与注释默认使用中文。

---

## 0. 当前进度（重要）

- **核心功能基本完成**：登录/认证、会话、聊天（SSE 流式/推理/工具调用/引用来源）、附件、IM 历史、项目、大屏两栏、外观与字号设置。
- **认证**：账号密码登录 + dashboard **JWT**（API Key 方案已废弃并移除）。
- **Release 已开启 R8**（`isMinifyEnabled` + `isShrinkResources`）。
- 编译：
  ```bash
  cd <项目根>
  ./gradlew :app:assembleDebug        # 日常
  ./gradlew :app:assembleRelease      # R8（未配签名，产出未签名 APK）
  ```
- 联调需要一个可用的自建 AstrBot 实例（账号密码登录，可能开启 TOTP）；实例地址与账号属私有信息，勿写入仓库。
- ⚠️ 服务端 `/api/v1/chat` **强制要求 `session_id`**（缺省会返回 JSON 错误信封，不是 SSE）。新会话必须先 `GET /chat/sessions/new` 建会话再发。

---

## 1. 项目简介

- **名称**：`astrbot`（应用显示名）
- **包名 / applicationId**：`com.ling.astrbot`
- **定位**：AstrBot 的**聊天客户端**。连接单个 AstrBot 实例，使用内置 WebChat Agent 能力对话；另含「项目（Projects）+ 工作区文件浏览」与 IM 历史浏览。
- **非目标**：插件/MCP/Provider 等管理面板；多实例管理。
- **目标平台**：Android，`minSdk = 34`、`targetSdk = 37`，支持 Material You 动态取色与 M3 Expressive。
- **官方 logo**：`https://raw.githubusercontent.com/AstrBotDevs/AstrBot/master/docs/public/logo.png`

---

## 2. 已确认的决策（不要擅自更改）

| 项 | 决定 |
|---|---|
| UI 版本管理 | **Compose BOM** 统一版本；material3 由 BOM 提供 |
| 认证 | **账号密码登录（dashboard JWT）**，已移除 API Key |
| 2FA | 支持 TOTP；「信任此设备」默认开启（约 30 天免 2FA） |
| 登录态 | 存密码（Keystore 加密）+ JWT + 信任设备 cookie；401 自动重登 |
| 本地数据 | **在线 + Room 本地缓存历史** |
| 实例 | **仅单实例** |
| minSdk / targetSdk | **34 / 37** |
| Markdown | **multiplatform-markdown-renderer**；**流式与定稿都用它**（同一套 typography，避免切换跳变）；表格单元格自动换行；表格/链接样式自定义 |
| 自签名证书 | **不支持**（OkHttp 默认校验） |
| 导航 | 窄屏：底部 3 tab（聊天/历史/设置）+ **ModalNavigationDrawer**（会话/项目）；宽屏：**PermanentNavigationDrawer 两栏**。不用 `NavigationSuiteScaffold`（IME 原因见第 7 节） |
| 正文字号 | 设置页可调，仅作用于聊天正文；标题按固定比例跟随（`LocalBodyFontScale`） |
| 发布 | Release 开 R8（代码/资源裁剪），见 `app/proguard-rules.pro` |

---

## 3. 技术栈与版本

- Kotlin **2.3.21**、KSP **2.3.12**、AGP **9.4.1**、Gradle **9.8.1**、JDK 17/21
  - **AGP 9 使用内置 Kotlin**：不要应用 `org.jetbrains.kotlin.android`；KGP/KSP 版本在根 `build.gradle.kts` 的 `buildscript` 里显式提升。
  - 新 DSL：`compileSdk { version = release(37) }`、`minSdk { version = release(34) }`、`targetSdk { version = release(37) }`。
- Compose BOM **2026.09.00**（material3 1.4.0）
- Hilt、Retrofit 3 + OkHttp 5（**手写 SSE 解析**，未用 okhttp-sse）、kotlinx.serialization
- Room 2.8.5、DataStore
- Coil 3（`SingletonImageLoader`、`coil-network-okhttp`）
- **androidx.graphics:graphics-shapes 1.1.0**（M3E 形变加载指示器）
- 安全存储：**Android Keystore 自实现 AES/GCM**（`core:datastore` 的 `KeystoreCrypto`）
- 依赖下装：阿里云 Maven 镜像 + 华为云 Gradle 发行包，`gradle.properties` 配了下载超时。

---

## 4. 模块结构

```
app                        // 入口、NavHost（3 tab + 抽屉）、Application、Hilt 装配、主题挂载、登录守卫
core/
  common                   // TimeUtils、Username（Username 为遗留，见第 9 节）
  model                    // 领域模型（ChatSession/ChatMessage/ChatRef/ToolCall/AgentStats/ChatProject/Conversation/Auth…）
  designsystem             // M3 主题、色板、ExpressiveLoadingIndicator、LocalBodyFontScale
  network                  // Retrofit API、SSE 解析、认证(SessionManager/CookieJar)、错误映射
  database                 // Room（DAO/Entity/数据库）
  datastore                // 认证配置+JWT+外观设置（Keystore 加密）
data/
  chat                     // ChatRepository(SSE/上传/停止/再生/续订) + ChatSessionRepository(会话/历史)
  conversations            // IM 平台历史（只读）
  projects                 // 项目 + 工作区（ProjectRepository）
feature/
  chat                     // 聊天页 + ChatViewModel（含抽屉内容宿主在 app 层）
  sessions                 // 会话抽屉内容（会话/项目列表 + 项目详情 + 工作区浏览）
  conversations            // IM 历史浏览页
  settings                 // 设置/登录页（含外观、正文字号）
```

依赖方向：`feature -> data -> core`。`core:network` **依赖** `core:datastore`（读认证配置、存 JWT）。
UI 层不得直接依赖 `core:network` 的 DTO，须经 `data` 层映射为 `core:model` 领域模型。

---

## 5. AstrBot API 关键信息

### 5.1 认证（账号密码 → JWT）

```
POST /api/auth/login
  body: {"username","password","code"?,"trust_device_flag"?}
  成功: 200 {"status":"ok","data":{"token":"<JWT>","username":...}}
        + Set-Cookie: astrbot_dashboard_jwt, astrbot_totp_trusted_device
  需要 2FA: 401 {"message":"需要 TOTP 验证","data":{"totp_required":true}}
```
- JWT HS256，**有效期 7 天**；之后所有 `/api/v1/*` 用 `Authorization: Bearer <JWT>`。
- JWT 下 `auth.username` = dashboard 用户名 → 会话与 WebUI 共享。
- 信任设备 cookie 有效期 **30 天**，重登不自动续期。
- 统一信封：`{"status":"ok|error","message":...,"data":...}`。

### 5.2 两套历史体系（勿混淆）

- `/api/v1/chat/*` —— WebChat 内置 Agent，可读可写，聊天主通道。
- `/api/v1/conversations/*`、`/api/v1/sessions` —— 会话管理器历史，App 的「历史」tab。

### 5.3 常用接口

| 功能 | 方法与路径 |
|---|---|
| 登录 | `POST /api/auth/login` |
| 发送消息（SSE） | `POST /api/v1/chat` |
| 会话列表 | `GET /api/v1/chat/sessions?username=&page=&page_size=` ⚠️ 对象或数组，见 5.6 |
| **新建会话** | `GET /api/v1/chat/sessions/new?platform_id=webchat` |
| 会话详情+历史 | `GET /api/v1/chat/sessions/{id}` |
| 改名/删除 | `PATCH` / `DELETE /api/v1/chat/sessions/{id}` |
| 停止生成 | `POST /api/v1/chat/sessions/{id}/stop` |
| 断线续传（SSE） | `GET /api/v1/chat/runs/{run_id}/stream` |
| 重新生成 | `POST /api/v1/chat/sessions/{id}/messages/{message_id}/regenerate` |
| 附件上传 | `POST /api/v1/file`（multipart，字段名 `file`） |
| 附件下载 | `GET /api/v1/file?attachment_id=`（需 Bearer）|
| IM 会话列表 | `GET /api/v1/conversations` |
| IM 会话详情 | `GET /api/v1/conversations/{cid}?user_id=` |
| IM 会话筛选项 | `GET /api/v1/conversations/filter-options` |

### 5.4 项目 / 工作区（需 chat scope；JWT 身份）

| 功能 | 方法与路径 |
|---|---|
| 项目列表 | `GET /api/v1/chat/projects` |
| 新建 / 更新 / 删除 | `POST` / `PATCH` `{id}` / `DELETE` `{id}`，体 `ChatProjectRequest{title,emoji,description,workspace_type,workspace_path}` |
| 项目内会话 | `GET /chat/projects/{id}/sessions` |
| 加入 / 移出会话 | `POST /chat/projects/{id}/sessions/{sid}` / `DELETE /chat/projects/sessions/{sid}` |
| 列目录 | `GET /chat/projects/{id}/workspace/files?path=` → `{path, entries:[{name,path,type,size,readable}]}` |
| 读文本 | `GET /chat/projects/{id}/workspace/file?path=` → `{path,content,size}`（UTF-8，≤512KB） |
| 下载 | `GET /chat/projects/{id}/workspace/file/download?path=`（二进制流） |

- `workspace_type`：`session`（默认，回退默认会话工作区）/ `project`（项目独立目录）/ `custom`（自定义服务器路径）。
- 工作区接口**只读**（无写入/上传）。
- 服务端不支持时（404/405）给「当前服务端版本不支持项目功能」提示。

### 5.5 `POST /api/v1/chat` 要点

- **必带 `session_id`**（服务端 `build_chat_stream` 强制；否则返回非 SSE 错误信封）。App 对新会话先建会话再发。
- `username`（JWT 下服务端忽略）、`message`（字符串或消息段数组）、`flags{enable_streaming,enable_reasoning}`。
- 段类型：`plain`(text) / `image|file|audio|record|video`(attachment_id) / `reply`(message_id, selected_text)。
- 返回 `text/event-stream`。

### 5.6 SSE 事件格式

`data: {json}\n\n`，空闲 1s 发 `: heartbeat\n\n`。

| `type` | 说明 | 关键字段 |
|---|---|---|
| `run_started` | 运行开始（拿 run_id） | `data.run_id` |
| `run_snapshot` | 续订首帧，全量累积状态 | `data.{run_id,session_id,status,revision,content}` |
| `session_id` | 新建会话的 id | 顶层 `session_id` |
| `user_message_saved` | 用户消息落库 | `data.id` |
| `plain` | **文本增量；推理/工具调用也走这里** | `data`=文本, `streaming`, `chain_type` |
| `agent_stats` | Agent 运行统计 | `data`=对象（见下） |
| `message_saved` | 助手消息落库（**不含 refs**） | `data.id` |
| `error` | 出错 | `data`=错误串 |
| `end` | 生成结束 | — |

**按 `chain_type` 分流**：`reasoning`→思考过程；`tool_call`/`tool_call_result`→工具调用；null/空/`plain`→正文；其它忽略。

**`agent_stats` 结构**（`core/agent/response.py: AgentStats.to_dict`）：
```json
{"token_usage":{"input_other":..,"input_cached":..,"output":..,"total":..},
 "current_context_tokens":123,"start_time":1.2e9,"end_time":1.2e9,"time_to_first_token":4.8}
```
App 展示：输入（缓存/其他 Token）、输出 Token、首字时间、耗时（=end-start）。

**refs（引用来源）**：只存在于会话历史的助手消息 `content.refs.used[]`（含 `index/url/title/snippet/favicon`），正文用 `<ref>N</ref>` 标注。SSE 的 `message_saved` 不带 refs；App 在落库后按消息 id 从历史回填。

消息存储结构：用户 `{"type":"user","message":[parts]}`；助手 `{"type":"bot","message":[parts],"reasoning"?,"agent_stats"?,"refs"?}`。
`parts` 类型：`plain`(`text`)、`think`(`think`)、`image|record|file|video`(`attachment_id`)、`tool_call`(`tool_calls`)。

### 5.7 会话列表返回形状不一致（重要坑）

`GET /api/v1/chat/sessions` 可能是分页对象 `{"sessions":[...]}` 或 legacy 数组 `[...]`。App 在 `data/chat/ChatMappers.kt` 的 `parseSessions` 做了兼容解析，改这段务必保留两种。

### 5.8 IM 历史（conversations）

- 详情 `{user_id,cid,title,persona_id,history,created_at,updated_at,umo_info}`。
- `history` 是 LLM 上下文格式（JSON 字符串或数组），形如 `[{"role":"user","content":[...]},{"role":"assistant","content":[{"type":"think"},{"type":"text"}]},{"role":"_checkpoint"}]`。
- App：剥离 `<system_reminder>`、跳过 `_checkpoint`/非 user/assistant（`data/conversations/ConversationHistoryParser.kt`）。
- 详情页的思维链可折叠（「思考过程」）。

### 5.9 聊天配置文件（配置档案）

- 列表：`GET /api/v1/chat/configs` → `data.info_list: [{id,name,path}]`（`default` 为内置兜底）。
- 切换是**按会话（UMO）绑定**，不是每条消息的参数：`PUT /api/v1/config-routes/{umo}`，体 `{config_id}`；`config_id=default` 表示解除绑定。
- UMO 约定：`webchat:FriendMessage:webchat!{用户名}!{会话id}`（用户名即 JWT/dashboard 用户名）。
- 读取当前绑定：`GET /api/v1/config-routes` → `data.routing: {umo: configId}`（pattern 支持 `*`/空段通配）。
- App：`data/chat/ChatConfigRepository.kt`；选择在聊天页输入栏「+」按钮里切换。新会话的配置文件 id 持久化到 DataStore，建会话后先绑定再发送。

---

## 6. 关键数据流

### 6.1 聊天（SSE 流式）

```
ChatViewModel.send()
  → 若 currentSessionId == null：先 sessionRepository.createSession()（GET /chat/sessions/new）
  → ChatRepository.sendMessage(sessionId, text, attachments, replyToMessageId)  // POST /api/v1/chat
  → Flow<ChatStreamEvent>（手写 SSE 解析，flowOn(Dispatchers.IO)）
  → reduce 到 ChatUiState：
      session_id → 记录会话；run_started → 记录 runId
      plain(chain_type=null) → 追加正文；plain(chain_type=reasoning) → 追加推理
      tool_call / tool_call_result → 累计；agent_stats → 统计
      user_message_saved → 乐观用户消息临时 id 换正式 id
      message_saved → 流式内容「原地」落成正式助手消息
      end → 结束
  → finishRun：refreshSession(写 Room) + refreshSessions；并按 id 从历史回填 refs 与 createdAt（SSE 不带）
```
- **断线续传**：流在 `end` 前异常结束时用 `GET /chat/runs/{run_id}/stream` 续订（首帧 `run_snapshot`），最多 3 次。
- 停止 `POST .../stop`；再生 `POST .../regenerate`（仅对已有会话）。

### 6.2 认证

```
设置页登录 → SessionManager.login() → POST /api/auth/login
  ├─ 成功 → 存 JWT(加密)+过期 → LOGGED_IN → 登录守卫放行
  └─ totp_required → NEEDS_TOTP → 设置页显示验证码输入
拦截器加 Authorization: Bearer <token>；401 → SessionAuthenticator 用存好的密码自动重登；失败回登录页
```

---

## 7. UI / 设计规范

- **主题**：`MaterialTheme`（1.4.0 起 Expressive 默认；不要用已 internal 的 `MaterialExpressiveTheme`）+ 动态取色。`AstrbotTheme(bodyFontScale=…)` 通过 `LocalBodyFontScale` 下发正文字号。
- **启动图标**：纯矢量自适应图标（`drawable/ic_launcher_foreground.xml` + `ic_launcher_monochrome.xml`，白底 `ic_launcher_background`，品牌蓝 `brand_blue`）；`monochrome` 单独用单色剪影层以适配 Android 13+ 主题图标，切勿复用彩色前景。内容控制在 66dp 安全圆内。
- **系统栏图标**：`MainActivity` 用 `SideEffect` 按 App 主题设置 `isAppearanceLightStatusBars/NavigationBars`（而非系统深浅色）。
- **导航**：
  - 窄屏：底部 3 tab（聊天/历史/设置）+ 聊天页 `ModalNavigationDrawer`（内容为 `SessionsDrawerContent`：会话/项目分段、搜索、重命名、批量删除、项目编辑）。
  - 宽屏（`screenWidthDp ≥ 600`）：`PermanentNavigationDrawer` 两栏（左侧导航项 + 会话/项目列表；右侧内容），无底部栏。
  - 底层 IME 处理（窄屏）：`NavigationBar` 用 `offset { IntOffset(0, imeBottomPx) }` 随键盘下移，NavHost 底部留白 = `barHeight - imeBottom`；`enableEdgeToEdge()` 在 `super.onCreate()` 后调用，manifest `adjustResize`。
- **聊天页**：
  - 助手消息**无卡片底**，直接铺在页面上（左右留白：窄屏 8dp、宽屏 48dp，叠加列表 12dp）；用户消息为气泡（`primaryContainer`，`shapes.extraLarge`）。
  - 长按弹菜单（复制/引用），`indication = null`（无波纹）。助手正文下方一行小操作：重新生成 / 复制 / 详情（统计气泡）/ 时间（`HH:mm`）。
  - 正文用 `AssistantMarkdown`：流式与定稿同一套 Markdown 配置；定稿消息 `immediate=true` 同步解析（避免「气泡撑开」）。标题按正文固定比例缩放。
  - 引用来源：`<ref>N</ref>` → 对应域名的小药片链接；消息下方可折叠「引用来源」。
  - 滚动锚定：以「我的消息」为锚点；末尾自适应 spacer（回复高度未测量时记 null → spacer 0，避免回弹）；打开会话时先显示 M3E 加载指示器，列表布局/锚点滚动完成后再整体显示。
  - 输入栏「+」按钮：弹出菜单含「图片/视频、文件」附件入口，以及「配置文件（当前：…）」入口——点击弹出对话框切换配置文件（见 5.9）。
- **加载指示器**：`core:designsystem` 的 `ExpressiveLoadingIndicator`（graphics-shapes 形变+旋转）。
- **设置页**：账号登录 + 外观（主题/动态取色/正文字号：小1.0/标准1.2/大1.5/特大1.8）。宽屏内容限宽居中（登录 520dp、设置 480dp）。

---

## 8. 里程碑

- **M0** 骨架/主题/导航 ✅
- **M1** 网络层 + 设置 + 加密凭据 ✅（后改账号登录）
- **M2** 会话列表 + 新建/删除 + 缓存 ✅
- **M3** 聊天 SSE + Markdown + 停止/再生 ✅
- **M4** 附件上传/展示 ✅
- **M5** 推理/工具调用 + 引用回复 ✅
- **M6** 断线续传 ✅
- **M7** IM 历史浏览 ✅
- **M8** 会话/项目合并 + 抽屉/两栏布局 + 大屏适配 ✅
- **M9** 项目 CRUD + 工作区文件浏览 ✅
- **M10** 引用来源（refs）显示、正文字号、M3E 加载指示器、R8 ✅

---

## 9. 注意事项 / 已知坑

- **敏感数据**：`/api/v1/system-config` 会返回 dashboard 密码哈希、`jwt_secret`、`totp.secret`；`/api/v1/bots` 明文返回 telegram token。**绝不显示/记录/上报/提交**。
- **凭据存储**：密码、JWT、cookie 均用 `KeystoreCrypto`（AES/GCM）加密存储；`allowBackup=false`（不参与系统备份）；CookieJar 以 `domain|name` 为键、读取按 `Cookie.matches(url)` 过滤（避免把 JWT 发往第三方域）。
- **`session_id` 必填**：服务端强制；新会话先建会话（第 5.5 节）。
- **refs 仅历史有**：SSE 不带，落库后回填（第 5.6 节）。
- **`chain_type`**：推理/工具调用都走 `plain`（第 5.6 节）。
- **会话列表返回形状**：对象/数组都要兼容（第 5.7 节）。
- **自签名证书不支持**；实例证书过期时会报 `Chain validation failed`（服务端续期即可）。
- **2FA 频率**：信任设备 30 天不续期 → 约每月需输一次 TOTP。
- **Material3 Expressive**：部分 API 实验性；锁 BOM 版本，实验 API 明确 `@OptIn`。
- **构建**：用 `./gradlew`；若遇 KSP/Dagger 缓存异常（如 `*_Factory.java` 缺失 / `hilt_aggregated_deps`），删对应模块 `build/generated/ksp` 后重编。
- **R8**：release 已开启；反射/序列化相关的 keep 在 `app/proguard-rules.pro`。改动 DTO/序列化后务必真机跑 release 验证。
- **Room**：`version` + `fallbackToDestructiveMigration(dropAllTables=true)`，schema 变更会清空本地缓存历史（联网会重拉）。

---

## 10. 待办 / 精修候选（按优先级）

**已修复（本轮）**
- 安全：CookieJar 按 `domain|name` 存 + `matches(url)` 过滤 + Keystore 加密；`allowBackup=false`。
- 稳健：`SseParser`/`ChatMappers` 的 `jsonPrimitive` 全部改为 `as? JsonPrimitive` 安全取值；`normalizeBaseUrl`/登录 URL 默认 `https://`；`toUserError()` 重抛 `CancellationException`。
- 正确性：新会话 id 写回 `SavedStateHandle`（进程重建不丢）；登录成功后才 `saveAuth`；`logout()` 复位 `needsTotp`；`refreshSessions` 不再因解析失败清空缓存并清理孤儿消息；冷启动校验 JWT 过期并尝试重登；`openDirectory` 失败不再错位；`regenerate` 先移除旧回复。
- 清理：删除 `Username.kt`/`EmptyState.kt`/`ChatSessionsPageDto`/`getChatConfigs`+`ChatConfigsData`/`enterSelection`；移除未用依赖（`okhttp-sse`/`okhttp-logging`/`material3-adaptive-navigation-suite`/`security-crypto`/`paging*`/`room-paging` 及多处多余模块依赖）；`bodyFontScale` 默认值统一为 `DefaultBodyFontScale`。

**低（仍可清理）**
- 若干"写了没读"的字段：`AuthState.message`、`ChatMessage.sessionId`、`Conversation.displayName`、`ChatProject.resolvedWorkspacePath`、`WorkspaceFile.size` 等。
- 多个 core 模块仍有未直接 import 的 `core.common/core.model` 依赖（传递依赖带入）；`core.common/model` 的 `kotlinx-coroutines-android` 未用。
- `core/designsystem` 的 `material-icons-extended` / `ui-tooling*` 未用。
- `strings.xml` 的 `destination_sessions` 冗余。
- `chatMarkdownTypography/Colors/chatBodyTextStyle` 未 `remember`，每次重组重建。
- `AstrbotClientFactory.create()` 每次新建 OkHttp/Retrofit（可改为按 baseUrl 缓存）。
- 部分 DTO 字段未被消费（保留即可）。

**功能（未做）**
- 侧线线程 threads（`POST /chat/threads`，需选中文本 + 父消息 checkpoint）。
- 后台生成（前台 Service + 通知）。
- 测试（无 `src/test`/`src/androidTest`；已移除未用的测试依赖）。
- 深链/分享、通知、缓存清理。
