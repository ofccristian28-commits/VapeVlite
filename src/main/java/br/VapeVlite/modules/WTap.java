package br.vapevlite.modules;
import br.vapevlite.*;
public class WTap extends Module {
 private final NumberSetting delay=new NumberSetting("Delay",80,0,250,5);
 public WTap(){super("WTap",Category.COMBAT);addSetting(delay);}
}
