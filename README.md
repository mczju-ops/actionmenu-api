# ActionMenu API

ActionMenu 的公开 Paper 插件服务接口，用于由其他插件创建和批量修改服务器菜单。仓库只包含公开接口、数据类型和文档，独立构建，不依赖 ActionMenu 本体源码或父 POM。

当前 API 版本为 `1.0.0`，与主插件版本独立。服务能力为查询、创建、批量修改、启停和打开菜单；本批不提供删除和次数重置。

## 构建与发布

- 使用 JDK 25，Paper API 依赖与 ActionMenu 当前目标版本一致。
- 本地运行 `mvn clean install`，将 `com.github.mczju-ops:actionmenu-api:1.0.0` 安装到本机 Maven 仓库，输出 JAR 位于 `target/`。
- API JAR 仅供编译依赖，不应安装到服务器 plugins 目录。ActionMenu 本体会将它合入自己的最终 JAR。
- JitPack 只访问本公开仓库，不需要访问主插件仓库。仓库提供 `jitpack.yml`，通过 SDKMAN 选择 Temurin JDK 25 后执行 Maven install。
- 首次发布时，由维护者提交代码、创建并推送 `1.0.0` 标签，再在 JitPack 查看构建结果。依赖版本必须对应真实的标签或提交；文档中的版本并不表示已经发布。
- 新增或变更 API 时更新 API 版本和发布标签。本体内部更新且 API 契约不变时，不需要修改本仓库或让调用插件升级依赖。
- 正式发布后不要移动已有标签或用同一版本覆盖不同的 API。开发期间优先本地联调，验证后再发布固定标签。
- 当前仅准备了发布配置，未执行 Maven、SDKMAN 或 JitPack 构建，JitPack 的 JDK 下载及完整构建仍需发布时验证。

ActionMenu 本体使用普通编译依赖并 Shade 此 API；调用插件使用 provided 依赖。API 包名始终为 `io.mczju.actionmenu.api`，不能 relocate，也不能由调用插件重复打包。

## 调用插件的依赖

调用插件自己的 POM 添加 JitPack 仓库：

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

然后添加 API 依赖：

```xml
<dependency>
    <groupId>com.github.mczju-ops</groupId>
    <artifactId>actionmenu-api</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

首次发布前，可以先将本项目安装到本机 Maven 仓库，再使用相同坐标开发。调用插件仍需声明自身使用的 Paper API 依赖。不要将 actionmenu-api 再次 Shade 到调用插件，不要复制 API 源码；运行时统一使用 ActionMenu 插件提供的接口类。

使用传统 plugin.yml 的调用插件添加：

```yaml
depend: [ActionMenu]
```

若已有其他依赖，应合并进列表。使用 paper-plugin.yml 的调用插件添加服务器依赖：

```yaml
dependencies:
  server:
    ActionMenu:
      load: BEFORE
      required: true
      join-classpath: true
```

两种插件描述文件按调用插件实际使用的格式选择，不必同时添加。

## 获取服务

在调用插件的 onEnable 中获取：

```java
import io.mczju.actionmenu.api.ActionMenuApi;

