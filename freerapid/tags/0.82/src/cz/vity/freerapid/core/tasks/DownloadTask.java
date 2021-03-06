package cz.vity.freerapid.core.tasks;

import cz.vity.freerapid.core.AppPrefs;
import cz.vity.freerapid.core.MainApp;
import cz.vity.freerapid.core.UserProp;
import cz.vity.freerapid.gui.managers.TaskServiceManager;
import cz.vity.freerapid.model.DownloadFile;
import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFileDownloadTask;
import cz.vity.freerapid.plugins.webclient.interfaces.ShareDownloadService;
import cz.vity.freerapid.swing.Swinger;
import cz.vity.freerapid.utilities.FileUtils;
import cz.vity.freerapid.utilities.LogUtils;
import cz.vity.freerapid.utilities.Sound;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.jdesktop.application.Application;
import org.jdesktop.application.TaskEvent;
import org.jdesktop.application.TaskListener;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Vity
 */
public class DownloadTask extends CoreTask<Void, Long> implements HttpFileDownloadTask {
    private final static Logger logger = Logger.getLogger(DownloadTask.class.getName());
    protected HttpDownloadClient client;
    protected DownloadFile downloadFile;
    protected ShareDownloadService service;
    private long speedInBytes;
    private float averageSpeed;
    private volatile long counter;
    private Integer sleep = 0;
    protected File outputFile;
    protected File storeFile;
    private static java.util.Timer timer = new java.util.Timer();
    private DownloadTaskError serviceError;

    private int youHaveToSleepSecondsTime = 0;
    private static final int NO_DATA_TIMEOUT_LIMIT = 100;
    private static final int INPUT_BUFFER_SIZE = 24000;
    private static final int OUTPUT_FILE_BUFFER_SIZE = 600000;
    private volatile boolean connectionTimeOut;
    private int fileAlreadyExists;

    public DownloadTask(Application application) {
        super(application);
        init();
    }

    public DownloadTask(Application application, HttpDownloadClient client, DownloadFile downloadFile, ShareDownloadService service) {
        super(application);
        this.client = client;
        this.downloadFile = downloadFile;
        this.service = service;
        init();
        downloadFile.setConnectionSettings(client.getSettings());
    }

    protected void init() {
        this.serviceError = DownloadTaskError.NO_ERROR;
        this.setInputBlocker(null);
        this.setUserCanCancel(true);
        this.youHaveToSleepSecondsTime = 0;
        this.connectionTimeOut = false;
        this.speedInBytes = 0;
        this.averageSpeed = 0;
        fileAlreadyExists = -2;
    }

    @Override
    protected Void doInBackground() throws Exception {
        initDownloadThread();


        downloadFile.setDownloaded(0);
        final int seconds = AppPrefs.getProperty(UserProp.ERROR_SLEEP_TIME, UserProp.ERROR_SLEEP_TIME_DEFAULT);
        if (seconds > 0)
            sleep(seconds);
        downloadFile.setState(DownloadState.GETTING);
        service.run(this);//run plugin
        service = null;
        return null;
    }

    protected void initDownloadThread() {
        client.getHTTPClient().setHttpConnectionManager(new SimpleHttpConnectionManager());
        final int timerPurge = timer.purge();
        if (timerPurge > 0)
            logger.info("Purged timers " + timerPurge);
        client.getHTTPClient().getHttpConnectionManager().closeIdleConnections(0);
    }

    protected OutputStream getFileOutputStream(final File f, final long fileSize) throws NotEnoughSpaceException, IOException {
        if (f.getParentFile().getFreeSpace() < fileSize + 10 * 1024 * 1024) { //+ 10MB
            throw new NotEnoughSpaceException();
        }
        final OutputStream fos;
        if (AppPrefs.getProperty(UserProp.ANTI_FRAGMENT_FILES, UserProp.ANTI_FRAGMENT_FILES_DEFAULT)) {
            synchronized (DownloadTask.class) {
                if (isTerminated())
                    return null;
                fos = FileUtils.createEmptyFile(f, fileSize, this);
                if (isTerminated())
                    return null;
            }
        } else {
            fos = new FileOutputStream(f);
        }

        return new BufferedOutputStream(fos, AppPrefs.getProperty(UserProp.OUTPUT_FILE_BUFFER_SIZE, OUTPUT_FILE_BUFFER_SIZE));
    }

