package io.mczju.actionmenu;

import org.jetbrains.annotations.Range;

public interface ActionMenuApi {

    boolean createMenu(String menuId, String title, @Range(from = 1, to = 6) int row);

    boolean deleteMenu(String menuId);

    boolean hasMenu(String menuId);

    /** 原子写入一个槽位的完整配置 */
    boolean configureSlot(String menuId, int slot, SlotConfig config);

    /** 启用/禁用某菜单 */
    boolean setEnabled(String menuId, boolean enabled);
}
