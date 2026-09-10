package br.vapevlite.modules;
import br.vapevlite.*;
public class Hitbox extends Module {
 private final NumberSetting expand=new NumberSetting("Expand",0.1,0,1,0.05);
 public Hitbox(){super("Hitbox",Category.COMBAT);addSetting(expand);}
}
