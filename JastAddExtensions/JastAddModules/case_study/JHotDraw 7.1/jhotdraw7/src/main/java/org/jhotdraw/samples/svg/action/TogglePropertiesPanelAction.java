/*
 * @(#)TogglePropertiesPanelAction.java  1.0  22. April 2007
 *
 * Copyright (c) 2007 by the original authors of JHotDraw
 * and all its contributors.
 * All rights reserved.
 *
 * The copyright of this software is owned by the authors and  
 * contributors of the JHotDraw project ("the copyright holders").  
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * the copyright holders. For details see accompanying license terms. 
 */

package org.jhotdraw.samples.svg.action;

import java.awt.event.*;
import javax.swing.*;
import org.jhotdraw.app.*;
import org.jhotdraw.app.action.*;
import org.jhotdraw.samples.svg.*;

//NEIL: forced to do this due to a split package caused by SVGAttributeKeys
import jhotdraw::org.jhotdraw.samples.svg.*;

import org.jhotdraw.util.*;

/**
 * TogglePropertiesPanelAction.
 * 
 * @author Werner Randelshofer
 * @version 1.0 22. April 2007 Created.
 */
public class TogglePropertiesPanelAction extends AbstractViewAction {
    
    /** Creates a new instance. */
    public TogglePropertiesPanelAction(Application app) {
        super(app);
        setPropertyName("propertiesPanelVisible");
        ResourceBundleUtil labels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.samples.svg.Labels");
        putValue(AbstractAction.NAME, labels.getString("propertiesPanel"));
    }
    
    /**
     * This method is invoked, when the property changed and when
     * the view changed.
     */
    protected void updateView() {
        putValue(Actions.SELECTED_KEY,
                getActiveView() != null &&
                ! getActiveView().isPropertiesPanelVisible()
                );
    }
    
    
    public SVGView getActiveView() {
        return (SVGView) super.getActiveView();
    }
    
    public void actionPerformed(ActionEvent e) {
        getActiveView().setPropertiesPanelVisible(
                ! getActiveView().isPropertiesPanelVisible()
                );
    }
    
}
