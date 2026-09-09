package br.vapevlite;

import java.util.HashSet;
import java.util.Set;
import org.lwjgl.input.Keyboard;

public class KeyState {
    private static final Set<Integer> DOWN = new HashSet<Integer>();

    public static boolean pressed(int key) {
        if (key <= 0) return false;
        boolean now = Keyboard.isKeyDown(key);
        boolean was = DOWN.contains(key);
        if (now) DOWN.add(key); else DOWN.remove(key);
        return now && !was;
    }
}
