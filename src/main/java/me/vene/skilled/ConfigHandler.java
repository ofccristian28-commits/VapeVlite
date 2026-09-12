package me.vene.skilled;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.vene.skilled.modules.Module;
import me.vene.skilled.modules.ModuleManager;
import me.vene.skilled.values.BooleanValue;
import me.vene.skilled.values.NumberValue;
import me.vene.skilled.values.StringValue;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Map;

public final class ConfigHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigHandler() {}

    private static File getFile() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "vapevlite");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "config.json");
    }

    public static synchronized void save() {
        try {
            JsonObject root = new JsonObject();
            JsonArray modules = new JsonArray();
            for (Module mod : ModuleManager.getModules()) {
                JsonObject m = new JsonObject();
                m.addProperty("name", mod.getName());
                m.addProperty("enabled", mod.getState());
                m.addProperty("key", mod.getKey());

                JsonObject options = new JsonObject();
                for (BooleanValue v : mod.getOptions()) options.addProperty(v.getName(), v.getState());
                m.add("options", options);

                JsonObject values = new JsonObject();
                for (NumberValue v : mod.getValues()) values.addProperty(v.getName(), v.getValue());
                m.add("values", values);

                JsonObject strings = new JsonObject();
                for (StringValue v : mod.getStrings()) strings.addProperty(v.getName(), v.getValue());
                m.add("strings", strings);
                modules.add(m);
            }
            root.add("modules", modules);
            FileWriter writer = new FileWriter(getFile());
            GSON.toJson(root, writer);
            writer.close();
        } catch (Throwable ignored) {}
    }

    public static synchronized void load() {
        File file = getFile();
        if (!file.isFile()) return;
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("modules")) return;
            for (JsonElement e : root.getAsJsonArray("modules")) {
                if (!e.isJsonObject()) continue;
                JsonObject m = e.getAsJsonObject();
                String name = m.has("name") ? m.get("name").getAsString() : "";
                Module mod = null;
                for (Module candidate : ModuleManager.getModules()) {
                    if (candidate.getName().equalsIgnoreCase(name)) { mod = candidate; break; }
                }
                if (mod == null) continue;
                if (m.has("key")) mod.setKey(m.get("key").getAsInt());
                if (m.has("options")) {
                    JsonObject options = m.getAsJsonObject("options");
                    for (BooleanValue v : mod.getOptions()) if (options.has(v.getName())) v.setState(options.get(v.getName()).getAsBoolean());
                }
                if (m.has("values")) {
                    JsonObject values = m.getAsJsonObject("values");
                    for (NumberValue v : mod.getValues()) if (values.has(v.getName())) v.setValue(values.get(v.getName()).getAsDouble());
                }
                if (m.has("strings")) {
                    JsonObject strings = m.getAsJsonObject("strings");
                    for (StringValue v : mod.getStrings()) if (strings.has(v.getName())) v.setValue(strings.get(v.getName()).getAsString());
                }
                if (m.has("enabled")) mod.setState(m.get("enabled").getAsBoolean());
            }
        } catch (Throwable ignored) {}
        finally {
            if (reader != null) try { reader.close(); } catch (IOException ignored) {}
        }
    }
}
