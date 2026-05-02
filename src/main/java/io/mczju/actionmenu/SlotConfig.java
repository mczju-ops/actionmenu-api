package io.mczju.actionmenu;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述一个槽位的完整配置，通过 {@link Builder} 构建。
 * <p>
 * 设计原则：所有字段均有默认值，子插件只需设置关心的部分，
 * 主插件后续新增字段时不会造成 API 破坏性变更。
 */
public final class SlotConfig {

    // 交易消耗
    private final List<ItemStack> costItems;
    private final List<Integer> costAmounts;
    private final double costBalance;

    // 交易所得
    private final List<ItemStack> resultItems;
    private final List<Integer> resultAmounts;
    private final double resultBalance;

    @Nullable private final ItemStack display; // null = 主插件自动生成
    @Nullable private final String command; // null = 无命令
    private final int limit; // -1 = 无限制

    private SlotConfig(Builder b) {
        this.costItems = List.copyOf(b.costItems);
        this.costAmounts = List.copyOf(b.costAmounts);
        this.costBalance = b.costBalance;
        this.resultItems = List.copyOf(b.resultItems);
        this.resultAmounts = List.copyOf(b.resultAmounts);
        this.resultBalance = b.resultBalance;
        this.display = b.display;
        this.command = b.command;
        this.limit = b.limit;
    }

    // Getter
    public List<ItemStack> getCostItems() { return costItems; }
    public List<Integer> getCostAmounts() { return costAmounts; }
    public double getCostBalance() { return costBalance; }
    public List<ItemStack> getResultItems() { return resultItems; }
    public List<Integer> getResultAmounts() { return resultAmounts; }
    public double getResultBalance() { return resultBalance; }
    @Nullable public ItemStack getDisplay() { return display; }
    @Nullable public String getCommand() { return command; }
    public int getLimit() { return limit; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static final class Builder {

        // 默认值：空列表 / 0 / null / 无限制
        private List<ItemStack> costItems = new ArrayList<>();
        private List<Integer> costAmounts = new ArrayList<>();
        private double costBalance = 0.0;
        private List<ItemStack> resultItems = new ArrayList<>();
        private List<Integer> resultAmounts = new ArrayList<>();
        private double resultBalance = 0.0;
        @Nullable private ItemStack display = null;
        @Nullable private String command = null;
        private int limit = -1;

        private Builder() {}

        /**
         * 添加一条"消耗物品+数量"记录（可多次调用）。
         * 相比直接传两个平行 List，更不容易错位。
         */
        public Builder addCostItem(ItemStack item, int amount) {
            costItems.add(item);
            costAmounts.add(amount);
            return this;
        }

        /** 批量设置消耗物品（与 costAmounts 长度须一致，由主插件在 build/apply 时校验） */
        public Builder costItems(List<ItemStack> items, List<Integer> amounts) {
            this.costItems = new ArrayList<>(items);
            this.costAmounts = new ArrayList<>(amounts);
            return this;
        }

        public Builder costBalance(double balance) {
            this.costBalance = balance;
            return this;
        }

        /** 添加一条“所得物品 + 数量”记录（可多次调用）。 */
        public Builder addResultItem(ItemStack item, int amount) {
            resultItems.add(item);
            resultAmounts.add(amount);
            return this;
        }

        public Builder resultItems(List<ItemStack> items, List<Integer> amounts) {
            this.resultItems = new ArrayList<>(items);
            this.resultAmounts = new ArrayList<>(amounts);
            return this;
        }

        public Builder resultBalance(double balance) {
            this.resultBalance = balance;
            return this;
        }

        /** 不调用则主插件自动生成图标。 */
        public Builder display(@Nullable ItemStack display) {
            this.display = display;
            return this;
        }

        public Builder command(@Nullable String command) {
            this.command = command;
            return this;
        }

        /** 每个玩家的操作上限，默认 -1（无限）。 */
        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public SlotConfig build() {
            // 基本校验：两个平行列表长度一致
            if (costItems.size() != costAmounts.size())
                throw new IllegalStateException("costItems 与 costAmounts 长度不一致");
            if (resultItems.size() != resultAmounts.size())
                throw new IllegalStateException("resultItems 与 resultAmounts 长度不一致");
            return new SlotConfig(this);
        }
    }
}