    protected void initBackground() {
        //client.initClient();
    }

    @Override
    public boolean isTerminated() {
        return this.isCancelled() || Thread.currentThread().isInterrupted();
    }

    @Override
    public void saveToFile(InputStream inputStream) throws Exception {
        if (inputStream == null)
            throw new NullPointerException("Input stream for saving cannot be null");
        downloadFile.setFileState(FileState.CHECKED_AND_EXISTING);
        final boolean temporary = useTemporaryFiles();

        final byte[] buffer = new byte[AppPrefs.getProperty(UserProp.INPUT_BUFFER_SIZE, INPUT_BUFFER_SIZE)];
        final OutputStream[] fileOutputStream = new OutputStream[]{null};
        final String fileName = downloadFile.getFileName();
        outputFile = downloadFile.getOutputFile();
        //outputFile = new File("d:/vystup.pdf");
        if (temporary) {
            this.fileAlreadyExists = checkExists();
            if (this.fileAlreadyExists == UserProp.SKIP) {
                this.cancel(true);
                return;
            }
        }

        final File saveToDirectory = downloadFile.getSaveToDirectory();
        try {
            if (!saveToDirectory.exists())
                saveToDirectory.mkdirs();
            storeFile = (temporary) ? File.createTempFile(fileName + ".", ".part", saveToDirectory) : outputFile;
            final long fileSize = downloadFile.getFileSize();

            if (temporary)
                storeFile.deleteOnExit();

            try {
                fileOutputStream[0] = getFileOutputStream(storeFile, fileSize);
                if (isTerminated()) {
                    closeFileStream(fileOutputStream[0]);
                    checkDeleteTempFile();
                    return;
                }
                int len;
                counter = 0;
                downloadFile.setState(DownloadState.DOWNLOADING);
                setSpeed(0);
                final long time = System.currentTimeMillis();

                final int avgSpeedMeasuredSeconds = AppPrefs.getProperty(UserProp.AVG_SPEED_MEASURED_SECONDS, UserProp.AVG_SPEED_MEASURED_SECONDS_DEFAULT);
                final long[] avgSpeedArray = new long[avgSpeedMeasuredSeconds];
                Arrays.fill(avgSpeedArray, -1);

                timer.schedule(new TimerTask() {
                    private long lastSize = 0;
                    private int noDataTimeOut = 0; //10 seconds to timeout
                    private short indexer = 0;

                    public void run() {

                        if (isTerminated() || downloadFile.getState() != DownloadState.DOWNLOADING) {
                            this.cancel();
                            return;
                        }

                        final long speed = counter - lastSize;

                        setSpeed(speed);

                        if (speed == 0) {
                            if (++noDataTimeOut >= NO_DATA_TIMEOUT_LIMIT) { //X seconds with no data
                                logger.info("Cancelling download - no downloaded data during " + NO_DATA_TIMEOUT_LIMIT + " seconds");
                                connectionTimeOut = true;
//                                closeFileStream(fileOutputStream[0]);
//                                fileOutputStream[0] = null;

                                this.cancel();//radsi driv
                                DownloadTask.this.cancel(true);
                                return;
                            }
                        } else {
                            noDataTimeOut = 0;
                            lastSize = counter;
                            setDownloaded(counter);
                        }

                        final long current = System.currentTimeMillis();
                        final float l = (current - time) / (float) 1000.0;

                        if (Float.compare(l, 0) == 0) {
                            setAverageSpeed(0.0F);
                        } else
                            setAverageSpeed((float) counter / l);
                        if (indexer == avgSpeedMeasuredSeconds)
                            indexer = 0;
                        avgSpeedArray[indexer++] = speed;
                        updateShortAvgSpeed(avgSpeedArray);
                    }
                }, 0, 1000);

                //data downloading-------------------------------
                while ((len = inputStream.read(buffer)) != -1) {
                    fileOutputStream[0].write(buffer, 0, len);
                    counter += len;
                    if (isTerminated()) {
                        fileOutputStream[0].flush();
                        break;
                    }
                }
                //-----------------------------------------------

                if (!isTerminated()) {
                    if (counter != fileSize)
                        throw new IOException("ErrorDuringDownload");
                    setDownloaded(fileSize);//100%
                } else {
                    logger.info("File downloading was terminated");
                }
            }
            catch (Exception e) {
                if (storeFile != null && storeFile.exists()) {
                    closeFileStream(fileOutputStream[0]);
                    fileOutputStream[0] = null;

                    storeFile.delete();
                }
                throw e;
            }
            finally {
//                if (timer != null)
//                    timer.cancel();
//                try {
//                    if (inputStream != null)
//                        inputStream.close();
//                } catch (IOException e) {
//                    LogUtils.processException(logger, e);
//                }
                closeFileStream(fileOutputStream[0]);
                fileOutputStream[0] = null;
                checkDeleteTempFile();
            }
        }
        finally {
            setSpeed(0);
            checkDeleteTempFile();
        }

    }

