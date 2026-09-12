package me.vene.skilled.modules.mods.render;

import com.google.gson.*;
import me.vene.skilled.modules.Category;
import me.vene.skilled.modules.Module;
import me.vene.skilled.utilities.StringRegistry;
import me.vene.skilled.values.StringValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Elixe-style local skin changer, but without the artificial 1.5s activation delay. */
public class SkinChanger extends Module {
    private static final AtomicInteger REQUEST_VERSION = new AtomicInteger();
    private static final Map<String, SkinData> CACHE = new ConcurrentHashMap<String, SkinData>();
    private final StringValue skinNick;
    private volatile String loadedNick = "";
    private volatile String requestedNick = "";
    private volatile ResourceLocation loadedSkin;

    public SkinChanger() {
        super(StringRegistry.register("Skin Changer"), 0, Category.R);
        skinNick = new StringValue("Skin", "", 16);
        addString(skinNick);
    }

    @Override
    public void onEnable() {
        // Do not reset the local skin here. Start the request immediately instead.
        loadedNick = "";
        requestedNick = "";
        requestNow();
    }

    @Override
    public void onDisable() {
        REQUEST_VERSION.incrementAndGet();
        requestedNick = "";
        loadedNick = "";
        restoreOriginal();
    }

    @Override
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !getState()) return;
        requestNow();
    }

    private void requestNow() {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        final String wanted = skinNick.getValue().trim();
        if (wanted.length() == 0) {
            if (loadedSkin != null) restoreOriginal();
            loadedNick = "";
            requestedNick = "";
            return;
        }
        if (wanted.equalsIgnoreCase(loadedNick) || wanted.equalsIgnoreCase(requestedNick)) return;
        requestedNick = wanted;
        final int version = REQUEST_VERSION.incrementAndGet();
        SkinData cached = CACHE.get(wanted.toLowerCase(Locale.ROOT));
        if (cached != null) {
            mc.addScheduledTask(new Runnable() {
                @Override public void run() {
                    if (version == REQUEST_VERSION.get() && getState()) applySkin(cached, wanted);
                }
            });
            return;
        }
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    final SkinData data = resolveSkin(wanted);
                    if (data == null) return;
                    CACHE.put(wanted.toLowerCase(Locale.ROOT), data);
                    if (version != REQUEST_VERSION.get() || !getState()) return;
                    mc.addScheduledTask(new Runnable() {
                        @Override public void run() {
                            if (version == REQUEST_VERSION.get() && getState()) applySkin(data, wanted);
                        }
                    });
                } catch (Throwable ignored) {}
            }
        }, "VapeVlite-SkinChanger").start();
    }

    private void applySkin(SkinData data, String wanted) {
        try {
            ResourceLocation loc = new ResourceLocation("vapevlite", "skins/" + data.uuid.toString().replace("-", "") + ".png");
            ResourceLocation fallback = net.minecraft.client.resources.DefaultPlayerSkin.getDefaultSkin(data.uuid);
            ThreadDownloadImageData image = new ThreadDownloadImageData(null, data.url, fallback, null);
            Minecraft.getMinecraft().getTextureManager().loadTexture(loc, image);
            loadedSkin = loc;
            setNetworkSkin(loc, data.slim);
            loadedNick = wanted;
            requestedNick = wanted;
        } catch (Throwable ignored) {}
    }

    private void setNetworkSkin(ResourceLocation loc, boolean slim) {
        try {
            NetworkPlayerInfo info = getInfo();
            if (info == null) return;
            java.lang.reflect.Field skin = findResourceField(info);
            if (skin != null) { skin.setAccessible(true); skin.set(info, loc); }
            java.lang.reflect.Field type = findStringField(info);
            if (type != null) { type.setAccessible(true); type.set(info, slim ? "slim" : "default"); }
            java.lang.reflect.Field loaded = findBooleanField(info);
            if (loaded != null) { loaded.setAccessible(true); loaded.setBoolean(info, true); }
        } catch (Throwable ignored) {}
    }

    private void restoreOriginal() {
        try {
            NetworkPlayerInfo info = getInfo();
            if (info == null) return;
            java.lang.reflect.Field skin = findResourceField(info);
            if (skin != null) { skin.setAccessible(true); skin.set(info, null); }
            java.lang.reflect.Field loaded = findBooleanField(info);
            if (loaded != null) { loaded.setAccessible(true); loaded.setBoolean(info, false); }
            loadedSkin = null;
        } catch (Throwable ignored) {}
    }

    private NetworkPlayerInfo getInfo() {
        try {
            if (Minecraft.getMinecraft().thePlayer == null) return null;
            java.lang.reflect.Field f = findFieldAssignable(AbstractClientPlayer.class, NetworkPlayerInfo.class);
            if (f == null) return null;
            f.setAccessible(true);
            return (NetworkPlayerInfo) f.get(Minecraft.getMinecraft().thePlayer);
        } catch (Throwable ignored) { return null; }
    }

    private static java.lang.reflect.Field findFieldAssignable(Class<?> c, Class<?> t) {
        for (java.lang.reflect.Field f : c.getDeclaredFields()) if (t.isAssignableFrom(f.getType())) return f;
        return null;
    }

    private static java.lang.reflect.Field findResourceField(NetworkPlayerInfo info) {
        for (java.lang.reflect.Field f : info.getClass().getDeclaredFields()) if (f.getType() == ResourceLocation.class) return f;
        return null;
    }

    private static java.lang.reflect.Field findStringField(NetworkPlayerInfo info) {
        for (java.lang.reflect.Field f : info.getClass().getDeclaredFields()) if (f.getType() == String.class) return f;
        return null;
    }

    private static java.lang.reflect.Field findBooleanField(NetworkPlayerInfo info) {
        for (java.lang.reflect.Field f : info.getClass().getDeclaredFields()) if (f.getType() == boolean.class) return f;
        return null;
    }

    private static SkinData resolveSkin(String nick) throws Exception {
        JsonObject p = getJson("https://api.mojang.com/users/profiles/minecraft/" + nick);
        if (p == null || !p.has("id")) return null;
        UUID uuid = parseUuid(p.get("id").getAsString());
        JsonObject s = getJson("https://sessionserver.mojang.com/session/minecraft/profile/" + p.get("id").getAsString());
        if (s == null || !s.has("properties")) return null;
        for (JsonElement e : s.getAsJsonArray("properties")) {
            JsonObject o = e.getAsJsonObject();
            if (!o.has("name") || !"textures".equals(o.get("name").getAsString())) continue;
            byte[] raw = Base64.getDecoder().decode(o.get("value").getAsString());
            JsonObject d = new JsonParser().parse(new String(raw, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject tx = d.getAsJsonObject("textures");
            if (tx == null || !tx.has("SKIN")) return null;
            JsonObject sk = tx.getAsJsonObject("SKIN");
            boolean slim = sk.has("metadata") && sk.getAsJsonObject("metadata").has("model") && "slim".equalsIgnoreCase(sk.getAsJsonObject("metadata").get("model").getAsString());
            return new SkinData(uuid, sk.get("url").getAsString(), slim);
        }
        return null;
    }

    private static JsonObject getJson(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(u).openConnection();
        c.setConnectTimeout(5000);
        c.setReadTimeout(5000);
        c.setUseCaches(false);
        c.setRequestProperty("User-Agent", "VapeVlite-SkinChanger/1.0");
        if (c.getResponseCode() != 200) { c.disconnect(); return null; }
        BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder b = new StringBuilder();
        String l;
        while ((l = r.readLine()) != null) b.append(l);
        r.close();
        c.disconnect();
        JsonElement e = new JsonParser().parse(b.toString());
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    private static UUID parseUuid(String s) {
        String x = s.replace("-", "").toLowerCase(Locale.ROOT);
        return UUID.fromString(x.substring(0,8)+"-"+x.substring(8,12)+"-"+x.substring(12,16)+"-"+x.substring(16,20)+"-"+x.substring(20));
    }

    private static final class SkinData {
        final UUID uuid; final String url; final boolean slim;
        SkinData(UUID u, String x, boolean s) { uuid=u; url=x; slim=s; }
    }
}
