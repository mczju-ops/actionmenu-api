package io.mczju.actionmenu.api;

import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/** 一种交易物品，数量可以超过堆叠上限；模板在输入和读取时均复制。 */
public record TradeItemDefinition(ItemStack item, int amount) {
    public TradeItemDefinition {
        Objects.requireNonNull(item, "item");
        if (amount <= 0) throw new IllegalArgumentException("交易物品数量必须大于 0");
        item = item.clone();
    }

    @Override
    public ItemStack item() { return item.clone(); }
}
