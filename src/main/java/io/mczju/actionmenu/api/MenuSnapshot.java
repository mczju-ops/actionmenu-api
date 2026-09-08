package io.mczju.actionmenu.api;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/** 菜单快照。标题使用 MiniMessage；槽位索引从 0 开始。 */
public record MenuSnapshot(String id, String title, int rows, boolean enabled,
                           Material background, @Nullable SoundDefinition sound,
                           Map<Integer, SlotDefinition> slots) {
    public MenuSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(background, "background");
        slots = Map.copyOf(slots);
    }
}
