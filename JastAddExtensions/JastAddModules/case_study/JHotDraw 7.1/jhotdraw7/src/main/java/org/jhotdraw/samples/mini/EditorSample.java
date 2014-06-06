/*
 * @(#)EditorSample.java   1.0  March 8, 2007
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
package org.jhotdraw.samples.mini;

import java.awt.geom.*;
import javax.swing.*;
import org.jhotdraw.draw.*;
import org.jhotdraw.geom.*;

/**
 * Example showing how to create an editor that can edit multiple
 * drawing views. For simplicity, the editor can create rectangle figures only.
 *
 * @author Werner Randelshofer
 * @version 1.0 March 8, 2007 Created.
 */
public class EditorSample {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                
                // Create a simple drawing consisting of three
                // text areas and an elbow connection.
                TextAreaFigure ta = new TextAreaFigure();
                ta.setBounds(new Point2D.Double(10,10),new Point2D.Double(100,100));
                TextAreaFigure tb = new TextAreaFigure();
                tb.setBounds(new Point2D.Double(220,120),new Point2D.Double(310,210));
                TextAreaFigure tc = new TextAreaFigure();
                tc.setBounds(new Point2D.Double(220,10),new Point2D.Double(310,100));
                ConnectionFigure cf = new LineConnectionFigure();
                cf.setLiner(new ElbowLiner());
                cf.setStartConnector(ta.findConnector(Geom.center(ta.getBounds()), cf));
                cf.setEndConnector(tb.findConnector(Geom.center(tb.getBounds()), cf));
                Drawing drawing = new DefaultDrawing();
                drawing.add(ta);
                drawing.add(tb);
                drawing.add(tc);
                drawing.add(cf);
                
                // Show the drawing
                JFrame f = new JFrame("My Drawing");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setSize(400,300);
                
                // Set up the drawing view
                DrawingView view = new DefaultDrawingView();
                view.setDrawing(drawing);
                f.getContentPane().add(view.getComponent());
                
                // Set up the drawing editor
                DrawingEditor editor = new DefaultDrawingEditor();
                editor.add(view);
                editor.setTool(new DelegationSelectionTool());
                
                f.show();
            }
        });
    }
}
