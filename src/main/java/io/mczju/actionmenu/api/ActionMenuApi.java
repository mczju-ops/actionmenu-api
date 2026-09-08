package io.mczju.actionmenu.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;

/**
 * 通过 Bukkit ServicesManager 获取的菜单服务。所有方法必须在 Paper 主线程调用。
 * 写操作成功表示内存已提交，文件仍由 ActionMenu 合并保存；不提供落盘完成保证。
 * 回调仅用于编辑草稿，不得嵌套调用本服务或执行命令、交易等外部操作。
 */
public interface ActionMenuApi {
    /** 返回独立不可变快照；菜单不存在时返回 null。 */
    @Nullable MenuSnapshot getMenu(String id);

    Set<String> getMenuIds();

    /**
     * 创建完整菜单，默认禁用、标题为 ID、背景为黑色玻璃板。
     * ID 建议带调用插件前缀，限小写字母、数字、下划线和连字符，首字符为字母或数字。
     * rows 为 1 到 6。同名菜单或文件存在、校验或提交失败时返回 false，不覆盖旧数据。
     */
    boolean create(String id, int rows, Consumer<MenuDraft> changes);

    /**
     * 一次提交多个字段和槽位的修改；ID 和行数不可更改。
     * 缺失、回调期间菜单已改变或校验/提交失败时返回 false。
     * 替换槽位会清除该槽位原有的未知 YAML 字段；不会重置玩家次数或动态价格成交量。
     */
    boolean update(String id, Consumer<MenuDraft> changes);

    default boolean setEnabled(String id, boolean enabled) {
        return update(id, draft -> draft.setEnabled(enabled));
    }

    /**
     * 打开普通菜单，不检查管理员权限，也不触发 OP 调试棒的编辑快捷入口。
     * 调用插件自行控制访问权限。菜单不存在、已禁用或玩家离线时返回 false。
     * 不应直接在 InventoryClickEvent 中调用，请由调用方安排到下一 tick。
     */
    boolean open(String id, Player player);
}
