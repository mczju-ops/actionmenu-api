package io.mczju.actionmenu.api;

/** 音效 ID、非负音量和 0 到 2 的音调；菜单音效为 null 时继承主配置。 */
public record SoundDefinition(String id, float volume, float pitch) {}
