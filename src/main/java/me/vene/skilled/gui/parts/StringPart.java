package me.vene.skilled.gui.parts;
import java.awt.Color;
import me.vene.skilled.gui.component.Component;
import me.vene.skilled.utilities.StringRegistry;
import me.vene.skilled.values.StringValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
public class StringPart extends Component {
 private StringValue value; private ModulesPart parent; private int offset; private boolean editing;
 public StringPart(StringValue value, ModulesPart parent, int offset){this.value=value;this.parent=parent;this.offset=offset;}
 public void render(){int x=parent.parent.getX(),y=parent.parent.getY()+offset,w=parent.parent.getWidth(); Gui.drawRect(x+1,y-2,x+w-2,y+12,new Color(35,35,35,230).getRGB()); String shown=value.getValue(); if(shown.length()==0)shown="<empty>"; if(editing)shown+="_"; Minecraft.getMinecraft().fontRendererObj.drawString(StringRegistry.register(value.getName())+": "+shown,x+3,y+1,-1);}
 public void setOff(int n){offset=n;} public void updateComponent(int x,int y){}
 public void mouseClicked(int x,int y,int button){if(button==0&&parent.open&&x>parent.parent.getX()&&x<parent.parent.getX()+parent.parent.getWidth()&&y>parent.parent.getY()+offset&&y<parent.parent.getY()+offset+12)editing=true;}
 public void keyTyped(char c,int key){if(!editing)return; if(key==28||key==1){editing=false;return;} if(key==14){String s=value.getValue();if(!s.isEmpty())value.setValue(s.substring(0,s.length()-1));return;} if(c>=32&&c<=126)value.setValue(value.getValue()+c);}
}
