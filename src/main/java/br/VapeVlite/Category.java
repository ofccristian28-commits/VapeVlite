package br.vapevlite;

public enum Category {
    PLAYER("player"),
    RENDER("render"),
    COMBAT("combat"),
    WORLD("world"),
    MOVEMENT("movement"),
    UTILS("utils");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
