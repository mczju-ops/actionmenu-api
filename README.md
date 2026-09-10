# ActionMenu API

ActionMenu 的公开 Paper 插件服务接口，用于由其他插件创建和批量修改服务器菜单，或在提供有效菜单定义的情况下打开虚拟菜单。

API 主插件版本独立，服务能力为查询、创建、批量修改、启停和打开菜单。目前不提供删除和次数重置。

## 调用插件的依赖

以 Maven 构建为例，调用插件自己的 POM 添加 JitPack 仓库：

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

使用 plugin.yml 的调用插件添加：

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

## 能力与约定

| 方法 | 含义 |
| --- | --- |
| getMenu(id) | 获取不可变快照，不存在返回 null |
| getMenuIds() | 获取不可变 ID 集合 |
| create(id, rows, callback) | 一次创建完整菜单，同名菜单或文件存在时不覆盖 |
| update(id, callback) | 在独立草稿中批量修改一个已有菜单 |
| setEnabled(id, enabled) | 启用或停用菜单 |
| open(id, player) | 为在线玩家打开已启用的普通菜单 |
| openTemporary(player, snapshot) | 为玩家打开外部快照，不保存普通菜单定义 |
| invalidateTemporary(id) | 使该 ID 的现有临时会话失效，保留次数 |
| clearTemporaryCounts(id) | 使现有会话失效并清理该临时 ID 的所有玩家次数 |

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
- 不开放普通菜单删除、普通菜单次数重置、动态价格管理和备份接口。临时菜单可单独清理次数，不引入商品身份或所有权注册系统。

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

## 按玩家打开临时菜单

临时菜单适合由外部插件生成并保存个人商品快照的场景。ActionMenu 仅在本次玩家会话中使用传入内容，不注册普通菜单、不写入 `menus/`；`getMenu`、`getMenuIds`、普通打开命令与编辑器都不会访问临时快照。

以下代码在主线程执行：

```java
String periodId = "rotating_shop_20260909_01";
ItemStack wheat = new ItemStack(Material.WHEAT);
MenuSnapshot personalMenu = new MenuSnapshot(
        periodId, "<dark_green>旅行商人", 6, true,
        Material.BLACK_STAINED_GLASS_PANE, null,
        java.util.Map.of(22, SlotDefinition.trade(
                new DisplayDefinition(wheat, null, 32, null,
                        java.util.List.of("{cost}", "{result}", "{remaining}")),
                TradeDefinition.ofItems(new TradeItemDefinition(wheat, 32)),
                TradeDefinition.money(50),
                5
        ))
);

// 调用方必须先保存个人商品，再允许玩家交易；重复打开使用同一份已保存结果。
boolean opened = api.openTemporary(player, personalMenu);
if (!opened) {
    // 玩家离线、快照未启用、窗口打开被取消等情况下，由调用方提示或稍后重试。
}

// 换期：先阻止旧期打开请求，再使旧窗口与批量交易回调失效。
api.invalidateTemporary(periodId);

// 历史保留期结束，确认不会再使用此 ID 后才清理次数。
// 不应每次玩家打开或退出时调用。
api.clearTemporaryCounts(periodId);
```

### 内容、访问与计数

- ID 格式与 API 创建普通菜单相同，建议包含调用插件前缀和唯一的本期标识。不同玩家可共用本期 ID，但传入不同商品；每个会话绑定传入玩家，不存在按 ID 获取他人临时快照的普通命令入口。
- 快照 `enabled` 必须为 true。完整快照经过与普通菜单创建相同的内容校验；非法值抛出异常。打开成功返回 true 只表示窗口已打开，不代表调用方商品或交易次数已落盘。
- 普通菜单和临时菜单的同名 ID 相互独立。临时次数存储在 `plugins/ActionMenu/data/temporary/<id>.yml`，按槽位和玩家 UUID 记录，约每 60 秒合并保存，正常停服补保存。临时快照本身由调用插件保存。
- 普通交易、Shift 批量交易、自定义次数对话框、潜影盒交易、命令槽位和整行 `{remaining}` 继续使用现有行为。临时菜单不参与普通菜单动态价格组，价格以快照为准。
- 同一玩家同一期同一槽位必须保持交易含义稳定。反复随机生成不同商品却复用同一个 ID/槽位，会继承之前的使用次数；ActionMenu 不负责校验业务商品身份。
- 临时命令槽位在执行命令前记录操作次数，防止命令同步打开另一会话或清理次数后又写回旧记录；命令效果由命令自身负责，不提供事务回滚。
- 旧普通菜单的次数不会自动迁移到临时目录。切换方案时建议另起一期；不要把旧普通菜单文件和个人商品错误关联。

### 会话失效与清理

- 同一玩家重新打开通过校验的临时菜单会替换其旧临时会话，旧对话框不能再提交；若新窗口被其他插件取消，旧会话仍失效。
- `invalidateTemporary(id)` 立即使该 ID 的所有现有会话失效，包括自定义交易次数对话框。不会强制关闭容器或对话框，后续点击或确认会被拒绝。
- 失效只作用于已经创建的会话，不注册永久禁用名单。之后显式传入快照仍可打开；调用方必须在换期时拦截晚到的异步旧期请求。
- 玩家退出或 ActionMenu 停用会清除会话令牌，但保留次数。调用插件在自身停用时也应失效其仍在使用的临时 ID，不应仅停止 NPC 交互。
- `clearTemporaryCounts(id)` 先失效会话，再清除内存中的该 ID 次数。磁盘文件在后续保存时删除；保存与删除串行，失败保持待保存标记并重试。此方法不返回落盘完成保证，强制终止可能使未落盘清理尚未生效。
- 建议每期使用独立 ID，换期只失效旧会话，历史保留期后再清理次数。若使用固定 ID 并清零，外部状态和次数清理没有跨插件事务保证，调用方需自行处理切换失败。
- 会话注册表每位在线玩家最多一个轻量令牌，不保留全部个人快照，也不积累历史失效 ID。普通关闭不能直接注销令牌，因为批量交易对话框可能替换原容器后仍需使用它。

### 接入验证建议

1. 为两个玩家使用同一临时 ID 打开不同商品，确认内容互不覆盖、次数独立，重新打开及重启后沿用次数。
2. 设置少量限购，检查单次、Shift、自定义次数及潜影盒成交后的计数和 `{remaining}`。
3. 打开批量交易对话框后使本期失效，确认提交不扣款、不扣物、不增加次数；重新打开同一玩家的另一临时快照后，旧回调也应被拒绝。
4. 创建同名普通菜单，确认临时打开、失效、清理及普通动态价格配置互不影响。
5. 清理历史次数后等待正常保存，确认临时次数文件删除；文件写入或删除失败时检查日志和重试，正常停服重启不会清零未清理的记录。
6. 取消 InventoryOpenEvent、退出重登或停用调用插件，确认打开结果及会话失效行为符合上述约定。
