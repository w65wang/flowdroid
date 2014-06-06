/*
 * @(#)EditorColorChooserAction.java  2.0  2006-06-07
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

import javax.swing.*;
import java.util.*;
import java.awt.*;
import java.beans.*;
import javax.swing.*;
import org.jhotdraw.draw.*;
import org.jhotdraw.undo.CompositeEdit;
/**
 * EditorColorChooserAction.
 * <p>
 * The behavior for choosing the initial color of the JColorChooser matches with
 * {@link EditorColorIcon }.
 *
 * @author Werner Randelshofer
 * @version 2.0 2006-06-07 Reworked.
 * <br>1.0 2004-03-02  Created.
 */
public class EditorColorChooserAction extends AbstractSelectedAction {
    protected AttributeKey<Color> key;
    private static JColorChooser colorChooser;
    private HashMap<AttributeKey,Object> fixedAttributes;
    
    /** Creates a new instance. */
    public EditorColorChooserAction(DrawingEditor editor, AttributeKey<Color> key) {
        this(editor, key, null, null);
    }
    /** Creates a new instance. */
    public EditorColorChooserAction(DrawingEditor editor, AttributeKey<Color> key, Icon icon) {
        this(editor, key, null, icon);
    }
    /** Creates a new instance. */
    public EditorColorChooserAction(DrawingEditor editor, AttributeKey<Color> key, String name) {
        this(editor, key, name, null);
    }
    public EditorColorChooserAction(DrawingEditor editor, final AttributeKey<Color> key, String name, Icon icon) {
        this(editor, key, name, icon, new HashMap<AttributeKey,Object>());
    }
    public EditorColorChooserAction(DrawingEditor editor, final AttributeKey<Color> key, String name, Icon icon,
            Map<AttributeKey,Object> fixedAttributes) {
        super(editor);
        this.key = key;
        putValue(AbstractAction.NAME, name);
        //putValue(AbstractAction.MNEMONIC_KEY, new Integer('V'));
        putValue(AbstractAction.SMALL_ICON, icon);
        setEnabled(true);
    }
    
    public void actionPerformed(java.awt.event.ActionEvent e) {
        if (colorChooser == null) {
            colorChooser = new JColorChooser();
        }
        Color initialColor = getInitialColor();
        Color chosenColor = colorChooser.showDialog((Component) e.getSource(), labels.getString("drawColor"), initialColor);
        if (chosenColor != null) {
            changeAttribute(chosenColor);
        }
    }
    
    public void changeAttribute(Color value) {
        CompositeEdit edit = new CompositeEdit("attributes");
        fireUndoableEditHappened(edit);
        Drawing drawing = getDrawing();
        Iterator i = getView().getSelectedFigures().iterator();
        while (i.hasNext()) {
            Figure figure = (Figure) i.next();
            figure.willChange();
            key.basicSet(figure, value);
            for (Map.Entry<AttributeKey,Object> entry : fixedAttributes.entrySet()) {
                entry.getKey().basicSet(figure, entry.getValue());
            }
            figure.changed();
        }
        getEditor().setDefaultAttribute(key, value);
        fireUndoableEditHappened(edit);
    }
    public void selectionChanged(FigureSelectionEvent evt) {
        //setEnabled(getView().getSelectionCount() > 0);
    }
    
    protected Color getInitialColor() {
        Color initialColor = (Color) getEditor().getDefaultAttribute(key);
        if (initialColor == null) {
            initialColor = Color.red;
        }
        return initialColor;
    }
}
