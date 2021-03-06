package cz.vity.freerapid.gui.managers;

import cz.vity.freerapid.model.DownloadFile;

import java.io.File;
import java.net.URL;

/**
 * @author Ladislav Vitasek
 */
public class FileHistoryItem {
    private URL url;
    private long finishedTime;

    private File outputFile;
    private String description;
    private String fileType;

    private long fileSize;
    private String fileName;

    private String shareDownloadServiceID;

    public FileHistoryItem() {

    }

    public FileHistoryItem(DownloadFile file, File savedTo) {
        this.url = file.getFileUrl();
        this.finishedTime = System.currentTimeMillis();
        this.outputFile = savedTo;
        this.description = file.getDescription();
        this.fileName = file.getFileName();
        this.fileSize = file.getFileSize();
        this.fileType = file.getFileType();
        this.shareDownloadServiceID = file.getShareDownloadServiceID();
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public long getFinishedTime() {
        return finishedTime;
    }

    public void setFinishedTime(long finishedTime) {
        this.finishedTime = finishedTime;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getShareDownloadServiceID() {
        return shareDownloadServiceID;
    }

    public void setShareDownloadServiceID(String shareDownloadServiceID) {
        this.shareDownloadServiceID = shareDownloadServiceID;
    }


    public String toString() {
        return "FileHistoryItem{" +
                "url=" + url +
                '}';
    }
}
