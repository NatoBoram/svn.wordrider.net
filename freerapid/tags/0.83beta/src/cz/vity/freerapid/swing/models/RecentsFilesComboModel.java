package cz.vity.freerapid.swing.models;

import cz.vity.freerapid.core.AppPrefs;
import cz.vity.freerapid.core.UserProp;
import cz.vity.freerapid.gui.FRDUtils;
import cz.vity.freerapid.utilities.FileUtils;
import cz.vity.freerapid.utilities.Utils;

import javax.swing.*;
import java.io.File;
import java.util.*;

/**
 * @author Vity
 */
public final class RecentsFilesComboModel extends DefaultComboBoxModel {

    private final Stack<String> stack;
    private String keyProperties = null;
    private boolean autosave;
    private int maxRecentPhrasesCount;


    public RecentsFilesComboModel(final String keyProperties, final boolean autosave) {
        this(new Stack<String>());
        this.maxRecentPhrasesCount = AppPrefs.getProperty(UserProp.MAX_RECENT_PHRASES_COUNT, UserProp.MAX_RECENT_PHRASES_COUNT_DEFAULT);
        this.keyProperties = keyProperties;
        this.autosave = autosave;
        final String[] values = AppPrefs.getProperty(keyProperties, "").split("\\|");
        for (String value : values) {
            final File file = new File(value);
            if (!file.exists())
                continue;
            if (value.length() > 0) {
                stack.add(0, FileUtils.getAbsolutPath(file));
//                System.out.println("Loading :" + searched);
                //       AppPrefs.removeProperty(key);
            }
        }
    }

    private RecentsFilesComboModel(final Stack<String> v) {
        super(v);    //call to super
        this.stack = v;
        autosave = false;
    }

    public void setSelectedItem(Object anObject) {
        super.setSelectedItem(anObject);
        addElement(anObject);
    }

    public final void addElement(final Object anObject) {
        if (anObject == null)
            return;
        final int index = getIndexOf(anObject);
        if (index < 0) {
            final String s = anObject.toString().trim();
            if (!"".equals(s) && !"?".equals(s)) {
                //    if (!(new File(s).exists())) {
                if (!getNormalizedFiles(stack).contains(new File(s))) {
                    super.insertElementAt(anObject, 0);
                    if (stack.size() > maxRecentPhrasesCount) {
                        this.remove(stack.size() - 1);
                        if (autosave)
                            storeFiles();
                    }
                }
                //    }
            }
            if (autosave)
                storeFiles();
        }

    }


    private void remove(int index) {
        setSelectedItem(getElementAt(0));
        stack.removeElementAt(index);
        fireIntervalRemoved(this, index, index);
    }

    public final Collection<String> getList() {
        return stack;
    }

    public void removeAllProperties() {
        AppPrefs.removeProperty(keyProperties);
    }

    Set<File> getNormalizedFiles(Collection<String> col) {
        final Set<File> set = new HashSet<File>(col.size());
        final boolean isWindows = Utils.isWindows();
        for (String str : col) {
            if (isWindows && !str.endsWith("\\"))
                str = str + "\\";
            final File file = new File(str);
            if (!set.contains(file)) {
                set.add(file);
            }
        }
        return set;
    }


    public void storeFiles() {
        final StringBuilder builder = new StringBuilder();
        for (final Iterator<File> it = getNormalizedFiles(stack).iterator(); it.hasNext();) {
            File str = it.next();
            builder.append(FRDUtils.getAbsRelPath(str));
            if (it.hasNext())
                builder.append("|");
        }

        AppPrefs.storeProperty(keyProperties, builder.toString());

    }
}