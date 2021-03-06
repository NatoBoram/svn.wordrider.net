package cz.vity.freerapid.plugins.services.youtube;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author tong2shot
 */
enum VideoQuality {
    Lowest(0, "Lowest available"),
    _240(240),
    _360(360),
    _480(480),
    _720(720),
    _1080(1080),
    _1440(1440),
    _2160(2160),
    _3072(3072),
    _4320(4320),
    Highest(10000, "Highest available");

    private final int quality;
    private final String name;

    VideoQuality(int quality) {
        this.quality = quality;
        this.name = quality + "p";
    }

    VideoQuality(int quality, String name) {
        this.quality = quality;
        this.name = name;
    }

    public int getQuality() {
        return quality;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static VideoQuality[] getItems() {
        final VideoQuality[] items = values();
        Arrays.sort(items, Collections.reverseOrder());
        return items;
    }
}
