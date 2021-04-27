package org.bonitasoft.custompage.bookmobile.tool;


public class CastData {
    
    public static Boolean getBoolean(Object value, Boolean defaultValue) {
        if (value==null)
            return defaultValue;
        if (value instanceof Boolean)
            return (Boolean) value;
        try {
            return Boolean.valueOf( value.toString());
        } catch(Exception e)
        {
            return defaultValue;
        }
    }

}
