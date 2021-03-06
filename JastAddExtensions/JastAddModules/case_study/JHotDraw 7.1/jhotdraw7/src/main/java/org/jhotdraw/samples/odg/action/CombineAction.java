/*
 * @(#)CombinePathsAction.java  1.0  2007-07-28
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

package org.jhotdraw.samples.odg.action;

import org.jhotdraw.draw.*;
import org.jhotdraw.draw.action.*;
import org.jhotdraw.samples.odg.figures.*;
import org.jhotdraw.undo.*;
import org.jhotdraw.util.*;
import java.util.*;
import javax.swing.*;
import javax.swing.undo.*;

/**
 * CombinePathsAction.
 *
 * @author  Werner Randelshofer
 * @version 1.0 2007-07-38 Created.
 */
public class CombineAction extends GroupAction {
    public final static String ID = "selectionCombine";
    
    /** Creates a new instance. */
    public CombineAction(DrawingEditor editor) {
        super(editor, new ODGPathFigure());
        
        labels = ResourceBundleUtil.getLAFBundle(
                "org.jhotdraw.samples.odg.Labels",
                Locale.getDefault()
                );
        labels.configureAction(this, ID);
    }
    
   @Override protected boolean canGroup() {
        boolean canCombine = getView().getSelectionCount() > 1;
        if (canCombine) {
            for (Figure f : getView().getSelectedFigures()) {
                if (!(f instanceof ODGPathFigure)) {
                    canCombine = false;
                    break;
                }
            }
        }
        return canCombine;
    }
    public Collection<Figure> ungroupFigures(DrawingView view, CompositeFigure group) {
        LinkedList<Figure> figures = new LinkedList<Figure>(group.getChildren());
        view.clearSelection();
        group.basicRemoveAllChildren();
        LinkedList<Figure> paths = new LinkedList<Figure>();
        for (Figure f : figures) {
            ODGPathFigure path = new ODGPathFigure();
            path.removeAllChildren();
            for (Map.Entry<AttributeKey,Object> entry : group.getAttributes().entrySet()) {
                path.setAttribute(entry.getKey(), entry.getValue());
            }
            path.add(f);
            view.getDrawing().basicAdd(path);
            paths.add(path);
        }
        view.getDrawing().remove(group);
        view.addToSelection(paths);
        return figures;
    }
    public void groupFigures(DrawingView view, CompositeFigure group, Collection<Figure> figures) {
        Collection<Figure> sorted = view.getDrawing().sort(figures);
        view.getDrawing().basicRemoveAll(figures);
        view.clearSelection();
        view.getDrawing().add(group);
        group.willChange();
      ((ODGPathFigure) group).removeAllChildren();
        for (Map.Entry<AttributeKey,Object> entry : figures.iterator().next().getAttributes().entrySet()) {
            group.setAttribute(entry.getKey(), entry.getValue());
        }
        for (Figure f : sorted) {
            ODGPathFigure path = (ODGPathFigure) f;
            // XXX - We must fire an UndoableEdito for the flattenTransform!
            path.flattenTransform();
            for (Figure child : path.getChildren()) {
                group.basicAdd(child);
            }
        }
        group.changed();
        view.addToSelection(group);
    }
}
