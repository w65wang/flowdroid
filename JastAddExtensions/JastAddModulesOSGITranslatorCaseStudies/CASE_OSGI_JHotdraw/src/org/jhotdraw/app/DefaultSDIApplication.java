/*
 * @(#)DefaultSDIApplication.java  1.5  2007-12-25
 *
 * Copyright (c) 1996-2007 by the original authors of JHotDraw
 * and all its contributors.
 * All rights reserved.
 *
 * The copyright of this software is owned by the authors and  
 * contributors of the JHotDraw project ("the copyright holders").  
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * the copyright holders. For details see accompanying license terms. 
 */
package org.jhotdraw.app;

import org.jhotdraw.util.*;
import org.jhotdraw.util.prefs.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import java.util.prefs.*;
import javax.swing.*;
import org.jhotdraw.app.action.*;

/**
 * A DefaultSDIApplication can handle the life cycle of a single document window being
 * presented in a JFrame. The JFrame provides all the functionality needed to
 * work with the document, such as a menu bar, tool bars and palette windows.
 *
 *
 * @author Werner Randelshofer
 * @version 1.5 2007-12-25 Added method updateViewTitle. Replaced 
 * currentProject by activeProject in super class. 
 * <br>1.4 2007-01-11 Removed method addStandardActionsTo.
 * <br>1.3 2006-05-03 Show asterisk in window title, when view has
 * unsaved changes.
 * <br>1.2.1 2006-02-28 Stop application when last view is closed.
 * <br>1.2 2006-02-06 Support for multiple open id added.
 * <br>1.1 2006-02-06 Revised.
 * <br>1.0 October 16, 2005 Created.
 */
public class DefaultSDIApplication extends AbstractApplication {

    private Preferences prefs;

    /** Creates a new instance. */
    public DefaultSDIApplication() {
    }

    public void launch(String[] args) {
        System.setProperty("apple.awt.graphics.UseQuartz", "false");
        super.launch(args);
    }

    public void init() {
        initLookAndFeel();
        super.init();
        prefs = Preferences.userNodeForPackage((getModel() == null) ? getClass() : getModel().getClass());
        initLabels();
        initApplicationActions();
    }

    public void remove(View p) {
        super.remove(p);
        if (views().size() == 0) {
            stop();
        }
    }

    public void configure(String[] args) {
        System.setProperty("apple.laf.useScreenMenuBar", "false");
        System.setProperty("com.apple.macos.useScreenMenuBar", "false");
        System.setProperty("apple.awt.graphics.UseQuartz", "false");
        System.setProperty("swing.aatext", "true");
    }

