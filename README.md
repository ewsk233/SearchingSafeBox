# SearchingSafeBox

SearchingSafeBox 是 Searching 插件的附属插件，用于给指定 `nodeTypeId` 的搜刮箱增加“保险箱 / 破译开锁”流程。

它不会修改 Searching 主插件源码，而是监听 Searching 的 `SearchingPreOpenEvent`。当玩家尝试打开被配置为保险箱的节点时，插件会先拦截原本的打开流程，进入破译 UI；破译成功或自动破译完成后，再调用 Searching API 打开原搜刮箱。

## 功能

- 按 `nodeTypeId` 配置保险箱规则
- 打开前进入破译界面
- 默认数字密码 UI，可作为占位实现或二次开发参考
- 底部 9 格自动破译进度条
- 支持自动破译与手动破译成功立即打开
- 支持权限、冷却、超时、失败处理
- Session 生命周期管理，玩家退出、重载、关闭插件时会清理会话
- 已打开但尚未刷新的 Searching 节点不会再次进入破译界面
- 可插拔 `UnlockUIProvider`
- 可替换 `SearchingOpenBridge`
- 提供基础命令和 Bukkit 事件

## 依赖

- Minecraft Bukkit / Paper 服务端
- Searching
- TabooLib 6
- Java 8+

## 安装

1. 确保服务端已经安装并启用 Searching。
2. 构建或获取 `SearchingSafeBox-*.jar`。
3. 将 jar 放入服务器 `plugins` 目录。
4. 启动服务器，生成并检查 `config.yml`。
5. 在 `safeboxes` 中把 Searching 的 `nodeTypeId` 配置为保险箱规则。

## 构建

Windows:

```powershell
.\gradlew.bat build
```

Linux / macOS:

```bash
./gradlew build
```

构建产物位于：

```text
build/libs/SearchingSafeBox-1.0.0.jar
```

开发 API 包：

```bash
./gradlew taboolibBuildApi -PDeleteCode
```

## 配置

示例：

```yaml
settings:
  debug: false

safeboxes:
  military_box:
    enabled: true
    unlock-ui: "default"
    title: "&c保险箱破译"

    auto-unlock: true
    auto-unlock-seconds: 30
    open-immediately-on-success: true

    unlock-timeout-seconds: 90
    consume-on-fail: false
    cooldown-seconds: 3
    permission: ""

    messages:
      locked: "&c这个保险箱被加密保护，需要先破译。"
      unlocked: "&a破译成功，正在打开保险箱。"
      failed: "&c破译失败。"
      auto-unlocked: "&a自动破译完成。"
```

字段说明：

| 字段 | 说明 |
| --- | --- |
| `enabled` | 是否启用该保险箱规则 |
| `unlock-ui` | 使用的 UI Provider ID |
| `title` | 破译界面标题 |
| `auto-unlock` | 是否启用自动破译 |
| `auto-unlock-seconds` | 自动破译耗时，单位秒 |
| `open-immediately-on-success` | 手动破译成功后是否立即打开原搜刮箱 |
| `unlock-timeout-seconds` | 玩家最大破译时间 |
| `consume-on-fail` | 破译失败是否直接结束本次流程 |
| `cooldown-seconds` | 同一玩家再次尝试同一节点的冷却 |
| `permission` | 打开该保险箱需要的权限，留空表示不限制 |
| `messages` | 玩家提示消息 |

## 打开逻辑

SearchingSafeBox 只会在 Searching 节点状态为 `Unsearched` 时进入破译流程。

如果节点已经被打开过并处于 `Generated` 或 `Revealed` 等未刷新状态，插件会直接放行给 Searching 原本逻辑，不会重复要求破译。节点刷新后重新变为 `Unsearched`，再次打开时会重新进入破译流程。

## 默认破译 UI

默认 UI Provider ID：

```text
default
```

默认 UI 是一个简单的三位数字破译界面：

