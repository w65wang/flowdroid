/*
 * @(#)DelegationSelectionTool.java  2.1  2007-04-14
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

package org.jhotdraw.draw;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import org.jhotdraw.app.action.Actions;
/**
 * A SelectionTool, which recognizes double clicks and popup menu triggers.
 * If a double click or popup trigger is encountered a hook method is called,
 * which handles the event. This methods can be overriden in subclasse to
 * provide customized behaviour.
 * <p>
 * By default, this Tool delegates mouse events to a specific Tool if
 * the figure which has been double clicked, provides a specialized tool.
 *
 * @author Werner Randelshofer
 * @version 2.1 2007-04-14 Added support for multi-clicks.
 * <br>2.0 2006-01-18 Changed to support double precision coordinates.
 * Popup timer added. Support for radio button menu items added.
 * <br>1.0 2003-12-01 Derived from JHotDraw 5.4b1.
 */
public class DelegationSelectionTool extends SelectionTool {
    /**
     * Set this to true to turn on debugging output on System.out.
     */
    private final static boolean DEBUG = false;
    
    /**
     * A set of actions which is applied to the drawing.
     */
    private Collection<Action> drawingActions;
    /**
     * A set of actions which is applied to a selection of figures.
     */
    private Collection<Action> selectionActions;
    
    /**
     * We use this timer, to show a popup menu, when the user presses the
     * mouse key for a second without moving the mouse.
     */
    private javax.swing.Timer popupTimer;
    
    /**
     * When the popup menu is visible, we do not track mouse movements.
     */
    private JPopupMenu popupMenu;
    
    /**
     * We store the last mouse click here, to support multi-click behavior,
     * that is, a behavior that is invoked, when the user clicks multiple on
     * the same spot, but in a longer interval than needed for a double click.
     */
    private MouseEvent lastClickEvent;
    
    /** Creates a new instance. */
    public DelegationSelectionTool() {
        this(new LinkedList<Action>(), new LinkedList<Action>());
    }
    /** Creates a new instance. */
    public DelegationSelectionTool(Collection<Action> drawingActions, Collection<Action> selectionActions) {
        this.drawingActions = drawingActions;
        this.selectionActions = selectionActions;
    }
    
    public void setDrawingActions(Collection<Action> drawingActions) {
        this.drawingActions = drawingActions;
    }
    public void setFigureActions(Collection<Action> selectionActions) {
        this.selectionActions = selectionActions;
    }
    
    /**
     * MouseListener method for mousePressed events. If the popup trigger has
     * been activated, then the appropriate hook method is called.
     */
    public void mousePressed(final MouseEvent evt) {
        if (popupTimer != null) {
            popupTimer.stop();
            popupTimer = null;
        }
        // XXX - When we want to support multiple views, we have to
        //       implement this:
        //setView((DrawingView)e.getSource());
        
        if (evt.isPopupTrigger()) {
            getView().requestFocus();
            handlePopupMenu(evt);
        } else {
            super.mousePressed(evt);
            popupTimer = new javax.swing.Timer(1000, new ActionListener() {
                public void actionPerformed(ActionEvent aevt) {
                    handlePopupMenu(evt);
                    popupTimer = null;
                }
            });
            popupTimer.setRepeats(false);
            popupTimer.start();
        }
    }
    /**
     * MouseListener method for mouseReleased events. If the popup trigger has
     * been activated, then the appropriate hook method is called.
     */
    public void mouseReleased(MouseEvent evt) {
        if (popupTimer != null) {
            popupTimer.stop();
            popupTimer = null;
        }
        
        super.mouseReleased(evt);
        
        if (evt.isPopupTrigger()) {
            handlePopupMenu(evt);
        }
    }
    public void mouseDragged(MouseEvent evt) {
        if (popupTimer != null) {
            popupTimer.stop();
            popupTimer = null;
        }
        
        if (popupMenu == null || ! popupMenu.isVisible()) {
            super.mouseDragged(evt);
        }
    }
    public void mouseClicked(MouseEvent evt) {
        if (DEBUG) System.out.println("DelegationSelectionTool.mouseClicked "+evt);
        super.mouseClicked(evt);
        if (! evt.isConsumed()) {
            if (evt.getClickCount() == 2) {
                handleDoubleClick(evt);
            } else if (evt.getClickCount() == 1 &&
                     evt.getModifiersEx() == 0 &&
                    lastClickEvent != null &&
                    lastClickEvent.getClickCount() == 1 &&
                    lastClickEvent.getModifiersEx() == 0 &&
                    lastClickEvent.getX() == evt.getX() &&
                    lastClickEvent.getY() == evt.getY()
                    ) {
                handleMultiClick(evt);
            }
        }
        lastClickEvent = evt;
    }
    /**
     * Hook method which can be overriden by subclasses to provide
     * specialised behaviour in the event of a popup trigger.
     */
    protected void handlePopupMenu(MouseEvent evt) {
        Point p =  new Point(evt.getX(), evt.getY());
        Figure figure = getView().findFigure(p);
        if (figure != null || drawingActions.size() > 0) {
            showPopupMenu(figure, p, evt.getComponent());
        } else {
            popupMenu = null;
        }
    }
    
