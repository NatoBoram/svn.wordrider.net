package cz.vity.freerapid.plugins.services.spankbang;

/**
 * @author tong2shot
 */

public class SettingsConfig {
    private VideoQuality videoQuality = VideoQuality.Highest;

    public VideoQuality getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(final VideoQuality videoQuality) {
        this.videoQuality = videoQuality;
    }

    @Override
    public String toString() {
        return "SettingsConfig{" +
                "videoQuality=" + videoQuality +
                '}';
    }
}
