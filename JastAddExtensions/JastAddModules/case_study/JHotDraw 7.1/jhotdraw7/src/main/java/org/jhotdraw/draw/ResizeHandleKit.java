/*
 * @(#)BoxHandleKit.java  1.1  2008-02-28
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

import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import org.jhotdraw.util.ResourceBundleUtil;

/**
 * A set of utility methods to create handles which resize a Figure by
 * using its <code>setBounds</code> method, if the Figure is transformable.
 * 
 * @author Werner Randelshofer
 * @version 1.1 2008-02-28 Only resize a figure, if it is transformable. 
 * <br>1.0 2007-04-14 Created.
 */
public class ResizeHandleKit {

    private final static boolean DEBUG = false;
    private final static Color HANDLE_FILL_COLOR = Color.WHITE; //new Color(0x00a8ff);
    private final static Color HANDLE_STROKE_COLOR = Color.BLACK; //Color.WHITE;

    /** Creates a new instance. */
    public ResizeHandleKit() {
    }

    /**
     * Creates handles for each corner of a
     * figure and adds them to the provided collection.
     */
    static public void addCornerResizeHandles(Figure f, Collection<Handle> handles) {
        handles.add(southEast(f));
        handles.add(southWest(f));
        handles.add(northEast(f));
        handles.add(northWest(f));
    }

    /**
     * Fills the given Vector with handles at each
     * the north, south, east, and west of the figure.
     */
    static public void addEdgeResizeHandles(Figure f, Collection<Handle> handles) {
        handles.add(south(f));
        handles.add(north(f));
        handles.add(east(f));
        handles.add(west(f));
    }

    /**
     * Fills the given Vector with handles at each
     * the north, south, east, and west of the figure.
     */
    static public void addResizeHandles(Figure f, Collection<Handle> handles) {
        handles.add(new BoundsOutlineHandle(f));
        addCornerResizeHandles(f, handles);
        addEdgeResizeHandles(f, handles);
    }

    static public Handle south(Figure owner) {
        return new SouthHandle(owner);
    }

    static public Handle southEast(Figure owner) {
        return new SouthEastHandle(owner);
    }

    static public Handle southWest(Figure owner) {
        return new SouthWestHandle(owner);
    }

    static public Handle north(Figure owner) {
        return new NorthHandle(owner);
    }

    static public Handle northEast(Figure owner) {
        return new NorthEastHandle(owner);
    }

    static public Handle northWest(Figure owner) {
        return new NorthWestHandle(owner);
    }

    static public Handle east(Figure owner) {
        return new EastHandle(owner);
    }

    static public Handle west(Figure owner) {
        return new WestHandle(owner);
    }

    private static class ResizeHandle extends LocatorHandle {

        private int dx,  dy;
        Object geometry;

        ResizeHandle(Figure owner, Locator loc) {
            super(owner, loc);
        }

        public String getToolTipText(Point p) {
            ResourceBundleUtil labels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.draw.Labels");
            return labels.getString("resizeHandle.tip");
        }

        /**
         * Draws this handle.
         * <p>
         * If the figure is transformable, the handle is drawn as a filled rectangle.
         * If the figure is not transformable, the handle is drawn as an unfilled
         * rectangle.
         */
        public void draw(Graphics2D g) {
            drawRectangle(g,
                    getOwner().isTransformable() ? HANDLE_FILL_COLOR : null,
                    HANDLE_STROKE_COLOR);
        }

        public void trackStart(Point anchor, int modifiersEx) {
            geometry = getOwner().getTransformRestoreData();
            Point location = getLocation();
            dx = -anchor.x + location.x;
            dy = -anchor.y + location.y;
        }

        public void trackStep(Point anchor, Point lead, int modifiersEx) {
            if (getOwner().isTransformable()) {
                Point2D.Double p = view.viewToDrawing(new Point(lead.x + dx, lead.y + dy));
                view.getConstrainer().constrainPoint(p);

                if (AttributeKeys.TRANSFORM.get(getOwner()) != null) {
                    try {
                        AttributeKeys.TRANSFORM.get(getOwner()).inverseTransform(p, p);
                    } catch (NoninvertibleTransformException ex) {
                        if (DEBUG) {
                            ex.printStackTrace();
                        }
                    }
                }

                trackStepNormalized(p);
            }
        }

        public void trackEnd(Point anchor, Point lead, int modifiersEx) {
            if (getOwner().isTransformable()) {
                fireUndoableEditHappened(
                        new GeometryEdit(getOwner(), geometry, getOwner().getTransformRestoreData()));
            }
        }

        protected void trackStepNormalized(Point2D.Double p) {
        }

        protected void setBounds(Point2D.Double anchor, Point2D.Double lead) {
            Figure f = getOwner();
            f.willChange();
            f.setBounds(anchor, lead);
            f.changed();
        }
    }

    private static class NorthEastHandle extends ResizeHandle {

        NorthEastHandle(Figure owner) {
            super(owner, RelativeLocator.northEast(true));
        }

        protected void trackStepNormalized(Point2D.Double p) {
            Rectangle2D.Double r = getOwner().getBounds();
            setBounds(
                    new Point2D.Double(r.x, Math.min(r.y + r.height - 1, p.y)),
                    new Point2D.Double(Math.max(r.x, p.x), r.y + r.height));
        }

