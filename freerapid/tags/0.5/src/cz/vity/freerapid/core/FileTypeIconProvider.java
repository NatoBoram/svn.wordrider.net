package cz.vity.freerapid.core;

import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.ResourceMap;
import sun.awt.shell.ShellFolder;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vity
 */
public class FileTypeIconProvider {
    private ResourceMap map;
    private HashSet<String> supportedTypes;
    private final static Logger logger = Logger.getLogger(FileTypeIconProvider.class.getName());
    private static Pattern pattern;
    private static final String DEFAULT_EXTENSION = "iso";
    private final Map<String, Icon> systemLargeIcons = new Hashtable<String, Icon>();
    private final Map<String, Icon> systemSmallIcons = new Hashtable<String, Icon>();


    public FileTypeIconProvider(ApplicationContext context) {
        map = context.getResourceMap();
        final String[] strings = (String[]) map.getObject("fileTypes", String[].class);
        supportedTypes = new HashSet<String>(Arrays.asList(strings));
        StringBuilder builder = new StringBuilder();
        builder.append("(\\.|_)(");
        for (int i = 0; i < strings.length; i++) {
            String item = strings[i];
            builder.append(item);
            if (i != strings.length - 1) {
                builder.append("|");
            }
        }
        builder.append(")(\\.?|_?|$)");
        final String regexp = builder.toString();
        logger.info("Regexp " + regexp);
        pattern = Pattern.compile(regexp);
    }

    public static String identifyFileType(String fileName) {
        if (pattern == null)
            throw new IllegalStateException("Not initialized yet");
        if (fileName == null)
            return DEFAULT_EXTENSION;
        fileName = fileName.toLowerCase();

        final Matcher matcher = pattern.matcher(fileName);
        final String fileType;
        if (matcher.find()) {
            fileType = matcher.group(2);
        } else {
            fileType = DEFAULT_EXTENSION;
        }
        logger.info("Found file type for file " + fileName + " (" + fileType + ")");
        return fileType;
    }

    public static String identifyFileName(String url) {
        int indexFrom = url.length();
        if (url.endsWith("/"))
            --indexFrom;
        final int foundIndex = url.lastIndexOf("/", indexFrom);
        if (foundIndex >= 0) {
            return url.substring(foundIndex + 1, indexFrom);
        } else return "";
    }

    public Icon getIconImageByFileType(String fileType, boolean bigImage) {
        if (fileType == null)
            return null;
        if (AppPrefs.getProperty(UserProp.USE_SYSTEM_ICONS, true)) {
            fileType = fileType.toLowerCase();

            if (bigImage)
                return getBigSystemIcon(fileType);
            else
                return getSmallSystemIcon(fileType);

        } else {


            fileType = fileType.toUpperCase();


            final String base;
            if (bigImage) {
                base = "iconFileTypeBig_" + fileType;
            } else {
                base = "iconFileTypeSmall_" + fileType;
            }
            if (map.containsKey(base)) {
                return map.getImageIcon(base);
            } else {
                if (bigImage)
                    return map.getImageIcon("iconFileTypeBig_ISO");
                else
                    return map.getImageIcon("iconFileTypeSmall_ISO");
            }
        }
    }

    private Icon getSmallSystemIcon(String extension) {
        if (this.systemSmallIcons.containsKey(extension))
            return systemSmallIcons.get(extension);
        try {
//Create a temporary file with the specified extension
            File file = File.createTempFile("icon", "." + extension);

            FileSystemView view = FileSystemView.getFileSystemView();
            Icon icon = view.getSystemIcon(file);

            //Delete the temporary file
            systemSmallIcons.put(extension, icon);
            file.delete();
            return icon;
        } catch (IOException e) {
            return map.getImageIcon("iconFileTypeSmall_ISO");
        }
    }

    private Icon getBigSystemIcon(String extension) {
        if (this.systemLargeIcons.containsKey(extension))
            return systemLargeIcons.get(extension);
        try {
            File file = File.createTempFile("icon", "." + extension);

            ShellFolder shellFolder = ShellFolder.getShellFolder(file);
            Icon icon = new ImageIcon(shellFolder.getIcon(true));

            //Delete the temporary file
            systemLargeIcons.put(extension, icon);
            file.delete();
            return icon;
        } catch (IOException e) {
            return map.getImageIcon("iconFileTypeBig_ISO");
        }
    }
}
