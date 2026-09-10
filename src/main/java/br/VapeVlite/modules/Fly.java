package br.vapevlite.modules;
import br.vapevlite.*;
public class Fly extends Module {
 private final NumberSetting speed=new NumberSetting("Speed",1,0.1,5,0.1);
 public Fly(){super("Fly",Category.MOVEMENT);addSetting(speed);}
}
