package br.vapevlite.modules;
import br.vapevlite.*;
import net.minecraft.client.Minecraft;
public class Velocity extends Module {
 private final NumberSetting horizontal=new NumberSetting("Horizontal",0,0,100,5);
 private final NumberSetting vertical=new NumberSetting("Vertical",0,0,100,5);
 public Velocity(){super("Velocity",Category.COMBAT);addSetting(horizontal);addSetting(vertical);}
 @Override public void onClientTick(){if(!isEnabled()||Minecraft.getMinecraft().thePlayer==null)return;}
}
