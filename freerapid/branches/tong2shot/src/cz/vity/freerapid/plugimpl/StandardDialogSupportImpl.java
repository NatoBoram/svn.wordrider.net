package cz.vity.freerapid.plugimpl;

import cz.vity.freerapid.core.AppPrefs;
import cz.vity.freerapid.core.QuietMode;
import cz.vity.freerapid.core.UserProp;
import cz.vity.freerapid.core.tasks.DownloadTask;
import cz.vity.freerapid.gui.dialogs.AccountDialog;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.DialogSupport;
import cz.vity.freerapid.swing.Swinger;
import cz.vity.freerapid.utilities.Hq2x;
import cz.vity.freerapid.utilities.LogUtils;
import cz.vity.freerapid.utilities.Sound;
import org.jdesktop.appframework.swingx.SingleXFrameApplication;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.SingleFrameApplication;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

/**
 * Standard implementation of DialogSupport
 *
 * @author Ladislav Vitasek
 */
public class StandardDialogSupportImpl implements DialogSupport {

    private final static Logger logger = Logger.getLogger(StandardDialogSupportImpl.class.getName());

    /**
     * result from the user's input for CAPTCHA
     */
    private volatile String captchaResult;
    /**
     * result from the user's input for password
     */
    private volatile String passwordResult;
    /**
     * synchronization lock - to block more than 1 CAPTCHA dialog
     */
    private final static Object captchaLock = new Object();
    /**
     * application context
     */
    private final ApplicationContext context;

    /**
     * Constructor - creates a new StandardDialogSupportImpl instance.
     *
     * @param context application context
     */
    public StandardDialogSupportImpl(final ApplicationContext context) {
        this.context = context;
    }

    @Override
    public PremiumAccount showAccountDialog(final PremiumAccount account, final String title, final boolean emptyAllowed) throws Exception {
        final PremiumAccount[] result = new PremiumAccount[]{null};
        if (!EventQueue.isDispatchThread()) {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    getAccount(title, account, result, emptyAllowed);
                }
            });
        } else getAccount(title, account, result, emptyAllowed);

        return result[0];
    }

    @Override
    public PremiumAccount showAccountDialog(final PremiumAccount account, final String title) throws Exception {
        return showAccountDialog(account, title, false);
    }

    @Override
    public boolean showOKCancelDialog(final Component container, final String title) throws Exception {
        final boolean[] dialogResult = new boolean[]{false};
        final Runnable runable = new Runnable() {
            @Override
            public void run() {
                dialogResult[0] = Swinger.showInputDialog(title, container, true) == Swinger.RESULT_OK;
            }
        };
        if (!EventQueue.isDispatchThread()) {
            SwingUtilities.invokeAndWait(runable);
        } else runable.run();
        return dialogResult[0];
    }

    @Override
    public void showOKDialog(final Component container, final String title) throws Exception {
        if (!EventQueue.isDispatchThread()) {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    Swinger.showInputDialog(title, container, false);
                }
            });
        } else Swinger.showInputDialog(title, container, false);
    }

    @Override
    public String askForCaptcha(final BufferedImage image) throws Exception {
        synchronized (captchaLock) {
            captchaResult = "";
            if (!EventQueue.isDispatchThread()) {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        askCaptcha(image);
                    }
                });
            } else askCaptcha(image);
            return captchaResult;
        }
    }

    private void askCaptcha(BufferedImage image) {
        if (AppPrefs.getProperty(UserProp.ZOOM_CAPTCHA_IMAGE, UserProp.ZOOM_CAPTCHA_IMAGE_DEFAULT)) {
            image = Hq2x.zoom(image);
        }
        boolean bringToFront = false;
        if (!QuietMode.getInstance().isActive() || !QuietMode.getInstance().isCaptchaDisabled()) {
            Swinger.bringToFront(((SingleFrameApplication) context.getApplication()).getMainFrame(), true);
            bringToFront = true;
        } else {
            QuietMode.getInstance().playUserInteractionRequiredSound();
        }
        if (AppPrefs.getProperty(UserProp.BLIND_MODE, UserProp.BLIND_MODE_DEFAULT)) {
            Sound.playSound(context.getResourceMap().getString("captchaWav"));
        }
        Component parentComponent;
        if (AppPrefs.getProperty(UserProp.POP_WINDOW_WITHOUT_MAIN_WINDOW_IN_QUIET_MODE, UserProp.POP_WINDOW_WITHOUT_MAIN_WINDOW_IN_QUIET_MODE_DEFAULT)) {
            parentComponent = null;
        } else
            parentComponent = (bringToFront) ? null : ((SingleFrameApplication) context.getApplication()).getMainFrame();
        captchaResult = (String) JOptionPane.showInputDialog(parentComponent, context.getResourceMap(DownloadTask.class).getString("InsertWhatYouSee"), context.getResourceMap(DownloadTask.class).getString("InsertCaptcha"), JOptionPane.PLAIN_MESSAGE, new ImageIcon(image), null, null);
    }

    private void getAccount(String title, PremiumAccount account, PremiumAccount[] result, boolean emptyAllowed) {
        final SingleXFrameApplication app = (SingleXFrameApplication) context.getApplication();
        final AccountDialog dialog = new AccountDialog(app.getMainFrame(), title, account, emptyAllowed);
        try {
            app.prepareDialog(dialog, true);
        } catch (IllegalStateException e) {
            LogUtils.processException(logger, e);
        }
        result[0] = dialog.getAccount();
    }

    private void getAccount(String title, PremiumAccount account, PremiumAccount[] result) {
        getAccount(title, account, result, false);
    }

    @Override
    public String askForPassword(final String name) throws Exception {
        synchronized (captchaLock) {
            passwordResult = "";
            if (!EventQueue.isDispatchThread()) {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        askPassword(name);
                    }
                });
            } else askPassword(name);
            return passwordResult;
        }
    }

    private void askPassword(final String name) {
        boolean bringToFront = false;
        if (!QuietMode.getInstance().isActive() || !QuietMode.getInstance().isDialogsDisabled()) {
            bringToFront = true;
            final JFrame mainFrame = ((SingleFrameApplication) context.getApplication()).getMainFrame();
            Swinger.bringToFront(mainFrame, true);
        } else {
            QuietMode.getInstance().playUserInteractionRequiredSound();
        }
        /*
        if (AppPrefs.getProperty(UserProp.BLIND_MODE, UserProp.BLIND_MODE_DEFAULT)) {
            Sound.playSound(context.getResourceMap().getString("captchaWav"));
        }
        */
        final Component parentComponent = (bringToFront) ? null : ((SingleFrameApplication) context.getApplication()).getMainFrame();
        passwordResult = (String) JOptionPane.showInputDialog(parentComponent, context.getResourceMap(DownloadTask.class).getString("FileIsPasswordProtected", name), context.getResourceMap(DownloadTask.class).getString("InsertPassword"), JOptionPane.PLAIN_MESSAGE, null, null, null);
    }

}
