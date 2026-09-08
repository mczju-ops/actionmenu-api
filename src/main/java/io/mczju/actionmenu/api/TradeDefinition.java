package io.mczju.actionmenu.api;

import java.util.List;

/** 一次交易的消耗或所得，可同时包含金额与多种物品。 */
public record TradeDefinition(double balance, List<TradeItemDefinition> items) {
    public TradeDefinition {
        items = List.copyOf(items);
    }

    public static TradeDefinition empty() { return new TradeDefinition(0, List.of()); }
    public static TradeDefinition money(double amount) { return new TradeDefinition(amount, List.of()); }
    public static TradeDefinition ofItems(TradeItemDefinition... items) {
        return new TradeDefinition(0, List.of(items));
    }
}