        public Cursor getCursor() {
            return Cursor.getPredefinedCursor(
                    getOwner().isTransformable() ? Cursor.NE_RESIZE_CURSOR : Cursor.DEFAULT_CURSOR
                    );
        }
    }

    private static class EastHandle extends ResizeHandle {

        EastHandle(Figure owner) {
            super(owner, RelativeLocator.east(true));
        }

        protected void trackStepNormalized(Point2D.Double p) {
            Rectangle2D.Double r = getOwner().getBounds();
            setBounds(
                    new Point2D.Double(r.x, r.y),
                    new Point2D.Double(Math.max(r.x + 1, p.x), r.y + r.height));
        }

        public Cursor getCursor() {
            return Cursor.getPredefinedCursor(
                    getOwner().isTransformable() ? Cursor.E_RESIZE_CURSOR : Cursor.DEFAULT_CURSOR);
        }
    }

    private static class NorthHandle extends ResizeHandle {

        NorthHandle(Figure owner) {
            super(owner, RelativeLocator.north(true));
        }

        protected void trackStepNormalized(Point2D.Double p) {
            Rectangle2D.Double r = getOwner().getBounds();
            setBounds(
                    new Point2D.Double(r.x, Math.min(r.y + r.height - 1, p.y)),
                    new Point2D.Double(r.x + r.width, r.y + r.height));
        }

        public Cursor getCursor() {
            return Cursor.getPredefinedCursor(
                    getOwner().isTransformable() ? Cursor.N_RESIZE_CURSOR : Cursor.DEFAULT_CURSOR
                    );
        }
    }

    private static class NorthWestHandle extends ResizeHandle {

        NorthWestHandle(Figure owner) {
            super(owner, RelativeLocator.northWest(true));
        }

        protected void trackStepNormalized(Point2D.Double p) {
            Rectangle2D.Double r = getOwner().getBounds();
            setBounds(
                    new Point2D.Double(Math.min(r.x + r.width - 1, p.x), Math.min(r.y + r.height - 1, p.y)),
                    new Point2D.Double(r.x + r.width, r.y + r.height));
        }

        public Cursor getCursor() {
            return Cursor.getPredefinedCursor(
                    getOwner().isTransformable() ? Cursor.NW_RESIZE_CURSOR : Cursor.DEFAULT_CURSOR);
        }
    }

    private static class SouthEastHandle extends ResizeHandle {

        SouthEastHandle(Figure owner) {
            super(owner, RelativeLocator.southEast(true));
        }

        protected void trackStepNormalized(Point2D.Double p) {
            Rectangle2D.Double r = getOwner().getBounds();
            setBounds(
                    new Point2D.Double(r.x, r.y),
                    new Point2D.Double(Math.max(r.x + 1, p.x), Math.max(r.y + 1, p.y)));
        }

        public Cursor getCursor() {
            return Cursor.getPredefinedCursor(
                    getOwner().isTransformable() ? Cursor.SE_RESIZE_CURSOR : Cursor.DEFAULT_CURSOR
                    );
        }
    }

    private static class SouthHandle extends ResizeHandle {

        SouthHandle(Figure owner) {
            super(owner, RelativeLocator.south(true));
        }

        protected void trackStepNormalized(Point2D.Double p) {
            Rectangle2D.Double r = getOwner().getBounds();
            setBounds(
                    new Point2D.Double(r.x, r.y),
                    new Point2D.Double(r.x + r.width, Math.max(r.y + 1, p.y)));
        }

        public Cursor getCursor() {
            return Cursor.getPredefinedCursor(
                    getOwner().isTransformable() ? Cursor.S_RESIZE_CURSOR : Cursor.DEFAULT_CURSOR
                    );
        }
    }

    private static class SouthWestHandle extends ResizeHandle {

        SouthWestHandle(Figure owner) {
            super(owner, RelativeLocator.southWest(true));
        }

        protected void trackStepNormalized(Point2D.Double p) {
            Rectangle2D.Double r = getOwner().getBounds();
            setBounds(
                    new Point2D.Double(Math.min(r.x + r.width - 1, p.x), r.y),
                    new Point2D.Double(r.x + r.width, Math.max(r.y + 1, p.y)));
        }

        public Cursor getCursor() {
            return Cursor.getPredefinedCursor(
                    getOwner().isTransformable() ? Cursor.SW_RESIZE_CURSOR : Cursor.DEFAULT_CURSOR
                    );
        }
    }

    private static class WestHandle extends ResizeHandle {

        WestHandle(Figure owner) {
            super(owner, RelativeLocator.west(true));
        }

        protected void trackStepNormalized(Point2D.Double p) {
            Rectangle2D.Double r = getOwner().getBounds();
            setBounds(
                    new Point2D.Double(Math.min(r.x + r.width - 1, p.x), r.y),
                    new Point2D.Double(r.x + r.width, r.y + r.height));
        }

        public Cursor getCursor() {
            return Cursor.getPredefinedCursor(
                    getOwner().isTransformable() ? Cursor.W_RESIZE_CURSOR : Cursor.DEFAULT_CURSOR
                    );
        }
    }
}
