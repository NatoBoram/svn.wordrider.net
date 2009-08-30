package cz.vity.freerapid.gui.content;

import cz.vity.freerapid.model.DownloadFile;
import cz.vity.freerapid.plugins.webclient.DownloadState;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * @author Ladislav Vitasek
 */
class AverageSpeedCellRenderer extends DefaultTableCellRenderer {
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        final DownloadFile downloadFile = (DownloadFile) value;
        final DownloadState state = downloadFile.getState();
        if (state == DownloadState.DOWNLOADING) {
            if (downloadFile.getSpeed() >= 0) {
                value = ContentPanel.bytesToAnother((long) downloadFile.getAverageSpeed()) + "/s";
            } else value = "0 B/s";
        } else value = "";

        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
}