- 发光数字是本轮密码中会出现的数字
- 点击数字后，上方输入槽会显示正确或错误
- 点击 `退格` 可以撤销上一位输入
- 输入正确后触发 `context.success()`
- 输入错误且 `consume-on-fail: false` 时，会短暂显示错误后重置输入槽
- 底部 9 格显示自动破译进度

这个 UI 是占位实现，实际项目可以注册自己的 `UnlockUIProvider` 替换。

## 自动破译

自动破译使用底部 9 格进度条：

- 灰色表示等待
- 绿色表示已完成进度
- 总格数固定为 9
- `auto-unlock-seconds: 9` 时，每秒点亮 1 格
- 读条结束后自动打开原搜刮箱

如果玩家提前破译成功，并且 `open-immediately-on-success: true`，会立即取消自动破译任务并打开原搜刮箱。

## 命令

主命令：

```text
/searchingsafebox
```

别名：

```text
/ssafebox
/safebox
```

子命令：

| 命令 | 说明 | 权限 |
| --- | --- | --- |
| `/searchingsafebox reload` | 重载配置并清理当前破译会话 | `searchingsafebox.command.reload` |
| `/searchingsafebox debug <nodeTypeId>` | 查看指定规则解析结果 | `searchingsafebox.command.debug` |
| `/searchingsafebox ui list` | 查看已注册 UI Provider | `searchingsafebox.command.ui` |
| `/searchingsafebox session list` | 查看当前破译会话 | `searchingsafebox.command.session` |
| `/searchingsafebox session clear <player>` | 清理指定玩家破译会话 | `searchingsafebox.command.session` |

基础命令权限：

```text
searchingsafebox.command
```

规则权限示例：

```text
searchingsafebox.lab
```

## 开发扩展

### 注册自定义 UI Provider

```kotlin
import org.ewsk.searchingsafebox.SearchingSafeBox
import org.ewsk.searchingsafebox.api.unlock.UnlockContext
import org.ewsk.searchingsafebox.api.unlock.UnlockUIProvider

object TerminalHackProvider : UnlockUIProvider {
    override val id: String = "terminal_hack"

    override fun open(context: UnlockContext) {
        // 打开你的破译 UI
        // 成功后调用 context.success()
        // 失败后调用 context.fail("reason")
        // 玩家取消时调用 context.cancel("reason")
    }
}

fun register() {
    SearchingSafeBox.uiRegistry.register(TerminalHackProvider)
}
```

配置中使用：

```yaml
unlock-ui: "terminal_hack"
```

### 替换 Searching Bridge

如果 Searching API 发生变化，或你需要自定义打开原搜刮箱的方式，可以注册新的 Bridge：

```kotlin
SearchingSafeBox.registerBridge(MySearchingOpenBridge)
```

需要实现：

```kotlin
interface SearchingOpenBridge {
    fun requiresUnlock(node: Any): Boolean
    fun openOriginal(player: Player, node: Any)
}
```

### 事件

插件提供以下 Bukkit 事件：

- `SafeBoxUnlockStartEvent`
- `SafeBoxUnlockSuccessEvent`
- `SafeBoxUnlockFailEvent`
- `SafeBoxUnlockCancelEvent`
- `SafeBoxAutoUnlockCompleteEvent`
- `SafeBoxOpenOriginalEvent`

监听示例：

```kotlin
import org.ewsk.searchingsafebox.event.SafeBoxUnlockSuccessEvent
import taboolib.common.platform.event.SubscribeEvent

object SafeBoxListener {
    @SubscribeEvent
    fun onSuccess(event: SafeBoxUnlockSuccessEvent) {
        // event.player
        // event.session
        // event.rule
    }
}
```

## 项目结构

```text
org.ewsk.searchingsafebox
├── api
├── api.unlock
├── bridge
├── command
├── config
├── event
├── listener
├── manager
├── registry
├── session
└── ui
```

## 注意事项

- 只有配置在 `safeboxes` 下的 `nodeTypeId` 会被当作保险箱。
- 破译 UI 必须通过 `UnlockContext` 回调结果，不要在 UI 内直接调用 Searching 打开逻辑。
