/*
 * @(#)TextAreaFigure.java  2.0.3  2007-05-02
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

package org.jhotdraw.draw;

import org.jhotdraw.util.*;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.text.*;
import java.util.*;
import java.io.*;
import static org.jhotdraw.draw.AttributeKeys.*;
import org.jhotdraw.geom.*;
import org.jhotdraw.xml.DOMInput;
import org.jhotdraw.xml.DOMOutput;
/**
 * A TextAreaFigure contains formatted text.<br>
 * It automatically rearranges the text to fit its allocated display area,
 * breaking the lines at word boundaries whenever possible.<br>
 * The text can contain either LF or CRLF sequences to separate paragraphs,
 * as well as tab characters for table like formatting and alignment.<br>
 * Currently the tabs are distributed at regular intervals as determined by
 * the TabSize property. Tabs align correctly with either fixed
 * or variable fonts.<br>
 * If, when resizing, the vertical size of the display box is not enough to
 * display all the text, TextAreaFigure displays a dashed red line at the
 * bottom of the figure to indicate there is hidden text.<br>
 * TextAreFigure uses all standard attributes for the area Rectangle2D.Double,
 * ie: FillColor, PenColor for the border, FontSize, FontStyle, and FontName,
 * as well as four additional attributes LeftMargin, RightMargin, TopMargin,
 * and TabSize.<br>
 * <p>
 * A DrawingEditor should provide the TextAreaTool to create a TextAreaFigure.
 * <p>
 * FIXME - TextAreaFigure should not draw a rectangle on its own but rather
 * rely on a decorator. We probably need a DecoratorConnector for this and we
 * need a way to specify the inner bounds of the decorator. We also need a way
 * to center the text of the TextAreaFigure verticaly and horizontaly.
 *
 * @author    Eduardo Francos - InContext (original version),
 *            Werner Randelshofer (this derived version)
 * @version 2.0.3 2007-04-05 Made all instance variables protected instead of private.
 * <br>2.0.2 2006-12-11 Implemented more efficient clipping.
 * <br>2.0.1 2006-02-27 Draw UNDERLINE_LOW_ONE_PIXEL instead of UNDERLINE_ON.
 * <br>2.0 2006-01-14 Changed to support double precison coordinates.
 * <br>1.0 5. March 2004  Created.
 */
