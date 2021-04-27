
package org.bonitasoft.custompage.bookmobile.data;

import java.sql.ResultSetMetaData;

/**
 * Page does not accept all the different type of country, so a transformation can be necessary
 * @author Firstname Lastname
 *
 */
public interface DataTransformerInt {
    
    public void setMetaData(ResultSetMetaData resultSetMetaData );
    
    /**
     * Transform the Base object to a Data object
     * @param columns
     * @param Data
     * @return
     */
    public Object BaseToPage( String columns, Object data);
}
