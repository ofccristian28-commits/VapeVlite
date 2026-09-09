package br.vapevlite;

public abstract class Setting<T> {
    private final String id;
    private T value;

    protected Setting(String id, T value) {
        this.id = id;
        this.value = value;
    }

    public String getId() { return id; }
    public T getValue() { return value; }
    public void setValue(T value) { this.value = value; }
}
