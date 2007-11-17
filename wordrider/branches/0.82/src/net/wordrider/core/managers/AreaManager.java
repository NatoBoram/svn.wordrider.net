package net.wordrider.core.managers;

import net.wordrider.area.RiderArea;
import net.wordrider.core.AppPrefs;
import net.wordrider.core.MainApp;
import net.wordrider.core.managers.interfaces.IAreaChangeListener;
import net.wordrider.core.managers.interfaces.IFileChangeListener;
import net.wordrider.core.managers.interfaces.IFileInstance;
import net.wordrider.core.managers.interfaces.InstanceListener;
import net.wordrider.utilities.LogUtils;
import net.wordrider.utilities.Swinger;
import org.noos.xing.mydoggy.Content;
import org.noos.xing.mydoggy.ContentManager;
import org.noos.xing.mydoggy.ContentManagerListener;
import org.noos.xing.mydoggy.event.ContentManagerEvent;
import org.noos.xing.mydoggy.plaf.MyDoggyToolWindowManager;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * @author Vity
 */
public final class AreaManager implements InstanceListener {
    private final static Logger logger = Logger.getLogger(AreaManager.class.getName());
    //private final ManagerDirector director;
    private final EventListenerList listenerList = new EventListenerList();
    //    private final Collection<IAreaChangeListener> areaChangelisteners = new ArrayList<IAreaChangeListener>(4);
    //    private final Collection<IFileChangeListener> fileStatusListeners = new HashSet<IFileChangeListener>(2);
    private final RecentFilesManager recentFilesManager;

    final Collection<FileInstance> runningInstancesIDs = new HashSet<FileInstance>(4);


    private static int anIDCounter = 0;
    private MyDoggyToolWindowManager toolWindowManager;

    public AreaManager(final ManagerDirector director) {
        super();    //call to super
        //  this.director = director;
        recentFilesManager = new RecentFilesManager(director.getMenuManager());
        toolWindowManager = director.getDockingWindowManager();
        addFileChangeListener(recentFilesManager);
        toolWindowManager.getContentManager().addContentManagerListener(new ContentManagerListener() {
            public void contentAdded(ContentManagerEvent event) {

            }

            public void contentRemoved(ContentManagerEvent event) {

            }

            public void contentSelected(ContentManagerEvent event) {
                final Content content = event.getContent();
                if (content != null) {
                    activateInstance((FileInstance) content.getKey());
                } else {
//                    getActiveInstance()
//                    if (activeInstanceID != null) {
//                        deactivateInstance(activeInstanceID);
//                        activeInstanceID = null;
//                    }
                }

            }
        });
    }


    public final void activateInstance(IFileInstance instance) {
        if (instance.equals(getActiveInstance()))
            return;
        for (FileInstance fileInstance : runningInstances()) {
            if (fileInstance.equals(instance))
                setActivateFileInstance(fileInstance);
        }
    }

    public FileInstance getActiveInstance() {
        final Content content = toolWindowManager.getContentManager().getSelectedContent();
        if (content != null)
            return (FileInstance) content.getKey();
        return null;
    }

    private Collection<FileInstance> runningInstances() {
        final Content[] contents = toolWindowManager.getContentManager().getContents();
        final Collection<FileInstance> runningIds = new ArrayList<FileInstance>(contents.length);
        for (Content content : contents) {
            runningIds.add((FileInstance) content.getKey());
        }
        return runningIds;
    }

    final public IFileInstance isFileAlreadyOpened(final File f) {
        for (IFileInstance instance : runningInstances()) {
            if (f.equals(instance.getFile()))
                return instance;
        }
        return null;
    }

    final public void openFileInstance() {
        openFileInstance(new FileInstance());
        //  activateInstance(id, instance);
        // MainApp.getMainApp().getMainAppFrame().setTitle();
        //tabbedPane.validate();
        //        tabbedPane.repaint();
    }