public class TextAreaFigure extends AbstractAttributedDecoratedFigure implements TextHolderFigure {
    protected Rectangle2D.Double bounds = new Rectangle2D.Double();
    protected boolean editable = true;
    private final static BasicStroke dashes = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, new float[] {4f, 4f}, 0f);
    /**
     * This is a cached value to improve the performance of method isTextOverflow();
     */
    private Boolean isTextOverflow;
    
    /** Creates a new instance. */
    public TextAreaFigure() {
        this(ResourceBundleUtil.
                getLAFBundle("org.jhotdraw.draw.Labels").
                getString("TextFigure.defaultText")
                );
    }
    public TextAreaFigure(String text) {
        setText(text);
    }
    
    // DRAWING
    protected void drawText(Graphics2D g) {
        if (getText() != null || isEditable()) {
            Font font = getFont();
            boolean isUnderlined = FONT_UNDERLINE.get(this);
            Insets2D.Double insets = getInsets();
            Rectangle2D.Double textRect = new Rectangle2D.Double(
                    bounds.x + insets.left,
                    bounds.y + insets.top,
                    bounds.width - insets.left - insets.right,
                    bounds.height - insets.top - insets.bottom
                    );
            float leftMargin = (float) textRect.x;
            float rightMargin = (float) Math.max(leftMargin + 1, textRect.x + textRect.width);
            float verticalPos = (float) textRect.y;
            float maxVerticalPos = (float) (textRect.y + textRect.height);
            if (leftMargin < rightMargin) {
                //float tabWidth = (float) (getTabSize() * g.getFontMetrics(font).charWidth('m'));
                float tabWidth = (float) (getTabSize() * font.getStringBounds("m", getFontRenderContext()).getWidth());
                float[] tabStops = new float[(int) (textRect.width / tabWidth)];
                for (int i=0; i < tabStops.length; i++) {
                    tabStops[i] = (float) (textRect.x + (int) (tabWidth * (i + 1)));
                }
                
                if (getText() != null) {
                    Shape savedClipArea = g.getClip();
                    g.clip(textRect);
                    
                    String[] paragraphs = getText().split("\n");//Strings.split(getText(), '\n');
                    for (int i = 0; i < paragraphs.length; i++) {
                        if (paragraphs[i].length() == 0) paragraphs[i] = " ";
                        AttributedString as = new AttributedString(paragraphs[i]);
                        as.addAttribute(TextAttribute.FONT, font);
                        if (isUnderlined) {
                            as.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
                        }
                        int tabCount = new StringTokenizer(paragraphs[i], "\t").countTokens() - 1;
                        verticalPos = drawParagraph(g, as.getIterator(), verticalPos, maxVerticalPos, leftMargin, rightMargin, tabStops, tabCount);
                        if (verticalPos > maxVerticalPos) {
                            break;
                        }
                    }
                    g.setClip(savedClipArea);
                }
            }
        }
    }
    
    /**
     * Draws of measures a paragraph of text at the specified y location and returns
     * the y position for the next paragraph.
     *
     * @param g Graphics object. This parameter is null, if we want to
     *  measure the size of the paragraph.
     */
    private float drawParagraph(Graphics2D g, AttributedCharacterIterator styledText, float verticalPos, float maxVerticalPos, float leftMargin, float rightMargin, float[] tabStops, int tabCount) {
        // This method is based on the code sample given
        // in the class comment of java.awt.font.LineBreakMeasurer, 
        
        // assume styledText is an AttributedCharacterIterator, and the number
        // of tabs in styledText is tabCount
        
        int[] tabLocations = new int[tabCount+1];
        
        int i = 0;
        for (char c = styledText.first(); c != styledText.DONE; c = styledText.next()) {
            if (c == '\t') {
                tabLocations[i++] = styledText.getIndex();
            }
        }
        tabLocations[tabCount] = styledText.getEndIndex() - 1;
        
        // Now tabLocations has an entry for every tab's offset in
        // the text.  For convenience, the last entry is tabLocations
        // is the offset of the last character in the text.
        
        LineBreakMeasurer measurer = new LineBreakMeasurer(styledText, getFontRenderContext());
        int currentTab = 0;
        
        while (measurer.getPosition() < styledText.getEndIndex() &&
                verticalPos <= maxVerticalPos) {
            
            // Lay out and draw each line.  All segments on a line
            // must be computed before any drawing can occur, since
            // we must know the largest ascent on the line.
            // TextLayouts are computed and stored in a List;
            // their horizontal positions are stored in a parallel
            // List.
            
            // lineContainsText is true after first segment is drawn
            boolean lineContainsText = false;
            boolean lineComplete = false;
            float maxAscent = 0, maxDescent = 0;
            float horizontalPos = leftMargin;
            LinkedList<TextLayout> layouts = new LinkedList<TextLayout>();
            LinkedList<Float> penPositions = new LinkedList<Float>();
            
            while (!lineComplete && verticalPos <= maxVerticalPos) {
                float wrappingWidth = rightMargin - horizontalPos;
                TextLayout layout = null;
                layout =
                        measurer.nextLayout(wrappingWidth,
                        tabLocations[currentTab]+1,
                        lineContainsText);
                
                // layout can be null if lineContainsText is true
                if (layout != null) {
                    layouts.add(layout);
                    penPositions.add(horizontalPos);
                    horizontalPos += layout.getAdvance();
                    maxAscent = Math.max(maxAscent, layout.getAscent());
                    maxDescent = Math.max(maxDescent,
                            layout.getDescent() + layout.getLeading());
                } else {
                    lineComplete = true;
                }
                
                lineContainsText = true;
                
                if (measurer.getPosition() == tabLocations[currentTab]+1) {
                    currentTab++;
                }
                
                if (measurer.getPosition() == styledText.getEndIndex())
                    lineComplete = true;
                else if (tabStops.length == 0 || horizontalPos >= tabStops[tabStops.length-1])
                    lineComplete = true;
                
                if (!lineComplete) {
                    // move to next tab stop
                    int j;
                    for (j=0; horizontalPos >= tabStops[j]; j++) {}
                    horizontalPos = tabStops[j];
                }
            }
            
            verticalPos += maxAscent;
            
            Iterator<TextLayout> layoutEnum = layouts.iterator();
            Iterator<Float> positionEnum = penPositions.iterator();
            
            // now iterate through layouts and draw them
            while (layoutEnum.hasNext()) {
                TextLayout nextLayout = layoutEnum.next();
                float nextPosition = positionEnum.next();
                if (g != null) {
                    nextLayout.draw(g, nextPosition, verticalPos);
                }
            }
            
            verticalPos += maxDescent;
        }
        
        return verticalPos;
    }
    
    
    protected void drawFill(Graphics2D g) {
        g.fill(bounds);
    }
    
    protected void drawStroke(Graphics2D g) {
        g.draw(bounds);
    }
    
    // SHAPE AND BOUNDS
    public void setBounds(Point2D.Double anchor, Point2D.Double lead) {
        bounds.x = Math.min(anchor.x, lead.x);
        bounds.y = Math.min(anchor.y, lead.y);
        bounds.width = Math.max(1, Math.abs(lead.x - anchor.x));
        bounds.height = Math.max(1, Math.abs(lead.y - anchor.y));
    }
    public void transform(AffineTransform tx) {
        Point2D.Double anchor = getStartPoint();
        Point2D.Double lead = getEndPoint();
        setBounds(
                (Point2D.Double) tx.transform(anchor, anchor),
                (Point2D.Double) tx.transform(lead, lead)
                );
    }
    
    
    public boolean figureContains(Point2D.Double p) {
        return bounds.contains(p);
    }
    
    public Rectangle2D.Double getBounds() {
        return (Rectangle2D.Double) bounds.getBounds2D();
    }
    public void restoreTransformTo(Object geometry) {
        Rectangle2D.Double r = (Rectangle2D.Double) geometry;
        bounds.x = r.x;
        bounds.y = r.y;
        bounds.width = r.width;
        bounds.height = r.height;
    }
    
    public Object getTransformRestoreData() {
        return bounds.clone();
    }
    
    // ATTRIBUTES
    /**
     * Gets the text shown by the text figure.
     */
    public String getText() {
        return (String) getAttribute(TEXT);
    }
    /**
     * Returns the insets used to draw text.
     */
    public Insets2D.Double getInsets() {
        double sw = Math.ceil(STROKE_WIDTH.get(this) / 2);
        Insets2D.Double insets = new Insets2D.Double(4,4,4,4);
        return new Insets2D.Double(insets.top+sw,insets.left+sw,insets.bottom+sw,insets.right+sw);
    }
    
    public int getTabSize() {
        return 8;
    }
    
    
    /**
     * Sets the text shown by the text figure.
     */
    public void setText(String newText) {
        TEXT.set(this, newText);
    }
    
    public int getTextColumns() {
        return (getText() == null) ? 4 : Math.max(getText().length(), 4);
    }
    public Font getFont() {
        return AttributeKeys.getFont(this);
    }
    public Color getTextColor() {
        return TEXT_COLOR.get(this);
    }
    
    public Color getFillColor() {
        return FILL_COLOR.get(this);
    }
    
    public void setFontSize(float size) {
        FONT_SIZE.set(this, new Double(size));
    }
    
    public float getFontSize() {
        return FONT_SIZE.get(this).floatValue();
    }
    
    // EDITING
    public boolean isEditable() {
        return editable;
    }
    public void setEditable(boolean b) {
        this.editable = b;
    }
    /**
     * Returns a specialized tool for the given coordinate.
     * <p>Returns null, if no specialized tool is available.
     */
    public Tool getTool(Point2D.Double p) {
        if (isEditable() && contains(p)) {
            TextAreaTool tool = new TextAreaTool(this);
            tool.setForCreationOnly(false);
            return tool;
        }
        return null;
    }
    public TextHolderFigure getLabelFor() {
        return this;
    }
    
    
    // CONNECTING
    // COMPOSITE FIGURES
    // CLONING
    public TextAreaFigure clone() {
        TextAreaFigure that = (TextAreaFigure) super.clone();
        that.bounds = (Rectangle2D.Double) this.bounds.clone();
        return that;
    }
    
    // EVENT HANDLING
    
    public Collection<Handle> createHandles(int detailLevel) {
        LinkedList<Handle> handles = (LinkedList<Handle>) super.createHandles(detailLevel);
        if (detailLevel == 0) {
            handles.add(new FontSizeHandle(this));
                handles.add(new TextOverflowHandle(this));
        }
        return handles;
    }
    
    protected void readBounds(DOMInput in) throws IOException {
        bounds.x = in.getAttribute("x",0d);
        bounds.y = in.getAttribute("y",0d);
        bounds.width = in.getAttribute("w",0d);
        bounds.height = in.getAttribute("h",0d);
    }
    protected void writeBounds(DOMOutput out) throws IOException {
        out.addAttribute("x",bounds.x);
        out.addAttribute("y",bounds.y);
        out.addAttribute("w",bounds.width);
        out.addAttribute("h",bounds.height);
    }
    public void read(DOMInput in) throws IOException {
        readBounds(in);
        readAttributes(in);
    }
    
    public void write(DOMOutput out) throws IOException {
        writeBounds(out);
        writeAttributes(out);
    }
    
    public void invalidate() {
        super.invalidate();
        isTextOverflow = null;
    }
    
    public boolean isTextOverflow() {
        if (isTextOverflow == null) {
            isTextOverflow = false;
            
            if (getText() != null || isEditable()) {
                Font font = getFont();
                boolean isUnderlined = FONT_UNDERLINE.get(this);
                Insets2D.Double insets = getInsets();
                Rectangle2D.Double textRect = new Rectangle2D.Double(
                        bounds.x + insets.left,
                        bounds.y + insets.top,
                        bounds.width - insets.left - insets.right,
                        bounds.height - insets.top - insets.bottom
                        );
                float leftMargin = (float) textRect.x;
                float rightMargin = (float) Math.max(leftMargin + 1, textRect.x + textRect.width);
                float verticalPos = (float) textRect.y;
                float maxVerticalPos = (float) (textRect.y + textRect.height);
                if (leftMargin < rightMargin) {
                    float tabWidth = (float) (getTabSize() * font.getStringBounds("m", getFontRenderContext()).getWidth());
                    float[] tabStops = new float[(int) (textRect.width / tabWidth)];
                    for (int i=0; i < tabStops.length; i++) {
                        tabStops[i] = (float) (textRect.x + (int) (tabWidth * (i + 1)));
                    }
                    
                    if (getText() != null) {
                        String[] paragraphs = getText().split("\n");//Strings.split(getText(), '\n');
                        for (int i = 0; i < paragraphs.length; i++) {
                            if (paragraphs[i].length() == 0) paragraphs[i] = " ";
                            AttributedString as = new AttributedString(paragraphs[i]);
                            as.addAttribute(TextAttribute.FONT, font);
                            if (isUnderlined) {
                                as.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
                            }
                            int tabCount = new StringTokenizer(paragraphs[i], "\t").countTokens() - 1;
                            verticalPos = drawParagraph(null, as.getIterator(), verticalPos, maxVerticalPos, leftMargin, rightMargin, tabStops, tabCount);
                            if (verticalPos > maxVerticalPos) {
                                break;
                            }
                        }
                    }
                    isTextOverflow = (leftMargin >= rightMargin || verticalPos > textRect.y + textRect.height);
                }
            }
        }
        return isTextOverflow;
    }
}