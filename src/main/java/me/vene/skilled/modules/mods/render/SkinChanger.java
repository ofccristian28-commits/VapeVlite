package me.vene.skilled.modules.mods.render;

import com.google.gson.*;
import me.vene.skilled.modules.Category;
import me.vene.skilled.modules.Module;
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
import java.util.concurrent.atomic.AtomicInteger;

/*
 * Elixe SkinChanger logic adapted to the VapeVlite value/module API.
 * The supplied Elixe class expects an Elixe renderer hook; VapeVlite does not
 * have that hook, so the texture is applied to NetworkPlayerInfo reflectively.
 */
public class SkinChanger extends Module {
    private static SkinChanger INSTANCE;
    private static final AtomicInteger REQUEST_VERSION = new AtomicInteger();

    private volatile String skinNick = "";
    private volatile String loadedNick = "";
    private volatile String pendingNick = "";
    private volatile ResourceLocation loadedSkin;
    private volatile boolean slimModel;
    private volatile long nextAllowedAttempt;

    private final StringValue skinNickOption;

    public SkinChanger() {
        super("Skin Changer", 0, Category.R);
        skinNickOption = new StringValue("skin nick", "", 16);
        addString(skinNickOption);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        invalidate();
    }

    @Override
    public void onDisable() {
        REQUEST_VERSION.incrementAndGet();
        pendingNick = "";
        loadedNick = "";
        loadedSkin = null;
        slimModel = false;
        nextAllowedAttempt = 0L;
        restoreOriginal();
    }

