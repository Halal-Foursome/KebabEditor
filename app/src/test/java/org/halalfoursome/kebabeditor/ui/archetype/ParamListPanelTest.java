package org.halalfoursome.kebabeditor.ui.archetype;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.halalfoursome.kebabeditor.archetype.ParamSpec;
import org.halalfoursome.kebabeditor.archetype.ParamType;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.ui.validation.ValidationGroup;
import org.halalfoursome.kebabeditor.utils.KebabStyle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ParamListPanelTest {

    @BeforeAll
    static void style() {
        System.setProperty("java.awt.headless", "true");
        KebabStyle.setCurrent(KebabStyle.defaultStyle());
    }

    private static Map<String, ParamSpec> everyType() {
        Map<String, ParamSpec> specs = new LinkedHashMap<>();
        specs.put("count", new ParamSpec(ParamType.INT, 7));
        specs.put("radius", new ParamSpec(ParamType.FLOAT, 0.25f));
        specs.put("solid", new ParamSpec(ParamType.BOOL, true));
        specs.put("label", new ParamSpec(ParamType.STRING, "crate"));
        specs.put("path", new ParamSpec(ParamType.REL_PATH, "models/crate.gltf"));
        specs.put("size", new ParamSpec(ParamType.VEC3, new Vec3(4, 5, 6)));
        specs.put("tint", new ParamSpec(ParamType.COLOR, new Color(255, 128, 0)));
        specs.put("target", ParamSpec.of(ParamType.NODE_REF));

        return specs;
    }

    @Test
    void editorsReadBackTheDefaultsTheyWereGiven() throws Exception {
        Map<String, ParamSpec> specs = everyType();
        AtomicReference<Map<String, ParamSpec>> result = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            ParamListPanel panel = new ParamListPanel(KebabStyle.getCurrent(), new ValidationGroup(), specs);
            result.set(panel.params());
        });

        assertEquals(specs, result.get());
        assertEquals(specs.keySet().stream().toList(), result.get().keySet().stream().toList());
    }

}
