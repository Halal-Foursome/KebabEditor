package org.halalfoursome.kebabeditor.archetype.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;

import org.halalfoursome.kebabeditor.archetype.ParamType;
import org.halalfoursome.kebabeditor.archetype.error.ParamDecodeException;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

class ColorParamTest {

    private static JsonNode text(String value) {
        return JsonNodeFactory.instance.textNode(value);
    }

    @Test
    void aColourIsWrittenAsAVec3WithComponentsFromZeroToOne() {
        JsonNode json = ParamJson.encode(ParamType.COLOR, new Color(255, 0, 51));

        assertEquals("(1.0, 0.0, 0.2)", json.textValue());
    }

    @Test
    void everyEightBitColourSurvivesAWriteAndReadRoundTrip() throws Exception {
        for (int value = 0; value < 256; value++) {
            Color color = new Color(value, 255 - value, (value * 7) % 256);

            Object decoded = ParamJson.decode(ParamType.COLOR, ParamJson.encode(ParamType.COLOR, color));

            assertEquals(color, decoded);
        }
    }

    @Test
    void outOfRangeComponentsAreClampedAndAlphaIsNotKept() throws Exception {
        assertEquals(new Color(255, 0, 128), ParamJson.decode(ParamType.COLOR, text("(2.0, -1.0, 0.5)")));
        assertEquals(
            "(1.0, 0.0, 0.0)",
            ParamJson.encode(ParamType.COLOR, new Color(255, 0, 0, 40)).textValue()
        );
    }

    @Test
    void aMalformedColourIsADecodeError() {
        assertThrows(ParamDecodeException.class, () -> ParamJson.decode(ParamType.COLOR, text("red")));
    }
}
