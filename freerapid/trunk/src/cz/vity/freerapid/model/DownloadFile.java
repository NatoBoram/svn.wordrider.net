package cz.vity.freerapid.model;

import cz.vity.freerapid.core.tasks.DownloadTask;
import cz.vity.freerapid.utilities.LogUtils;
import org.jdesktop.application.AbstractBean;

import java.beans.*;
import java.io.File;
import java.net.URL;
import java.util.logging.Logger;

/**
 * @author Ladislav Vitasek
 */
public class DownloadFile extends AbstractBean implements PropertyChangeListener {
    private final static Logger logger = Logger.getLogger(DownloadFile.class.getName());

    private long fileSize;
    private DownloadTask task = null;
    private DownloadState state = DownloadState.PAUSED;
    private String fileName;
    private long downloaded = 0;
    private int sleep;
    private float averageSpeed;
    private long speed;
    private String errorMessage;
    private URL fileUrl = null;
    private File saveToDirectory;


    static {
        try {
            BeanInfo info = Introspector.getBeanInfo(DownloadFile.class);
            PropertyDescriptor[] propertyDescriptors =
                    info.getPropertyDescriptors();
            for (PropertyDescriptor pd : propertyDescriptors) {
                if ("task".equals(pd.getName()) || "speed".equals(pd.getName()) || "averageSpeed".equals(pd.getName())) {
                    pd.setValue("transient", Boolean.TRUE);
                }
            }
        } catch (IntrospectionException e) {
            LogUtils.processException(logger, e);
        }

    }

    public DownloadFile() {//XMLEncoder
        logger.info("Konstruktor empty");
    }

    public DownloadFile(URL fileUrl, File saveToDirectory) {
        this.fileUrl = fileUrl;
        this.saveToDirectory = saveToDirectory;
        this.fileSize = -1;
        this.fileName = "";
        //this.downloaded = 0;
        this.sleep = -1;
        this.averageSpeed = -1;
        this.speed = 0;
    }

    public File getSaveToDirectory() {
        return saveToDirectory;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        long oldValue = this.fileSize;
        this.fileSize = fileSize;
        firePropertyChange("fileSize", oldValue, this.fileSize);
    }

    public DownloadTask getTask() {
        return task;
    }

    public void setTask(DownloadTask task) {
        if (task == null) {
            if (this.task != null)
                this.task.removePropertyChangeListener(this);
        } else {
            task.addPropertyChangeListener(this);
        }
        //System.out.println("task = " + task);
        this.task = task;
    }

    public DownloadState getState() {
        return state;
    }

    public void setState(DownloadState state) {
        if (this.state == DownloadState.DELETED)
            return;
        DownloadState oldValue = this.state;
        this.state = state;
        logger.info("Setting state to " + state.toString());
        firePropertyChange("state", oldValue, this.state);
    }

    public URL getFileUrl() {
        return fileUrl;
    }


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return fileUrl.toString();
    }

    public long getDownloaded() {
        return downloaded;
    }

    public void setDownloaded(long downloaded) {
        long oldValue = this.downloaded;
        this.downloaded = downloaded;
        logger.info("setting downloaded to " + downloaded);
        firePropertyChange("downloaded", oldValue, this.downloaded);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        //System.out.println("evt.getPropertyName() = " + evt.getPropertyName());
        if ("downloaded".equals(evt.getPropertyName())) {
            this.setDownloaded((Long) evt.getNewValue());
        } else if ("sleep".equals(evt.getPropertyName())) {
            this.setSleep((Integer) evt.getNewValue());
        } else if ("speed".equals(evt.getPropertyName())) {
            this.setSpeed((Long) evt.getNewValue());
        } else if ("averageSpeed".equals(evt.getPropertyName())) {
            this.setAverageSpeed((Float) evt.getNewValue());
        }
    }

    public void setSpeed(long speed) {
        long oldValue = this.speed;
        this.speed = speed;
        firePropertyChange("speed", oldValue, this.speed);
    }

    public long getSpeed() {
        return speed;
    }

    public void setSleep(int sleep) {
        int oldValue = this.sleep;
        this.sleep = sleep;
        firePropertyChange("sleep", oldValue, this.sleep);
    }

    public void setAverageSpeed(float averageSpeed) {
        float oldValue = this.averageSpeed;
        this.averageSpeed = averageSpeed;
        firePropertyChange("averageSpeed", oldValue, this.sleep);
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getSleep() {
        return sleep;
    }

    public float getAverageSpeed() {
        return averageSpeed;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public File getOutputFile() {
        return new File(this.getSaveToDirectory(), fileName);
    }

    public void setFileUrl(URL fileUrl) {
        this.fileUrl = fileUrl;
    }

    public void setSaveToDirectory(File saveToDirectory) {
        this.saveToDirectory = saveToDirectory;
    }
}
