package me.vene.skilled.values;
import me.vene.skilled.utilities.StringRegistry;
public class StringValue {
 private String name, value; private int maxLength;
 public StringValue(String name, String value, int maxLength){this.name=StringRegistry.register(name);this.value=value==null?"":value;this.maxLength=maxLength;}
 public String getName(){return StringRegistry.register(this.name);}
 public String getValue(){return this.value;}
 public void setValue(String value){String v=value==null?"":value.trim(); if(v.length()>maxLength)v=v.substring(0,maxLength); this.value=v;}
}