    final public void openFileInstance(final FileInstance fileInstance) {
        final Integer id = registerNewOne(fileInstance);
        fileInstance.setInternalId(id);
        fireFileOpened(fileInstance);
        fileInstance.addInstanceListener(this);
//        for (IFileChangeListener fileStatusListener : fileStatusListeners)
//            (fileStatusListener).fileWasOpened(fileInstance);
        //        fileInstance.getRiderArea().grabFocus();
        //        SwingUtilities.invokeLater(
        //                new Runnable() {
        //                    public void run() {
        //                        fileInstance.getRiderArea().grabFocus();
        //                    }
        //                }
        //        );
        //  activateInstance(id, instance);
        // MainApp.getMainApp().getMainAppFrame().setTitle();
        //tabbedPane.validate();
        //        tabbedPane.repaint();
    }

    private Integer registerNewOne(FileInstance instance) {
        final ContentManager contentManager = toolWindowManager.getContentManager();
        final Content content = contentManager.addContent(instance, instance.getTabName(), instance.getIcon(), instance.getComponent(), instance.getTip());
        runningInstancesIDs.add(instance);
        return nextID();
    }


    final protected void deactivateInstance(final FileInstance instance) {
        instance.deactivate();
        fireAreaDeactivated();
    }

    final protected void activateInstance(final FileInstance instance) {
        instance.activate();
        fireAreaActivated();
    }

