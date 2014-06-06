/*
 * @(#)SVGDrawingPanel.java  1.0.1  2007-12-17
 *
 * Copyright (c) 2004-2007 by the original authors of JHotDraw
 * and all its contributors.
 * All rights reserved.
 *
 * The copyright of this software is owned by the authors and  
 * contributors of the JHotDraw project ("the copyright holders").  
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * the copyright holders. For details see accompanying license terms. 
 */
package org.jhotdraw.samples.svg;

import org.jhotdraw.gui.JPopupButton;

//NEIL: forced to do this due to a split package caused by SVGAttributeKeys
import jhotdraw::org.jhotdraw.samples.svg.*;

import org.jhotdraw.samples.svg.action.*;
import org.jhotdraw.samples.svg.figures.*;
import org.jhotdraw.undo.*;
import org.jhotdraw.util.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import org.jhotdraw.app.action.*;
import org.jhotdraw.draw.*;
import org.jhotdraw.draw.action.*;

/**
 * SVGDrawingPanel.
 * 
 * @author Werner Randelshofer
 * @version 1.0.1 2007-12-17 Cleaned code up. 
 * <br>1.0 11. March 2004  Created.
 */
public class SVGDrawingPanel extends JPanel {

    private UndoRedoManager undoManager;
    private Drawing drawing;
    private DrawingEditor editor;

    /** Creates new instance. */
    public SVGDrawingPanel() {
        ResourceBundleUtil labels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.draw.Labels");
        initComponents();
        undoManager = new UndoRedoManager();
        editor = new DefaultDrawingEditor();
        editor.add(view);

        // To improve performance while scrolling, we paint via
        // a backing store.
        scrollPane.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
        
        addCreationButtonsTo(creationToolbar, editor);
        ButtonFactory.addAttributesButtonsTo(attributesToolbar, editor);

        JPopupButton pb = new JPopupButton();
        pb.setItemFont(UIManager.getFont("MenuItem.font"));
        labels.configureToolBarButton(pb, "actions");
        pb.add(new DuplicateAction());
        pb.addSeparator();
        pb.add(new GroupAction(editor));
        pb.add(new UngroupAction(editor));
        pb.addSeparator();
        pb.add(new MoveToFrontAction(editor));
        pb.add(new MoveToBackAction(editor));
        pb.addSeparator();
        pb.add(new CutAction());
        pb.add(new CopyAction());
        pb.add(new PasteAction());
        pb.add(new SelectAllAction());
        pb.add(new SelectSameAction(editor));
        pb.addSeparator();
        pb.add(undoManager.getUndoAction());
        pb.add(undoManager.getRedoAction());
        JMenu m = new JMenu(labels.getString("zoom"));
        JRadioButtonMenuItem rbmi;
        ButtonGroup group = new ButtonGroup();
        for (double factor : new double[]{0.1, 0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2, 3, 4}) {
            m.add(rbmi = new JRadioButtonMenuItem(new ZoomAction(editor, factor, null)));
            group.add(rbmi);
            if (factor == 1.0) {
                rbmi.setSelected(true);
            }
        }
        pb.add(m);
        pb.setFocusable(false);
        creationToolbar.addSeparator();
        creationToolbar.add(pb);

        DefaultDrawing drawing = new DefaultDrawing();
        view.setDrawing(drawing);
        drawing.addUndoableEditListener(undoManager);

    }

    public void setDrawing(Drawing d) {
        undoManager.discardAllEdits();
        view.getDrawing().removeUndoableEditListener(undoManager);
        view.setDrawing(d);
        d.addUndoableEditListener(undoManager);
    }

    public Drawing getDrawing() {
        return view.getDrawing();
    }

    public DrawingView getView() {
        return view;
    }

    public DrawingEditor getEditor() {
        return editor;
    }

    public static Collection<Action> createSelectionActions(DrawingEditor editor) {
        LinkedList<Action> a = new LinkedList<Action>();
        a.add(new DuplicateAction());

        a.add(null); // separator
        a.add(new GroupAction(editor, new SVGGroupFigure()));
        a.add(new UngroupAction(editor, new SVGGroupFigure()));
        a.add(new CombineAction(editor));
        a.add(new SplitAction(editor));

        a.add(null); // separator
        a.add(new MoveToFrontAction(editor));
        a.add(new MoveToBackAction(editor));

        return a;
    }

    private void addCreationButtonsTo(JToolBar tb, final DrawingEditor editor) {
        // AttributeKeys for the entitie sets
        HashMap<AttributeKey, Object> attributes;

        ResourceBundleUtil labels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.samples.svg.Labels");
        ResourceBundleUtil drawLabels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.draw.Labels");

        ButtonFactory.addSelectionToolTo(tb, editor,
                ButtonFactory.createDrawingActions(editor),
                createSelectionActions(editor));
        tb.addSeparator();

        attributes = new HashMap<AttributeKey, Object>();
        attributes.put(AttributeKeys.FILL_COLOR, Color.white);
        attributes.put(AttributeKeys.STROKE_COLOR, Color.black);
        ButtonFactory.addToolTo(tb, editor, new CreationTool(new SVGRectFigure(), attributes), "createRectangle", drawLabels);
        ButtonFactory.addToolTo(tb, editor, new CreationTool(new SVGEllipseFigure(), attributes), "createEllipse", drawLabels);
        ButtonFactory.addToolTo(tb, editor, new PathTool(new SVGPathFigure(), new SVGBezierFigure(true), attributes), "createPolygon", drawLabels);
        attributes = new HashMap<AttributeKey, Object>();
        attributes.put(AttributeKeys.FILL_COLOR, null);
        attributes.put(AttributeKeys.STROKE_COLOR, Color.black);
        ButtonFactory.addToolTo(tb, editor, new CreationTool(new SVGPathFigure(), attributes), "createLine", drawLabels);
        ButtonFactory.addToolTo(tb, editor, new PathTool(new SVGPathFigure(), new SVGBezierFigure(false), attributes), "createScribble", drawLabels);
        attributes = new HashMap<AttributeKey, Object>();
        attributes.put(AttributeKeys.FILL_COLOR, Color.black);
        attributes.put(AttributeKeys.STROKE_COLOR, null);
        ButtonFactory.addToolTo(tb, editor, new CreationTool(new SVGTextFigure(), attributes), "createText", drawLabels);
        TextAreaTool tat = new TextAreaTool(new SVGTextAreaFigure(), attributes);
        tat.setRubberbandColor(Color.BLACK);
        ButtonFactory.addToolTo(tb, editor, tat, "createTextArea", drawLabels);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        toolButtonGroup = new javax.swing.ButtonGroup();
        scrollPane = new javax.swing.JScrollPane();
        view = new org.jhotdraw.draw.DefaultDrawingView();
        jPanel1 = new javax.swing.JPanel();
        creationToolbar = new javax.swing.JToolBar();
        attributesToolbar = new javax.swing.JToolBar();

        setLayout(new java.awt.BorderLayout());

        scrollPane.setViewportView(view);

        add(scrollPane, java.awt.BorderLayout.CENTER);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        creationToolbar.setFloatable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(creationToolbar, gridBagConstraints);

        attributesToolbar.setFloatable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(attributesToolbar, gridBagConstraints);

        add(jPanel1, java.awt.BorderLayout.SOUTH);

    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToolBar attributesToolbar;
    private javax.swing.JToolBar creationToolbar;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.ButtonGroup toolButtonGroup;
    private org.jhotdraw.draw.DefaultDrawingView view;
    // End of variables declaration//GEN-END:variables
}