ActionMenuApi api = getServer().getServicesManager().load(ActionMenuApi.class);
if (api == null) {
    getLogger().severe("ActionMenu 服务不可用，停止启用");
    getServer().getPluginManager().disablePlugin(this);
    return;
}
```

ActionMenu 完成初始化后注册服务，在停用时先使旧服务引用失效，再注销服务和保存数据。调用插件不应在自身停用后继续使用服务。

## 首批能力与约定

| 方法 | 含义 |
| --- | --- |
| getMenu(id) | 获取不可变快照，不存在返回 null |
| getMenuIds() | 获取不可变 ID 集合 |
| create(id, rows, callback) | 一次创建完整菜单，同名菜单或文件存在时不覆盖 |
| update(id, callback) | 在独立草稿中批量修改一个已有菜单 |
| setEnabled(id, enabled) | 启用或停用菜单 |
| open(id, player) | 为在线玩家打开已启用的普通菜单 |

- 所有服务调用、草稿编辑及涉及 ItemStack 的准备均在 Paper 主线程执行。异步任务可以准备普通业务数据，再回主线程构造物品并提交。
- API 面向可信服务器插件。它不检查 actionmenu.op，调用方负责自己的访问控制。
- API 打开菜单不触发 OP 手持调试棒进入编辑界面的命令快捷行为。
- 不要直接在 InventoryClickEvent 中调用 open；由调用插件安排到下一 tick。
- 创建时 ID 允许小写字母、数字、下划线和连字符，首字符为字母或数字。建议使用插件前缀，例如 wandering_trader_20260907_01。
- 创建时行数为 1 到 6，默认标题为 ID、背景为黑色玻璃板、状态为禁用。启用需在草稿中明确设置。
- 已有菜单 ID 和行数不能通过 update 改变。槽位索引从 0 开始，最大为 rows * 9 - 1。
- 名称、标题和 Lore 采用现有 MiniMessage 格式。背景允许 AIR、GLASS_PANE 和染色玻璃板。
- 交易金额必须是有限非负数，交易物品不能是空气，数量必须为正整数；交易双方仍遵循原有成交规则。空消耗或空所得不会自动变成赠送功能。
- 次数限制为 -1 或非负整数；展示数量为正整数或 null。音效音量需非负、音调为 0 到 2 的有限数值。
- 回调仅修改草稿，不能嵌套调用服务，也不能执行命令或其他外部操作。回调异常原样抛出，整批草稿不提交。调用线程错误或服务停用时抛出 IllegalStateException。
- 创建参数、草稿操作中的非法值可能直接抛出 IllegalArgumentException；菜单缺失、冲突或内部验证/提交失败返回 false，内部失败记录日志。必须检查返回值。
- true 只表示内存状态提交成功，不表示文件已落盘。正常编辑仍以 100 tick 合并序列化并异步写盘，正常停服补保存；强制终止仍可能丢失未落盘修改。
- 快照集合不可修改，展示物品与交易模板在输入和读取时复制。不能通过修改它们直接更新菜单；保留并继续修改旧草稿也不会改变已提交菜单。
- setSlot 是完整替换：新槽位的配置覆盖原槽位子树，包括清除未知 YAML 字段。未触碰的槽位和菜单字段保持原有保存语义。
- 本批不开放删除菜单、重置次数、动态价格管理和备份接口。不引入商品身份或所有权注册系统。

## 创建周期菜单示例

以下代码在主线程中执行，api 是已获取的服务：

```java
import io.mczju.actionmenu.api.*;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

String menuId = "wandering_trader_20260907_01";
boolean created = api.create(menuId, 3, draft -> {
    draft.setTitle("<dark_green>流浪商人");
    draft.setEnabled(true);
    draft.setBackground(Material.BLACK_STAINED_GLASS_PANE);

    ItemStack diamond = new ItemStack(Material.DIAMOND);
    draft.setSlot(13, SlotDefinition.trade(
            DisplayDefinition.ofItem(diamond),
            TradeDefinition.money(100),
            TradeDefinition.ofItems(new TradeItemDefinition(diamond, 1)),
            3
    ));

    draft.setSlot(15, SlotDefinition.commands(
            DisplayDefinition.ofItem(new ItemStack(Material.PAPER)),
            java.util.List.of("am message {player} <green>欢迎光临"),
            -1
    ));
});
if (!created) {
    // 由调用插件记录失败或保留上一周期，不应继续切换其当前菜单 ID。
    throw new IllegalStateException("周期菜单创建失败：" + menuId);
}

// player 是调用插件已经确定的在线玩家。
boolean opened = api.open(menuId, player);
```

替换一批槽位时只调用一次 update，在回调中 clearSlots 后重新 setSlot 即可；修改后旧窗口会被原有的快照校验拦截。

不同周期应使用真正不同且不复用的菜单 ID。ActionMenu 的次数仍按菜单 ID + 槽位累计，创建同一个历史 ID 不会清理遗留的 data 记录。新 ID 不会自动加入动态价格组。

流浪商人插件负责记录当前周期 ID，并在新菜单创建成功后停用旧菜单。当前 API 尚不提供自动清理，旧菜单和记录会继续保留；长期运行前需完成后续删除与清理方案。停用会拦截旧窗口后续操作，但不会立即强制关闭所有玩家窗口。