    private void fireAreaActivated() {
        final IFileInstance instance = getActiveInstance();
        if (instance == null)
            return;
        Object[] listeners = this.listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        AreaChangeEvent event = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == IAreaChangeListener.class) {
                // Lazily create the event:
                if (event == null)
                    event = new AreaChangeEvent(this, instance);
                ((IAreaChangeListener) listeners[i + 1]).areaActivated(event);
            }
        }
    }

    private void fireFileOpened(final IFileInstance fileInstance) {
        Object[] listeners = this.listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        FileChangeEvent event = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == IFileChangeListener.class) {
                // Lazily create the event:
                if (event == null)
                    event = new FileChangeEvent(this, fileInstance);
                ((IFileChangeListener) listeners[i + 1]).fileWasOpened(event);
            }
        }
    }

    private void fireAreaDeactivated() {
        final IFileInstance instance = getActiveInstance();
        assert instance != null;
        Object[] listeners = this.listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        AreaChangeEvent event = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == IAreaChangeListener.class) {
                // Lazily create the event:
                if (event == null)
                    event = new AreaChangeEvent(this, instance);
                ((IAreaChangeListener) listeners[i + 1]).areaDeactivated(event);
            }
        }
    }

    private void setTabTitle(final FileInstance instance) {
        toolWindowManager.getContentManager().getContent(instance).setTitle(instance.getTabName());
    }

    final void addAreaChangeListener(final IAreaChangeListener listener) {
        listenerList.add(IAreaChangeListener.class, listener);
    }

    final public void addFileChangeListener(final IFileChangeListener listener) {
        listenerList.add(IFileChangeListener.class, listener);
    }

    public final Collection<FileInstance> getModifiedInstances() {
        final Collection<FileInstance> list = new ArrayList<FileInstance>();
        for (FileInstance fi : runningInstances()) {
            if (fi.isModified())
                list.add(fi);
        }
        return list;
    }

    public void updateHighlightCurrentLine() {
        final boolean property = AppPrefs.getProperty(AppPrefs.HIGHLIGHT_LINE, true);
        for (IFileInstance instance : runningInstances()) {
            ((RiderArea) instance.getRiderArea()).setCurrentLineHighlight(property);
        }
        repaintActive();
    }

    public void repaintActive() {
        final IFileInstance instance = getActiveInstance();
        if (instance != null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    instance.getRiderArea().repaint();
                }
            });
        }
    }

    public void updateBracketMatching() {
        final boolean property = AppPrefs.getProperty(AppPrefs.MATCH_BRACKETS, true);
        for (IFileInstance<RiderArea> instance : runningInstances()) {
            (instance.getRiderArea()).setBracketMatching(property);
        }
        repaintActive();
    }


    public final void setActivateFileInstance(final FileInstance instance) {
        final ContentManager contentManager = toolWindowManager.getContentManager();
        final Content content = contentManager.getContent(instance);
        if (content != null)
            content.setSelected(true);
    }

    public final void closeInstanceHard(final FileInstance instance) {
        instance.removeInstanceListener(this);
        closeHard(instance);
        fireFileClosed(instance);
    }

    private void fireFileClosed(IFileInstance instance) {
        Object[] listeners = this.listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        FileChangeEvent event = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == IFileChangeListener.class) {
                // Lazily create the event:
                if (event == null)
                    event = new FileChangeEvent(this, instance);
                ((IFileChangeListener) listeners[i + 1]).fileWasClosed(event);
            }
        }
    }

    public final boolean hasModifiedInstances() {
        for (FileInstance fi : runningInstances()) {
            if (fi.isModified())
                return true;
        }
        return false;
    }


    final void closeSoft(final FileInstance instance, final boolean removeTab) {
        if (instance == null || !runningInstancesIDs.contains(instance))
            return;

        boolean result = false;
        try {
            result = instance.closeSoft();
        } catch (Throwable throwable) {
            LogUtils.processException(logger, throwable);
        }
        if (result && removeTab)
            removeInstance(instance);
    }

    final void closeHard(final FileInstance anID) {
        if (anID == null || !runningInstancesIDs.contains(anID))
            return;
        removeInstance(anID);
    }

    private synchronized void removeInstance(final FileInstance instance) {
        runningInstancesIDs.remove(instance);
        final ContentManager contentManager = toolWindowManager.getContentManager();
        final Content content = contentManager.getContent(instance);
        contentManager.removeContent(content);
    }

    public final void closeActiveInstance() {
        final FileInstance fileInstance = getActiveInstance();
        if (fileInstance != null) {
            closeSoft(fileInstance, true);
            fireFileClosed(fileInstance);
        }
    }

    public RecentFilesManager getRecentFilesManager() {
        return recentFilesManager;
    }

    public final int getOpenedInstanceCount() {
        return runningInstancesIDs.size();
    }

    public boolean hasOpenedInstance() {
        return this.getOpenedInstanceCount() > 0;
    }


    public void grabActiveFocus() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final IFileInstance instance = getActiveInstance();
                if (instance != null)
                    grabActiveFocus(instance);
            }
        });
    }

    public static void grabActiveFocus(final IFileInstance instance) {
        Swinger.inputFocus(instance.getRiderArea());
    }


    public void applyTabLayout() {
//        tabbedPane.setTabLayoutPolicy(AppPrefs.getProperty(AppPrefs.SCROLL_LAYOUT, true) ? JTabbedPane.SCROLL_TAB_LAYOUT : JTabbedPane.WRAP_TAB_LAYOUT);
    }

    public static AreaManager getInstance() {
        return MainApp.getInstance().getMainAppFrame().getManagerDirector().getAreaManager();
    }

    public void instanceModifiedStatusChanged(InstanceEvent e) {
        fileAssigned(e);
    }

    public void fileAssigned(InstanceEvent e) {
        setTabTitle((FileInstance) e.getSource());
    }

    private static synchronized Integer nextID() {
        return ++anIDCounter;
    }

    public Collection<FileInstance> getOpenedInstances() {
        return runningInstances();
    }

    public final void getPrevTab() {
        final Content previousContent = toolWindowManager.getContentManager().getPreviousContent();
        if (previousContent != null)
            previousContent.setSelected(true);
    }


    public final void getNextTab() {
        final Content nextContent = toolWindowManager.getContentManager().getNextContent();
        if (nextContent != null)
            nextContent.setSelected(true);
    }

}
