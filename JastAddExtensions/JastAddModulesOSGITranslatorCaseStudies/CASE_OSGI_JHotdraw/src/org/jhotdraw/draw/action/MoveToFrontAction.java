/*
 * @(#)ToFrontAction.java  1.0  24. November 2003
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

package org.jhotdraw.draw.action;

import org.jhotdraw.util.*;
import javax.swing.*;
import java.util.*;
import javax.swing.undo.*;
import org.jhotdraw.draw.*;

/**
 * ToFrontAction.
 *
 * @author  Werner Randelshofer
 * @version 1.0 24. November 2003  Created.
 */
public class MoveToFrontAction extends AbstractSelectedAction {
       private ResourceBundleUtil labels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.draw.Labels", Locale.getDefault());
    
       public static String ID = "moveToFront";
       
    /** Creates a new instance. */
    public MoveToFrontAction(DrawingEditor editor) {
        super(editor);
        labels.configureAction(this, ID);
    }
    
    public void actionPerformed(java.awt.event.ActionEvent e) {
        final DrawingView view = getView();
        final LinkedList<Figure> figures = new LinkedList<Figure>(view.getSelectedFigures());
        bringToFront(view, figures);
        fireUndoableEditHappened(new AbstractUndoableEdit() {
            public String getPresentationName() {
       return labels.getString(ID);
            }
            public void redo() throws CannotRedoException {
                super.redo();
                MoveToFrontAction.bringToFront(view, figures);
            }
            public void undo() throws CannotUndoException {
                super.undo();
                MoveToBackAction.sendToBack(view, figures);
            }
        }
        );
    }
    public static void bringToFront(DrawingView view, Collection<Figure> figures) {
        Drawing drawing = view.getDrawing();
        Iterator i = drawing.sort(figures).iterator();
        while (i.hasNext()) {
            Figure figure = (Figure) i.next();
            drawing.bringToFront(figure);
        }
    }
    
}
