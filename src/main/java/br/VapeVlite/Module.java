package br.vapevlite;

public abstract class Module {
    private final String name;
    private final Category category;
    private boolean enabled;

    protected Module(String name, Category category) {
        this.name=name; this.category=category;
    }
    public String getName(){return name;}
    public Category getCategory(){return category;}
    public boolean isEnabled(){return enabled;}
    public void toggle(){setEnabled(!enabled);}
    public void setEnabled(boolean value){
        if(enabled==value)return;
        enabled=value;
        if(value) onEnable(); else onDisable();
    }
    protected void onEnable(){}
    protected void onDisable(){}
}