    protected void showPopupMenu(Figure figure, Point p, Component c) {
        if (DEBUG) System.out.println("DelegationSelectionTool.showPopupMenu "+figure);
        JPopupMenu menu = new JPopupMenu();
        popupMenu = menu;
        JMenu submenu = null;
        String submenuName = null;
        LinkedList<Action> popupActions = new LinkedList<Action>();
        if (figure != null) {
            LinkedList<Action> figureActions = new LinkedList<Action>(
                    figure.getActions(viewToDrawing(p))
                    );
            if (popupActions.size() != 0 && figureActions.size() != 0) {
                popupActions.add(null);
            }
            popupActions.addAll(figureActions);
            if (popupActions.size() != 0 && selectionActions.size() != 0) {
                popupActions.add(null);
            }
            popupActions.addAll(selectionActions);
        }
        if (popupActions.size() != 0 && drawingActions.size() != 0) {
            popupActions.add(null);
        }
        popupActions.addAll(drawingActions);
        
        HashMap<Object,ButtonGroup> buttonGroups = new HashMap<Object,ButtonGroup>();
        for (Action a : popupActions) {
            if (a != null && a.getValue(Actions.SUBMENU_KEY) != null) {
                if (submenuName == null || ! submenuName.equals(a.getValue(Actions.SUBMENU_KEY))) {
                    submenuName = (String) a.getValue(Actions.SUBMENU_KEY);
                    submenu = new JMenu(submenuName);
                    menu.add(submenu);
                }
            } else {
                submenuName = null;
                submenu = null;
            }
            if (a == null) {
                if (submenu != null) submenu.addSeparator();
                else menu.addSeparator();
            } else {
                AbstractButton button;
                
                if (a.getValue(Actions.BUTTON_GROUP_KEY) != null) {
                    ButtonGroup bg = buttonGroups.get(a.getValue(Actions.BUTTON_GROUP_KEY));
                    if (bg == null) {
                        bg = new ButtonGroup();
                        buttonGroups.put(a.getValue(Actions.BUTTON_GROUP_KEY), bg);
                    }
                    button = new JRadioButtonMenuItem(a);
                    bg.add(button);
                    button.setSelected(a.getValue(Actions.SELECTED_KEY) == Boolean.TRUE);
                } else if (a.getValue(Actions.SELECTED_KEY) != null) {
                    button = new JCheckBoxMenuItem(a);
                    button.setSelected(a.getValue(Actions.SELECTED_KEY) == Boolean.TRUE);
                } else {
                    button = new JMenuItem(a);
                }
                
                if (submenu != null) submenu.add(button);
                else menu.add(button);
            }
        }
        menu.show(c, p.x, p.y);
    }
    
    /**
     * Hook method which can be overriden by subclasses to provide
     * specialised behaviour in the event of a double click.
     */
    protected void handleDoubleClick(MouseEvent evt) {
        if (DEBUG) System.out.println("DelegationSelectionTool.handleDoubleClick "+evt);
        DrawingView v = getView();
        Point pos = new Point(evt.getX(), evt.getY());
        Handle handle = v.findHandle(pos);
        if (handle != null) {
        if (DEBUG) System.out.println("DelegationSelectionTool.handleDoubleClick by handle");
            handle.trackDoubleClick(pos, evt.getModifiersEx());
        } else {
            Point2D.Double p = viewToDrawing(pos);
            Figure outerFigure = getView().findFigure(pos);
            Figure figure = outerFigure;
            if (figure != null) {
        if (DEBUG) System.out.println("DelegationSelectionTool.handleDoubleClick by figure");
                Tool figureTool = figure.getTool(p);
                if (figureTool == null) {
                    figure = getDrawing().findFigureInside(p);
                    if (figure != null) {
                        figureTool = figure.getTool(p);
                    }
                }
                if (figureTool != null) {
                    setTracker(figureTool);
                    figureTool.mousePressed(evt);
                } else {
                    if (outerFigure.handleMouseClick(p, evt, getView())) {
                        v.clearSelection();
                        v.addToSelection(outerFigure);
                    } else {
                        v.clearSelection();
                        v.addToSelection(outerFigure);
                        v.setHandleDetailLevel(v.getHandleDetailLevel() + 1);
                    }
                }
            }
        }
        evt.consume();
    }
    
    /**
     * Hook method which can be overriden by subclasses to provide
     * specialised behaviour in the event of a multi-click.
     */
    protected void handleMultiClick(MouseEvent evt) {
        if (DEBUG) System.out.println("DelegationSelectionTool.handleMultiClick "+evt);
        DrawingView v = getView();
        Point pos = new Point(evt.getX(), evt.getY());
        Handle handle = v.findHandle(pos);
        if (handle == null) {
            v.setHandleDetailLevel(v.getHandleDetailLevel() + 1);
        }
    }
}