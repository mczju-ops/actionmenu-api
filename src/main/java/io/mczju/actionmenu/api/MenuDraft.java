package io.mczju.actionmenu.api;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 一次编辑的独立草稿。保留它或继续修改它不会改变已经提交的菜单。 */
public final class MenuDraft {
    private final String id;
    private final int rows;
    private String title;
    private boolean enabled;
    private Material background;
    private SoundDefinition sound;
    private final Map<Integer, SlotDefinition> slots;
    private final Set<Integer> replacedSlots = new HashSet<>();

    public MenuDraft(MenuSnapshot original) {
        id = original.id();
        rows = original.rows();
        title = original.title();
        enabled = original.enabled();
        background = original.background();
        sound = original.sound();
        slots = new HashMap<>(original.slots());
    }

    public String id() { return id; }
    public int rows() { return rows; }
    public String title() { return title; }
    public boolean enabled() { return enabled; }
    public Material background() { return background; }
    public @Nullable SoundDefinition sound() { return sound; }
    public Map<Integer, SlotDefinition> slots() { return Map.copyOf(slots); }

    public void setTitle(String value) { title = Objects.requireNonNull(value, "title"); }
    public void setEnabled(boolean value) { enabled = value; }
    public void setBackground(Material value) { background = Objects.requireNonNull(value, "background"); }
    public void setSound(@Nullable SoundDefinition value) { sound = value; }

    public void setSlot(int index, SlotDefinition value) {
        checkIndex(index);
        slots.put(index, Objects.requireNonNull(value, "slot"));
        replacedSlots.add(index);
    }

    /** 移除不存在的槽位时不作修改。 */
    public void removeSlot(int index) {
        checkIndex(index);
        if (slots.remove(index) != null) replacedSlots.add(index);
    }

    public void clearSlots() {
        replacedSlots.addAll(slots.keySet());
        slots.clear();
    }

    /** 本草稿显式替换或移除过的槽位；即使新旧内容相同，也保留替换意图。 */
    public Set<Integer> replacedSlots() { return Set.copyOf(replacedSlots); }

    public MenuSnapshot snapshot() {
        return new MenuSnapshot(id, title, rows, enabled, background, sound, slots);
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= rows * 9) throw new IllegalArgumentException("槽位超出菜单范围：" + index);
    }
}
