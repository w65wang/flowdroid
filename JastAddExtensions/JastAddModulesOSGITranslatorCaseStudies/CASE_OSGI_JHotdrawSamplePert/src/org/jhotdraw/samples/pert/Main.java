/*
 * @(#)Main.java  1.0  June 10, 2006
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

package org.jhotdraw.samples.pert;

import org.jhotdraw.app.Application;
import org.jhotdraw.app.DefaultApplicationModel;
import org.jhotdraw.app.DefaultOSXApplication;
import org.jhotdraw.app.DefaultSDIApplication;
/**
 * Main.
 *
 * @author Werner Randelshofer.
 * @version 1.0 June 10, 2006 Created.
 */
public class Main {
    
    /** Creates a new instance. */
    public static void main(String[] args) {
        Application app;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.startsWith("mac")) {
            app = new DefaultOSXApplication();
        } else if (os.startsWith("win")) {
          //  app = new DefaultMDIApplication();
            app = new DefaultSDIApplication();
        } else {
            app = new DefaultSDIApplication();
        }
        
        
        DefaultApplicationModel model = new PertApplicationModel();
        model.setName("JHotDraw Pert");
        model.setVersion(Main.class.getPackage().getImplementationVersion());
        model.setCopyright("Copyright 2006-2008 (c) by the authors of JHotDraw\n" +
                "This software is licensed under LGPL or Creative Commons 3.0 BY");
        //NEIL: Change to work around reflection
        model.setViewClass(new org.jhotdraw.samples.pert.PertView().getClass());
        app.setModel(model);
        app.launch(args);
    }
    
}
