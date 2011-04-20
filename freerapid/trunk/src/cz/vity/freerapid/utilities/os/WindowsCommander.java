package cz.vity.freerapid.utilities.os;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.LibraryLoader;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import cz.vity.freerapid.core.Consts;
import cz.vity.freerapid.utilities.LogUtils;
import cz.vity.freerapid.utilities.Utils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Ladislav Vitasek
 * @author ntoskrnl
 */
final class WindowsCommander extends AbstractSystemCommander {
    private final static Logger logger = Logger.getLogger(WindowsCommander.class.getName());

    static {
        System.setProperty(LibraryLoader.JACOB_DLL_PATH, new File("lib\\" + LibraryLoader.getPreferredDLLName() + ".dll").getAbsolutePath());
    }

    WindowsCommander() {
    }

    @Override
    public boolean isSupported(final OSCommand command) {
        return true;
    }

    @Override
    public boolean createShortCut(final OSCommand shortCutCommand) {
        if (!OSCommand.shortCutCommands.contains(shortCutCommand)) {
            throw new IllegalArgumentException("OS command " + shortCutCommand + " is not a shortcut command");
        }
        switch (shortCutCommand) {
            case CREATE_DESKTOP_SHORTCUT:
                return createShortCut(Shell32Util.getFolderPath(ShlObj.CSIDL_DESKTOP));
            case CREATE_QUICKLAUNCH_SHORTCUT:
                return createShortCut(Utils.addFileSeparator(Shell32Util.getFolderPath(ShlObj.CSIDL_APPDATA)) + "Microsoft\\Internet Explorer\\Quick Launch");
            case CREATE_STARTMENU_SHORTCUT:
                return createShortCut(Shell32Util.getFolderPath(ShlObj.CSIDL_PROGRAMS));
            case CREATE_STARTUP_SHORTCUT:
                return createShortCut(Shell32Util.getFolderPath(ShlObj.CSIDL_STARTUP), "-m");
            default:
                assert false;
                break;
        }
        return false;
    }

    private boolean createShortCut(final String folder) {
        return createShortCut(folder, null);
    }

    private boolean createShortCut(final String folder, final String arguments) {
        final String shortcutFile = folder + "\\" + Consts.APPVERSION + ".lnk";
        final String appPath = Utils.addFileSeparator(Utils.getAppPath());
        final String exe = appPath + Consts.WINDOWS_EXE_NAME;
        final String icon = appPath + Consts.WINDOWS_ICON_NAME;
        ActiveXComponent shell = null;
        ActiveXComponent shortcut = null;
        try {
            shell = new ActiveXComponent("WScript.Shell");
            shortcut = new ActiveXComponent(shell.invoke("CreateShortcut", shortcutFile).getDispatch());
            shortcut.setProperty("TargetPath", exe);
            shortcut.setProperty("WorkingDirectory", appPath);
            shortcut.setProperty("IconLocation", icon);
            shortcut.setProperty("Arguments", arguments);
            shortcut.invoke("Save");
        } finally {
            if (shell != null) shell.safeRelease();
            if (shortcut != null) shortcut.safeRelease();
        }
        return true;
    }

    @Override
    public boolean shutDown(final OSCommand shutDownCommand, final boolean force) {
        if (!OSCommand.shutDownCommands.contains(shutDownCommand)) {
            throw new IllegalArgumentException("OS command " + shutDownCommand + " is not a shut down command");
        }
        switch (shutDownCommand) {
            case RESTART_APPLICATION:
                return startNewApplicationInstance();
            case HIBERNATE:
                return WindowsShutdownUtils.hibernate();
            case STANDBY:
                return WindowsShutdownUtils.standby();
            case REBOOT:
                return WindowsShutdownUtils.reboot(force);
            case SHUTDOWN:
                return WindowsShutdownUtils.shutdown(force);
            default:
                assert false;
                break;
        }
        return false;
    }

    private boolean startNewApplicationInstance() {
        final String exe;
        if (!System.getProperties().containsKey("exePath")) {
            final String appPath = Utils.getAppPath();
            final String appSep = Utils.addFileSeparator(appPath);
            exe = appSep + Consts.WINDOWS_EXE_NAME;
        } else {
            exe = System.getProperty("exePath");
        }
        return run(exe + " " + Utils.getApplicationArguments(), false);
    }

    private static boolean run(final String cmd, final boolean waitForResult) {
        if (!Utils.isWindows())
            return true;
        logger.info("System command: " + cmd);
        try {
            final Process process = Runtime.getRuntime().exec(SysCommand.splitCommand(cmd));
            if (waitForResult) {
                process.waitFor();
                return process.exitValue() == 0;
            } else return true;
        } catch (IOException e) {
            logger.warning("Command: " + cmd + " failed for some reason");
            LogUtils.processException(logger, e);
            return false;
        } catch (InterruptedException e) {
            LogUtils.processException(logger, e);
            return false;
        }
    }

    @Override
    public boolean findTopLevelWindow(final String windowTitle, final boolean caseSensitive) throws IOException {
        final String stringToFind = caseSensitive ? windowTitle : windowTitle.toLowerCase();
        final boolean[] result = {false};
        User32.INSTANCE.EnumWindows(new WinUser.WNDENUMPROC() {
            @Override
            public boolean callback(final WinDef.HWND hwnd, final Pointer pointer) {
                String title = getWindowTitle(hwnd);
                if (title != null) {
                    if (!caseSensitive) {
                        title = title.toLowerCase();
                    }
                    if (title.contains(stringToFind)) {
                        result[0] = true;
                        return false;
                    }
                }
                return true;
            }
        }, Pointer.NULL);
        return result[0];
    }

    @Override
    public List<String> getTopLevelWindowsList() throws IOException {
        final List<String> list = new LinkedList<String>();
        User32.INSTANCE.EnumWindows(new WinUser.WNDENUMPROC() {
            @Override
            public boolean callback(final WinDef.HWND hwnd, final Pointer pointer) {
                final String title = getWindowTitle(hwnd);
                if (title != null) {
                    list.add(title);
                }
                return true;
            }
        }, Pointer.NULL);
        return list;
    }

    private static String getWindowTitle(final WinDef.HWND hwnd) {
        int len = User32.INSTANCE.GetWindowTextLength(hwnd);
        if (len > 0) {
            final char[] name = new char[Math.min(len, 2048)];
            len = User32.INSTANCE.GetWindowText(hwnd, name, name.length);
            if (len > 0) {
                return new String(name, 0, len);
            }
        }
        return null;
    }

}
