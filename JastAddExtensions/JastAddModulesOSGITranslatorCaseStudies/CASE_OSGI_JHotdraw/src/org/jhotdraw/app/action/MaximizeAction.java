/*
 * @(#)MaximizeAction.java  2.0  2006-05-05
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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import org.jhotdraw.app.Application;
/**
 * Maximizes the Frame of the current view.
 * 
 * @author Werner Randelshofer
 * @version 2.0 2005-05-05 Reworked.
 * <br>1.0  2005-06-10 Created.
 */
public class MaximizeAction extends AbstractViewAction {
    public final static String ID = "maximize";
    
    /** Creates a new instance. */
    public MaximizeAction(Application app) {
        super(app);
        ResourceBundleUtil labels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.app.Labels");
        labels.configureAction(this, ID);
    }
    
    private JFrame getFrame() {
        return (JFrame) SwingUtilities.getWindowAncestor(
                getActiveView().getComponent()
                );
    }
    
    public void actionPerformed(ActionEvent evt) {
        JFrame frame = getFrame();
        if (frame != null) {
            frame.setExtendedState(frame.getExtendedState() ^ Frame.MAXIMIZED_BOTH);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }
}
