package com.knmusic.otmitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Tiện ích xử lý màu (&amp; và &amp;#RRGGBB) và thay thế placeholder {key}.
 */
public final class Text {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private Text() {
    }

    public static Component color(String text) {
        return SERIALIZER.deserialize(text == null ? "" : text);
    }

    /**
     * Thay thế placeholder theo cặp tên/giá trị: format(template, "skill", "Đóng Băng", "time", "3").
     */
    public static String format(String template, String... pairs) {
        String result = template == null ? "" : template;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            result = result.replace("{" + pairs[i] + "}", pairs[i + 1]);
        }
        return result;
    }
}
