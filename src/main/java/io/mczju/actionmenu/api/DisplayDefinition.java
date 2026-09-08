package io.mczju.actionmenu.api;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 展示覆盖。名称和 Lore 使用 MiniMessage；null 表示不覆盖对应属性。
 * amount 为正整数或 null，glint 的 null 与 false 含义不同。
 * 物品在传入和读取时均复制，不能通过修改 ItemStack 改写已发布的快照。
 */
public record DisplayDefinition(@Nullable ItemStack item, @Nullable String name,
                                @Nullable Integer amount, @Nullable Boolean glint,
                                List<String> extraLore) {
    public DisplayDefinition {
        item = item == null ? null : item.clone();
        extraLore = List.copyOf(extraLore);
    }

    @Override
    public @Nullable ItemStack item() { return item == null ? null : item.clone(); }

    public static DisplayDefinition ofItem(ItemStack item) {
        return new DisplayDefinition(item, null, null, null, List.of());
    }
}