    private void updateShortAvgSpeed(long[] avgSpeedArray) {
        int i = 0;
        long sum = 0;
        for (long l : avgSpeedArray) {
            if (l != -1) {
                sum += l;
                ++i;
            }
        }
        downloadFile.setShortTimeAvgSpeed((i == 0) ? 0 : (float) sum / (float) i);
    }

    protected boolean useTemporaryFiles() {
        return AppPrefs.getProperty(UserProp.USE_TEMPORARY_FILES, true);
    }

    private void closeFileStream(OutputStream fileOutputStream) {
        try {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error closing file stream", e);
        }
    }

    private void checkDeleteTempFile() {
        if (isTerminated() && storeFile.exists()) {
            logger.info("Deleting partial file " + storeFile);
            final boolean b = storeFile.delete();
            if (!b)
                logger.info("Deleting partial file failed (" + storeFile + ")");
        }
    }

    @Override
    protected void cancelled() {
        if (downloadFile != null) {
            if (connectionTimeOut) {//no data in many seconds
                downloadFile.setState(DownloadState.ERROR);
                this.setServiceError(DownloadTaskError.CONNECTION_TIMEOUT);//we try reconnect
            } else
                downloadFile.setState(DownloadState.CANCELLED);
            downloadFile.setDownloaded(0);
            setSpeed(0);
            setAverageSpeed(0);
        }
    }

    protected void setSpeed(final long speedInBytes) {
        Long oldValue, newValue;
        synchronized (this) {
            oldValue = this.speedInBytes;
            this.speedInBytes = speedInBytes;
            newValue = this.speedInBytes;
        }
        firePropertyChange("speed", oldValue, newValue);
    }

    protected void setSleep(final int sleep) {
        Integer oldValue, newValue;
        synchronized (this) {
            oldValue = this.sleep;
            this.sleep = sleep;
            newValue = this.sleep;
        }
        firePropertyChange("sleep", oldValue, newValue);
    }

    protected void setDownloaded(final long counter) {
        downloadFile.setDownloaded(counter);
        //firePropertyChange("downloaded", 0, counter);
    }

    protected void setAverageSpeed(float avgSpeed) {
        final float oldValue, newValue;
        synchronized (this) {
            oldValue = this.averageSpeed;
            this.averageSpeed = avgSpeed;
            newValue = this.averageSpeed;
        }
        firePropertyChange("averageSpeed", oldValue, newValue);
    }

