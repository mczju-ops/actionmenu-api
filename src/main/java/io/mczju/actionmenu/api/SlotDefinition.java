package io.mczju.actionmenu.api;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/** 完整槽位；limit = -1 表示不限次数。有命令时优先执行命令，不进行交易。 */
public record SlotDefinition(@Nullable DisplayDefinition display, List<String> commands,
                             int limit, TradeDefinition cost, TradeDefinition result) {
    public SlotDefinition {
        commands = List.copyOf(commands);
        Objects.requireNonNull(cost, "cost");
        Objects.requireNonNull(result, "result");
    }

    public static SlotDefinition trade(DisplayDefinition display, TradeDefinition cost,
                                       TradeDefinition result, int limit) {
        return new SlotDefinition(display, List.of(), limit, cost, result);
    }

    public static SlotDefinition commands(DisplayDefinition display, List<String> commands, int limit) {
        return new SlotDefinition(display, commands, limit, TradeDefinition.empty(), TradeDefinition.empty());
    }
}
