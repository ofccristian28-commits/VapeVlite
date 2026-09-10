package br.vapevlite.modules;

import br.vapevlite.Category;
import br.vapevlite.Module;
import org.lwjgl.input.Keyboard;

public class ClickGUI extends Module {
    public ClickGUI() {
        super("ClickGUI", Category.RENDER);
        setKeybind(Keyboard.KEY_R);
    }
}