    protected void initLookAndFeel() {
        try {
            String lafName;
            if (System.getProperty("os.name").toLowerCase().startsWith("mac os x")) {
                JFrame.setDefaultLookAndFeelDecorated(true);
                JDialog.setDefaultLookAndFeelDecorated(true);
                lafName = UIManager.getCrossPlatformLookAndFeelClassName();
            } else {
                lafName = UIManager.getSystemLookAndFeelClassName();
            }
            UIManager.setLookAndFeel(lafName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (UIManager.getString("OptionPane.css") == null) {
            UIManager.put("OptionPane.css", "");
        }
    }

    protected void initApplicationActions() {
        ResourceBundleUtil appLabels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.app.Labels");
        ApplicationModel m = getModel();
        m.putAction(AboutAction.ID, new AboutAction(this));
        m.putAction(ExitAction.ID, new ExitAction(this));

        m.putAction(ClearAction.ID, new ClearAction(this));
        m.putAction(NewAction.ID, new NewAction(this));
        appLabels.configureAction(m.getAction(NewAction.ID), "newWindow");
        m.putAction(LoadAction.ID, new LoadAction(this));
        m.putAction(ClearRecentFilesAction.ID, new ClearRecentFilesAction(this));
        m.putAction(SaveAction.ID, new SaveAction(this));
        m.putAction(SaveAsAction.ID, new SaveAsAction(this));
        m.putAction(CloseAction.ID, new CloseAction(this));
        m.putAction(PrintAction.ID, new PrintAction(this));

        m.putAction(UndoAction.ID, new UndoAction(this));
        m.putAction(RedoAction.ID, new RedoAction(this));
        m.putAction(CutAction.ID, new CutAction());
        m.putAction(CopyAction.ID, new CopyAction());
        m.putAction(PasteAction.ID, new PasteAction());
        m.putAction(DeleteAction.ID, new DeleteAction());
        m.putAction(DuplicateAction.ID, new DuplicateAction());
        m.putAction(SelectAllAction.ID, new SelectAllAction());
    }

    protected void initViewActions(View p) {
        ApplicationModel m = getModel();
        p.putAction(LoadAction.ID, m.getAction(LoadAction.ID));
    }

    public void show(final View p) {
        if (!p.isShowing()) {
            p.setShowing(true);
            final JFrame f = new JFrame();
            f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            updateViewTitle(p, f);

            JPanel panel = (JPanel) wrapViewComponent(p);
            f.add(panel);
            f.setMinimumSize(new Dimension(200, 200));
            f.setPreferredSize(new Dimension(600, 400));

            f.setJMenuBar(createMenuBar(p, (java.util.List<Action>) panel.getClientProperty("toolBarActions")));

            PreferencesUtil.installFramePrefsHandler(prefs, "view", f);
            Point loc = f.getLocation();
            boolean moved;
            do {
                moved = false;
                for (Iterator i = views().iterator(); i.hasNext();) {
                    View aView = (View) i.next();
                    if (aView != p &&
                            SwingUtilities.getWindowAncestor(aView.getComponent()) != null &&
                            SwingUtilities.getWindowAncestor(aView.getComponent()).
                            getLocation().equals(loc)) {
                        loc.x += 22;
                        loc.y += 22;
                        moved = true;
                        break;
                    }
                }
            } while (moved);
            f.setLocation(loc);

            f.addWindowListener(new WindowAdapter() {

                public void windowClosing(final WindowEvent evt) {
                    getModel().getAction(CloseAction.ID).actionPerformed(
                            new ActionEvent(f, ActionEvent.ACTION_PERFORMED,
                            "windowClosing"));
                }

                @Override
                public void windowClosed(final WindowEvent evt) {
                    if (p == getActiveView()) {
                        setActiveView(null);
                    }
                    p.stop();
                }

                public void windowActivated(WindowEvent e) {
                    setActiveView(p);
                }
            });

            p.addPropertyChangeListener(new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent evt) {
                    String name = evt.getPropertyName();
                    if (name.equals(View.HAS_UNSAVED_CHANGES_PROPERTY) ||
                            name.equals(View.FILE_PROPERTY) ||
                            name.equals(View.MULTIPLE_OPEN_ID_PROPERTY)) {
                        updateViewTitle(p, f);
                    }
                }
            });

            f.setVisible(true);
            p.start();
        }
    }

    /**
     * Returns the view component. Eventually wraps it into
     * another component in order to provide additional functionality.
     */
    protected Component wrapViewComponent(View p) {
        JComponent c = p.getComponent();
        if (getModel() != null) {
            LinkedList<Action> toolBarActions = new LinkedList();

            int id = 0;
            for (JToolBar tb : new ReversedList<JToolBar>(getModel().createToolBars(this, p))) {
                id++;
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(tb, BorderLayout.NORTH);
                panel.add(c, BorderLayout.CENTER);
                c = panel;
                PreferencesUtil.installToolBarPrefsHandler(prefs, "toolbar." + id, tb);
                toolBarActions.addFirst(new ToggleVisibleAction(tb, tb.getName()));
            }
            c.putClientProperty("toolBarActions", toolBarActions);
        }
        return c;
    }

    public void hide(View p) {
        if (p.isShowing()) {
            p.setShowing(false);
            JFrame f = (JFrame) SwingUtilities.getWindowAncestor(p.getComponent());
            f.setVisible(false);
            f.remove(p.getComponent());
            f.dispose();
        }
    }

    public void dispose(View p) {
        super.dispose(p);
        if (views().size() == 0) {
            stop();
        }
    }

    /**
     * The view menu bar is displayed for a view.
     * The default implementation returns a new screen menu bar.
     */
    protected JMenuBar createMenuBar(final View p, java.util.List<Action> toolBarActions) {
        JMenuBar mb = new JMenuBar();
        mb.add(createFileMenu(p));
        JMenu lastMenu = null;
        for (JMenu mm : getModel().createMenus(this, p)) {
            mb.add(mm);
            lastMenu = mm;
        }
        JMenu viewMenu = createViewMenu(p, toolBarActions);
        if (lastMenu != null && lastMenu.getText().equals(viewMenu.getText())) {
            for (Component c : lastMenu.getMenuComponents()) {
                viewMenu.add(c);
            }
            mb.remove(lastMenu);
        }
        mb.add(viewMenu);
        mb.add(createHelpMenu(p));
        return mb;
    }

