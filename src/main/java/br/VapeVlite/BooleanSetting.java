package br.vapevlite;

public class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String id, boolean value) {
        super(id, value);
    }

    public void toggle() {
        setValue(!getValue());
    }
}