    @Override
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END && getState()) {
            String v = skinNickOption.getValue();
            if (!v.equals(skinNick)) {
                skinNick = v.trim();
                invalidate();
            }
            requestIfNeeded(skinNick);
        }
    }

    private void invalidate() {
        REQUEST_VERSION.incrementAndGet();
        pendingNick = "";
        loadedNick = "";
        loadedSkin = null;
        slimModel = false;
        nextAllowedAttempt = 0L;
    }

    public static ResourceLocation getLocalSkin(AbstractClientPlayer player) {
        SkinChanger instance = INSTANCE;
        if (instance == null || !instance.getState() || player == null) return null;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || player != mc.thePlayer) return null;

        String nick = instance.skinNick.trim();
        if (nick.length() == 0) return null;

        if (!nick.equalsIgnoreCase(instance.loadedNick)) {
            instance.requestIfNeeded(nick);
            return null;
        }
        return instance.loadedSkin;
    }

    public static String getLocalSkinType(AbstractClientPlayer player) {
        SkinChanger instance = INSTANCE;
        if (instance == null || !instance.getState() || player == null) return null;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || player != mc.thePlayer) return null;

        if (instance.loadedSkin == null) return null;
        return instance.slimModel ? "slim" : "default";
    }

    private synchronized void requestIfNeeded(final String wanted) {
        if (wanted == null || wanted.length() == 0) return;

        long now = System.currentTimeMillis();
        if (wanted.equalsIgnoreCase(pendingNick) || now < nextAllowedAttempt) return;

        pendingNick = wanted;
        nextAllowedAttempt = now + 1500L;
        final int version = REQUEST_VERSION.incrementAndGet();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SkinData data = resolveSkin(wanted);
                    if (version != REQUEST_VERSION.get() || data == null) return;
                    registerTexture(wanted, data, version);
                } catch (Throwable ignored) {
                    if (version == REQUEST_VERSION.get()) pendingNick = "";
                }
            }
        }, "Elixe-SkinChanger");
        t.setDaemon(true);
        t.start();
    }

    private void registerTexture(final String wanted, final SkinData data, final int version) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                if (version != REQUEST_VERSION.get() || !getState()) return;

                try {
                    ResourceLocation loc = new ResourceLocation(
                            "elixe",
                            "skins/" + data.uuid.toString().replace("-", "") + ".png"
                    );
                    ResourceLocation fallback =
                            net.minecraft.client.resources.DefaultPlayerSkin.getDefaultSkin(data.uuid);

                    ThreadDownloadImageData image =
                            new ThreadDownloadImageData(null, data.url, fallback, null);

                    mc.getTextureManager().loadTexture(loc, image);

                    loadedSkin = loc;
                    loadedNick = wanted;
                    pendingNick = "";
                    slimModel = data.slim;

                    setNetworkSkin(loc, data.slim);
                } catch (Throwable ignored) {}
            }
        });
    }

    private void setNetworkSkin(ResourceLocation loc, boolean slim) {
        try {
            NetworkPlayerInfo info = getInfo();
            if (info == null) return;

            FieldInfo resource = findResourceField(info);
            if (resource != null) {
                resource.field.setAccessible(true);
                resource.field.set(info, loc);
            }

            FieldInfo string = findStringField(info);
            if (string != null) {
                string.field.setAccessible(true);
                string.field.set(info, slim ? "slim" : "default");
            }

            FieldInfo loaded = findBooleanField(info);
            if (loaded != null) {
                loaded.field.setAccessible(true);
                loaded.field.setBoolean(info, true);
            }
        } catch (Throwable ignored) {}
    }

    private void restoreOriginal() {
        try {
            NetworkPlayerInfo info = getInfo();
            if (info == null) return;

            FieldInfo resource = findResourceField(info);
            if (resource != null) {
                resource.field.setAccessible(true);
                resource.field.set(info, null);
            }

            FieldInfo loaded = findBooleanField(info);
            if (loaded != null) {
                loaded.field.setAccessible(true);
                loaded.field.setBoolean(info, false);
            }
        } catch (Throwable ignored) {}
    }

    private NetworkPlayerInfo getInfo() {
        try {
            if (Minecraft.getMinecraft().thePlayer == null) return null;

            java.lang.reflect.Field f =
                    findFieldAssignable(AbstractClientPlayer.class, NetworkPlayerInfo.class);

            if (f == null) return null;
            f.setAccessible(true);

            return (NetworkPlayerInfo) f.get(Minecraft.getMinecraft().thePlayer);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static java.lang.reflect.Field findFieldAssignable(Class<?> c, Class<?> t) {
        for (java.lang.reflect.Field f : c.getDeclaredFields()) {
            if (t.isAssignableFrom(f.getType())) return f;
        }
        return null;
    }

    private static FieldInfo findResourceField(NetworkPlayerInfo info) {
        for (java.lang.reflect.Field f : info.getClass().getDeclaredFields()) {
            if (f.getType() == ResourceLocation.class) return new FieldInfo(f);
        }
        return null;
    }

    private static FieldInfo findStringField(NetworkPlayerInfo info) {
        for (java.lang.reflect.Field f : info.getClass().getDeclaredFields()) {
            if (f.getType() == String.class) return new FieldInfo(f);
        }
        return null;
    }

    private static FieldInfo findBooleanField(NetworkPlayerInfo info) {
        for (java.lang.reflect.Field f : info.getClass().getDeclaredFields()) {
            if (f.getType() == boolean.class) return new FieldInfo(f);
        }
        return null;
    }

    private static SkinData resolveSkin(String nick) throws Exception {
        JsonObject profile =
                getJson("https://api.mojang.com/users/profiles/minecraft/" + nick);

        if (profile == null || !profile.has("id")) return null;

        UUID uuid = parseUuid(profile.get("id").getAsString());

        JsonObject session =
                getJson("https://sessionserver.mojang.com/session/minecraft/profile/"
                        + profile.get("id").getAsString());

        if (session == null || !session.has("properties")) return null;

        for (JsonElement e : session.getAsJsonArray("properties")) {
            JsonObject o = e.getAsJsonObject();

            if (!o.has("name") || !"textures".equals(o.get("name").getAsString()))
                continue;

            byte[] raw = Base64.getDecoder().decode(o.get("value").getAsString());

            JsonObject d =
                    new JsonParser().parse(new String(raw, StandardCharsets.UTF_8))
                            .getAsJsonObject();

            JsonObject textures = d.getAsJsonObject("textures");
            if (textures == null || !textures.has("SKIN")) return null;

            JsonObject skin = textures.getAsJsonObject("SKIN");

            boolean slim =
                    skin.has("metadata")
                    && skin.getAsJsonObject("metadata").has("model")
                    && "slim".equalsIgnoreCase(
                            skin.getAsJsonObject("metadata").get("model").getAsString()
                    );

            return new SkinData(uuid, skin.get("url").getAsString(), slim);
        }

        return null;
    }

    private static JsonObject getJson(String u) throws Exception {
        HttpURLConnection c =
                (HttpURLConnection)new URL(u).openConnection();

        c.setConnectTimeout(5000);
        c.setReadTimeout(5000);
        c.setUseCaches(false);
        c.setRequestProperty("User-Agent", "Elixe-SkinChanger/1.0");

        if (c.getResponseCode() != 200) {
            c.disconnect();
            return null;
        }

        BufferedReader r =
                new BufferedReader(new InputStreamReader(
                        c.getInputStream(), StandardCharsets.UTF_8));

        StringBuilder b = new StringBuilder();
        String line;

        while ((line = r.readLine()) != null) b.append(line);

        r.close();
        c.disconnect();

        JsonElement e = new JsonParser().parse(b.toString());
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    private static UUID parseUuid(String s) {
        String x = s.replace("-", "").toLowerCase(Locale.ROOT);

        return UUID.fromString(
                x.substring(0, 8) + "-" +
                x.substring(8, 12) + "-" +
                x.substring(12, 16) + "-" +
                x.substring(16, 20) + "-" +
                x.substring(20)
        );
    }

    private static final class FieldInfo {
        final java.lang.reflect.Field field;
        FieldInfo(java.lang.reflect.Field field) {
            this.field = field;
        }
    }

    private static final class SkinData {
        final UUID uuid;
        final String url;
        final boolean slim;

        SkinData(UUID uuid, String url, boolean slim) {
            this.uuid = uuid;
            this.url = url;
            this.slim = slim;
        }
    }
}