    protected JMenu createFileMenu(final View p) {
        ApplicationModel model = getModel();
        ResourceBundleUtil labels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.app.Labels");

        JMenuBar mb = new JMenuBar();
        JMenu m;
        JMenuItem mi;
        final JMenu openRecentMenu;

        m = new JMenu();
        labels.configureMenu(m, labels.getString("file"));
        m.add(model.getAction(ClearAction.ID));
        m.add(model.getAction(NewAction.ID));
        m.add(model.getAction(LoadAction.ID));
        openRecentMenu = new JMenu();
        labels.configureMenu(openRecentMenu, "openRecent");
        openRecentMenu.add(model.getAction(ClearRecentFilesAction.ID));
        updateOpenRecentMenu(openRecentMenu);
        m.add(openRecentMenu);
        m.addSeparator();
        m.add(model.getAction(SaveAction.ID));
        m.add(model.getAction(SaveAsAction.ID));
        if (model.getAction(ExportAction.ID) != null) {
            mi = m.add(model.getAction(ExportAction.ID));
        }
        if (model.getAction(PrintAction.ID) != null) {
            m.addSeparator();
            m.add(model.getAction(PrintAction.ID));
        }
        m.addSeparator();
        m.add(model.getAction(ExitAction.ID));
        mb.add(m);

        addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                String name = evt.getPropertyName();
                if (name == "viewCount") {
                    if (p == null || views().contains(p)) {
                    } else {
                        removePropertyChangeListener(this);
                    }
                } else if (name == "recentFiles") {
                    updateOpenRecentMenu(openRecentMenu);
                }
            }
        });

        return m;
    }

    /**
     * Updates the title of a view and displays it in the given frame.
     * 
     * @param p The view.
     * @param f The frame.
     */
    protected void updateViewTitle(View p, JFrame f) {
        File file = p.getFile();
        String title;
        if (file == null) {
            title = labels.getString("unnamedFile");
        } else {
            title = file.getName();
        }
        if (p.hasUnsavedChanges()) {
            title += "*";
        }
        p.setTitle(labels.getFormatted("frameTitle", title, getName(), p.getMultipleOpenId()));
        f.setTitle(p.getTitle());
    }

    /**
     * Updates the "file &gt; open recent" menu item.
     * 
     * @param openRecentMenu
     */
    protected void updateOpenRecentMenu(JMenu openRecentMenu) {
        if (openRecentMenu.getItemCount() > 0) {
            JMenuItem clearRecentFilesItem = (JMenuItem) openRecentMenu.getItem(
                    openRecentMenu.getItemCount() - 1);
            openRecentMenu.removeAll();
            for (File f : recentFiles()) {
                openRecentMenu.add(new LoadRecentAction(DefaultSDIApplication.this, f));
            }
            if (recentFiles().size() > 0) {
                openRecentMenu.addSeparator();
            }
            openRecentMenu.add(clearRecentFilesItem);
        }
    }

    public boolean isSharingToolsAmongViews() {
        return false;
    }

    public Component getComponent() {
        View p = getActiveView();
        return (p == null) ? null : p.getComponent();
    }

    protected JMenu createViewMenu(final View p, java.util.List<Action> toolBarActions) {
        ApplicationModel model = getModel();
        ResourceBundleUtil labels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.app.Labels");

        JMenuBar mb = new JMenuBar();
        JMenu m, m2;
        JMenuItem mi;
        JCheckBoxMenuItem cbmi;
        final JMenu openRecentMenu;

        m = new JMenu();
        if (toolBarActions != null && toolBarActions.size() > 0) {
            m2 = (toolBarActions.size() == 1) ? m : new JMenu(labels.getString("toolBars"));
            labels.configureMenu(m, labels.getString("view"));
            for (Action a : toolBarActions) {
                cbmi = new JCheckBoxMenuItem(a);
                Actions.configureJCheckBoxMenuItem(cbmi, a);
                m2.add(cbmi);
            }
            m.add(m2);
        }

        return m;
    }

    protected JMenu createHelpMenu(View p) {
        ApplicationModel model = getModel();
        ResourceBundleUtil labels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.app.Labels");

        JMenu m;
        JMenuItem mi;

        m = new JMenu();
        labels.configureMenu(m, labels.getString("help"));
        m.add(model.getAction(AboutAction.ID));

        return m;
    }
}
