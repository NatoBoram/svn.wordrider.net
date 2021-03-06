package cz.vity.freerapid.plugins.services.vimeo;

/**
 * @author tong2shot
 */
public class VimeoSettingsConfig {
    private VideoQuality videoQuality = VideoQuality.HD;

    public VideoQuality getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(final VideoQuality videoQuality) {
        this.videoQuality = videoQuality;
    }

    @Override
    public String toString() {
        return "VimeoSettingsConfig{" +
                "videoQuality=" + videoQuality +
                '}';
    }
}
