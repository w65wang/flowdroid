/*
 * @(#)DOMFactory.java  1.0  February 17, 2004
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

package org.jhotdraw.xml;

/**
 * DOMFactory.
 *
 * @author  Werner Randelshofer
 * @version 1.0 February 17, 2004 Create..
 */
public interface DOMFactory {
    /**
     * Returns the element name for the specified object.
     * Note: The element names "string", "int", "float", "long", "double", 
     * "boolean", "enum" and "null"  are reserved and must not be returned by
     * this operation.
     */
    public String getName(DOMStorable o);
    /**
     * Creates an object from the specified element name.
     */
    public Object create(String name);
    
    /**
     * Returns the element tag name for the specified Enum class.
     */
    public String getEnumName(Enum o);
    /**
     * Returns the enum tag name for the specified Enum instance.
     */
    public String getEnumValue(Enum o);
    
    /**
     * Creates an enum from the specified element name.
     */
    public Enum createEnum(String name, String value);
}