    @Override
    protected void failed(Throwable cause) {
        if (cause instanceof IllegalArgumentException) {
            cause = new PluginImplementationException(service.getName() + " " + downloadFile.getFileUrl().toExternalForm(), cause);
        } else if (!(cause instanceof ErrorDuringDownloadingException)) {
            super.failed(cause);
            LogUtils.processException(logger, cause);
        }
        error(cause);
        if (cause instanceof PluginImplementationException) {
            logger.warning("Content from the last request\n" + client.getContentAsString());
            LogUtils.processException(logger, cause);
        }
        if (cause instanceof NotEnoughSpaceException) {
            Swinger.showErrorMessage(getResourceMap(), "NotEnoughSpaceException", (storeFile != null) ? storeFile : "");
            setServiceError(DownloadTaskError.NOT_RECOVERABLE_DOWNLOAD_ERROR);
        } else if (cause instanceof UnknownHostException) {
            downloadFile.setErrorMessage(getResourceMap().getString("UnknownHostError"));
        } else if (cause instanceof NotRecoverableDownloadException) {
            setServiceError(DownloadTaskError.NOT_RECOVERABLE_DOWNLOAD_ERROR);
        } else if (cause instanceof YouHaveToWaitException) {
            final YouHaveToWaitException waitException = (YouHaveToWaitException) cause;
            this.youHaveToSleepSecondsTime = waitException.getHowManySecondsToWait();
            setServiceError(DownloadTaskError.YOU_HAVE_TO_WAIT_ERROR);
        }
        final boolean connectError = cause instanceof NoRouteToHostException || cause instanceof ConnectException || cause instanceof UnknownHostException;

        if (AppPrefs.getProperty(UserProp.TEST_FILE, UserProp.TEST_FILE_DEFAULT))
            updateFileState(cause, connectError);

        if (AppPrefs.getProperty(UserProp.DISABLE_CONNECTION_ON_EXCEPTION, UserProp.DISABLE_CONNECTION_ON_EXCEPTION_DEFAULT)) {
            if (connectError) {
                setServiceError(DownloadTaskError.NO_ROUTE_TO_HOST);
            }
        }

        if (getServiceError() == DownloadTaskError.NOT_RECOVERABLE_DOWNLOAD_ERROR) {
            downloadFile.setErrorAttemptsCount(0);
            downloadFile.setTimeToQueued(-1);
            downloadFile.setTimeToQueuedMax(-1);
        }
        final Application app = getApplication();
        if (isAllComplete(app)) {
            checkShutDown(app);
        }
    }

    protected void updateFileState(Throwable cause, boolean connectError) {
        if (cause instanceof URLNotAvailableAnymoreException || cause instanceof InvalidURLOrServiceProblemException) {
            downloadFile.setFileState(FileState.FILE_NOT_FOUND);
        } else {
            if (!connectError && (!(cause instanceof CaptchaEntryInputMismatchException)) && (serviceError == DownloadTaskError.NOT_RECOVERABLE_DOWNLOAD_ERROR)) {
                downloadFile.setFileState(FileState.ERROR_GETTING_INFO);
            }
        }
    }


    protected void error(Throwable cause) {

        setFileErrorMessage(cause);
        setServiceError(DownloadTaskError.GENERAL_ERROR);
        if (!(cause instanceof YouHaveToWaitException)) {
            if (AppPrefs.getProperty(UserProp.PLAY_SOUNDS_FAILED, true))
                Sound.playSound(getContext().getResourceMap().getString("errorWav"));
            downloadFile.setState(DownloadState.ERROR);
        } else downloadFile.setState(DownloadState.SLEEPING);
    }

    protected void setFileErrorMessage(Throwable cause) {
        downloadFile.setErrorMessage(Swinger.getMessageFromException(getResourceMap(), cause));
    }

    @Override
    protected void interrupted(InterruptedException e) {
        cancelled();
    }

    @Override
    protected void succeeded(Void result) {
        super.succeeded(result);
        boolean runTask = false;
        boolean overWriteFile = false;
        if (storeFile != null && storeFile.exists()) {
            if (storeFile.equals(outputFile)) //pokud zapisovaci == vystupnimu
            {
                setCompleted();
                return;
            }
            if (outputFile.exists()) {
                //Rename/Overwrite/Skip/Ask

                int property = UserProp.RENAME;
                try {
                    property = fileAlreadyExistsProperty();
                } catch (InvocationTargetException e) {
                    LogUtils.processException(logger, e);
                } catch (InterruptedException e) {
                    LogUtils.processException(logger, e);
                }
                switch (property) {
                    case UserProp.OVERWRITE:
                        runTask = true;
                        overWriteFile = true;
                        break;
                    case UserProp.RENAME:
                        runTask = true;
                        break;
                }
            } else runTask = true;
        }
        if (runTask) {
            runMoveFileTask(overWriteFile);
        } else {
            setCompleted();
            if (storeFile != null && storeFile.exists()) {
                storeFile.delete();
            }
        }
    }

    protected int checkExists() throws InvocationTargetException, InterruptedException {
        if (!outputFile.exists())
            return -2;
        return fileAlreadyExistsProperty();
    }

