package br.vapevlite.modules;
import br.vapevlite.*;
public class AutoSoup extends Module {
 private final NumberSetting health=new NumberSetting("Health",10,1,20,1);
 public AutoSoup(){super("AutoSoup",Category.PLAYER);addSetting(health);}
}
