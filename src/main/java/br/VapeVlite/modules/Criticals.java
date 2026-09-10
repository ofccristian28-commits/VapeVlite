package br.vapevlite.modules;
import br.vapevlite.*;
public class Criticals extends Module {
 private final BooleanSetting onlyGround=new BooleanSetting("Only Ground",true);
 public Criticals(){super("Criticals",Category.COMBAT);addSetting(onlyGround);}
}