    private int fileAlreadyExistsProperty() throws InvocationTargetException, InterruptedException {
        if (fileAlreadyExists != -2)
            return fileAlreadyExists;
        final int[] property = new int[]{AppPrefs.getProperty(UserProp.FILE_ALREADY_EXISTS, UserProp.FILE_ALREADY_EXISTS_DEFAULT)};
        if (property[0] == UserProp.ASK) {
            synchronized (DownloadTask.class) {
                if (!EventQueue.isDispatchThread()) {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            property[0] = showFileAlreadyExistsDialog();
                        }
                    });
                } else return showFileAlreadyExistsDialog();
            }
        }
        return property[0];
    }

    private int showFileAlreadyExistsDialog() {
        return Swinger.showOptionDialog(getResourceMap(), JOptionPane.QUESTION_MESSAGE, "errorMessage", "fileAlreadyExists", new String[]{"renameFile", "overWriteFile", "skipFile"}, outputFile);
    }

    private void setCompleted() {
        downloadFile.setCompleteTaskDuration(this.getExecutionDuration(TimeUnit.SECONDS));
        if (downloadFile.getState() != DownloadState.QUEUED)
            downloadFile.setState(DownloadState.COMPLETED);
    }

    private void runMoveFileTask(boolean overWriteFile) {
        final MoveFileTask moveFileTask = new MoveFileTask(getApplication(), storeFile, downloadFile.getOutputFile(), true, overWriteFile, downloadFile);
        moveFileTask.addTaskListener(new TaskListener.Adapter<Void, Void>() {
            public boolean succeeded = false;

            @Override
            public void finished(TaskEvent<Void> event) {
                super.succeeded(event); //???
                if (succeeded) {
                    doAllSucceededActions();
                }
            }


            @Override
            public void succeeded(TaskEvent<Void> event) {
                this.succeeded = true;
                setCompleted();
            }

            @Override
            public void failed(TaskEvent<Throwable> event) {
                downloadFile.setState(DownloadState.ERROR);
                //noinspection ThrowableResultOfMethodCallIgnored
                downloadFile.setErrorMessage(getResourceMap().getString("transferFailed", event.getValue().getMessage()));
                Sound.playSound(getContext().getResourceMap().getString("errorWav"));
            }

            @Override
            public void cancelled(TaskEvent<Void> event) {
                downloadFile.setState(DownloadState.CANCELLED);
            }

            @Override
            public void interrupted(TaskEvent<InterruptedException> event) {
                downloadFile.setState(DownloadState.CANCELLED);
            }
        });
        final MainApp app = (MainApp) this.getApplication();
        final TaskServiceManager serviceManager = app.getManagerDirector().getTaskServiceManager();
        serviceManager.getTaskService(TaskServiceManager.MOVE_FILE_SERVICE).execute(moveFileTask);
    }

    private void doAllSucceededActions() {
        final Application app = getApplication();
        final boolean allComplete = isAllComplete(app);
        if (allComplete) {
            final boolean sound = AppPrefs.getProperty(UserProp.PLAY_SOUNDS_OK, true);
            if (sound)
                Sound.playSound(getContext().getResourceMap().getString("doneWav"));
            checkShutDown(app);
        }
    }

    private void checkShutDown(Application app) {
        if (AppPrefs.getProperty(UserProp.AUTOSHUTDOWN, UserProp.AUTOSHUTDOWN_DEFAULT) != UserProp.AUTOSHUTDOWN_DISABLED) {
            app.getContext().getTaskService().execute(new CloseInTimeTask(app));
        }
    }

    private boolean isAllComplete(Application app) {
        return ((MainApp) app).getManagerDirector().getDataManager().checkComplete();
    }

    public void sleep(int seconds) throws InterruptedException {
        setSleep(0);
        downloadFile.setState(DownloadState.WAITING);
        downloadFile.setTimeToQueuedMax(seconds);
        logger.info("Going to sleep on " + (seconds) + " seconds");
        for (int i = seconds; i > 0; i--) {
            if (isTerminated())
                break;
            setSleep(i);
            Thread.sleep(1000);
        }
        if (isTerminated())
            throw new InterruptedException();
    }

    @Override
    public HttpFile getDownloadFile() {
        return downloadFile;
    }

    @Override
    public HttpDownloadClient getClient() {
        return client;
    }

    public DownloadTaskError getServiceError() {
        return serviceError;
    }

    private void setServiceError(DownloadTaskError serviceError) {
        this.serviceError = serviceError;
    }

    public int getYouHaveToSleepSecondsTime() {
        return youHaveToSleepSecondsTime;
    }

    protected void setDownloadFile(DownloadFile downloadFile) {
        this.downloadFile = downloadFile;
    }


}
