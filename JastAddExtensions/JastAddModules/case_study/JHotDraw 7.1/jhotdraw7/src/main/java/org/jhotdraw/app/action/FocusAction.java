/*
 * @(#)FocusAction.java  2.0  2006-05-05
 *
 * Copyright (c) 1996-2006 by the original authors of JHotDraw
 * and all its contributors.
 * All rights reserved.
 *
 * The copyright of this software is owned by the authors and  
 * contributors of the JHotDraw project ("the copyright holders").  
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * the copyright holders. For details see accompanying license terms. 
 */

package org.jhotdraw.app.action;

import org.jhotdraw.util.*;

import java.beans.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import org.jhotdraw.app.View;
/**
 * Requests focus for a Frame.
 *
 * @author  Werner Randelshofer
 * @version 2.0 2006-05-05 Reworked.
 * <br>1.0  2005-06-10 Created.
 */
public class FocusAction extends AbstractAction {
    public final static String ID = "focus";
    private View view;
    
    /** Creates a new instance. */
    public FocusAction(View view) {
        this.view = view;
        ResourceBundleUtil labels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.app.Labels");
        labels.configureAction(this, ID);
        //setEnabled(false);
        setEnabled(view != null);
        
        view.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                ResourceBundleUtil labels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.app.Labels");
                String name = evt.getPropertyName();
                if (name.equals(View.FILE_PROPERTY)) {
                    putValue(Action.NAME,
                            (evt.getNewValue() == null) ?
                                labels.getString("unnamedFile") :
                                ((File) evt.getNewValue()).getName()
                                );
                }
            }
        });
    }
    
    public Object getValue(String key) {
        if (key == Action.NAME && view != null) {
            return getTitle();
        } else {
            return super.getValue(key);
        }
    }
    
    private String getTitle() {
        ResourceBundleUtil labels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.app.Labels");
        String title = labels.getString("unnamedFile");
        if (view != null) {
            File file = view.getFile();
            if (file == null) {
                title = labels.getString("unnamedFile");
            } else {
                title = file.getName();
            }
            if (view.hasUnsavedChanges()) {
                title += "*";
            }
            title = (labels.getFormatted("internalFrameTitle", title, view.getApplication().getName(), view.getMultipleOpenId()));
        }
        return title;
        
    }
    private JFrame getFrame() {
        return (JFrame) SwingUtilities.getWindowAncestor(
                view.getComponent()
                );
    }
    private Component getRootPaneContainer() {
        return SwingUtilities.getRootPane(
                view.getComponent()
                ).getParent();
    }
    
    public void actionPerformed(ActionEvent evt) {
        /*
        JFrame frame = getFrame();
        if (frame != null) {
            frame.setExtendedState(frame.getExtendedState() & ~Frame.ICONIFIED);
            frame.toFront();
            frame.requestFocus();
            JRootPane rp = SwingUtilities.getRootPane(view.getComponent());
            if (rp != null && (rp.getParent() instanceof JInternalFrame)) {
                ((JInternalFrame) rp.getParent()).toFront();
            }
            view.getComponent().requestFocus();
        } else {
            Toolkit.getDefaultToolkit().beep();
        }*/
        Component rpContainer = getRootPaneContainer();
        if (rpContainer instanceof Frame) {
            Frame frame = (Frame) rpContainer;
            frame.setExtendedState(frame.getExtendedState() & ~Frame.ICONIFIED);
            frame.toFront();
        } else if (rpContainer instanceof JInternalFrame) {
            JInternalFrame frame = (JInternalFrame) rpContainer;
            frame.toFront();
            try {
                frame.setSelected(true);
            } catch (PropertyVetoException e) {
                // Don't care.
            }
        }
        view.getComponent().requestFocusInWindow();
    }
}
