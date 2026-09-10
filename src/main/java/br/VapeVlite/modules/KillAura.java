package br.vapevlite.modules;
import br.vapevlite.*;
public class KillAura extends Module {
 private final NumberSetting range=new NumberSetting("Range",3.5,1,6,0.1);
 private final NumberSetting cps=new NumberSetting("CPS",10,1,20,1);
 public KillAura(){super("KillAura",Category.COMBAT);addSetting(range);addSetting(cps);}
}
