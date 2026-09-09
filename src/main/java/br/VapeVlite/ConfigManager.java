package br.vapevlite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;

public class ConfigManager {
    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ModuleManager modules;

    public ConfigManager(ModuleManager modules) {
        this.modules = modules;
        File dir = new File("config");
        if (!dir.exists()) dir.mkdirs();
        file = new File(dir, "vapevlite.json");
    }

    public void load() {
        if (!file.isFile()) return;
        try {
            Reader reader = new FileReader(file);
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            reader.close();
            for (Module m : modules.getModules()) {
                JsonObject obj = root.has(m.getName()) && root.get(m.getName()).isJsonObject()
                        ? root.getAsJsonObject(m.getName()) : null;
                if (obj == null) continue;
                if (obj.has("enabled")) m.setEnabled(obj.get("enabled").getAsBoolean());
                if (obj.has("keybind")) m.setKeybind(obj.get("keybind").getAsInt());
                if (obj.has("settings") && obj.get("settings").isJsonObject()) {
                    JsonObject values = obj.getAsJsonObject("settings");
                    for (Setting<?> s : m.getSettings()) {
                        if (values.has(s.getId())) apply(s, values.get(s.getId()));
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void apply(Setting<?> s, JsonElement e) {
        if (s instanceof NumberSetting) ((NumberSetting)s).setValue(e.getAsDouble());
    }

    public void save() {
        try {
            JsonObject root = new JsonObject();
            for (Module m : modules.getModules()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("enabled", m.isEnabled());
                obj.addProperty("keybind", m.getKeybind());
                JsonObject values = new JsonObject();
                for (Setting<?> s : m.getSettings()) {
                    if (s instanceof NumberSetting) values.addProperty(s.getId(), ((NumberSetting)s).getValue());
                }
                obj.add("settings", values);
                root.add(m.getName(), obj);
            }
            Writer writer = new FileWriter(file);
            gson.toJson(root, writer);
            writer.close();
        } catch (Exception ignored) {}
    }
}
