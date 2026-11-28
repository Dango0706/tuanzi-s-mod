package me.tuanzi.world.canvas;

/**
 * 画布与打印模型的访问权限等级。
 */
public enum CanvasPermission {
    PRIVATE("private"),
    TEAM_EDIT("team_edit"),
    PUBLIC_READ("public_read"),
    PUBLIC_EDIT("public_edit");

    private final String id;

    CanvasPermission(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static CanvasPermission fromId(String id) {
        if (id == null) {
            return PRIVATE;
        }
        for (CanvasPermission perm : values()) {
            if (perm.id.equalsIgnoreCase(id)) {
                return perm;
            }
        }
        return PRIVATE;
    }

    public CanvasPermission next() {
        CanvasPermission[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public boolean isPublic() {
        return this == PUBLIC_READ || this == PUBLIC_EDIT;
    }

    public String getTranslationKey() {
        return "canvas.tuanzis_mod.permission." + id;
    }
}
