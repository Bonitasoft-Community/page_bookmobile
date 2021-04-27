package org.bonitasoft.custompage.bookmobile.data;

import java.io.Reader;
import java.sql.Clob;
import java.sql.ResultSetMetaData;
import java.util.logging.Logger;

public class DataTransformerJson implements DataTransformerInt{

    static Logger logger = Logger.getLogger( DataTransformerJson.class.getName());
    public static final String LOGGER_LABEL = "DataTransformerJson: ";

    ResultSetMetaData resultSetMetaData;
    @Override
    public void setMetaData(ResultSetMetaData resultSetMetaData) {
        this.resultSetMetaData = resultSetMetaData;
        
    }

    /**
     * A Json format is required, so transform the data accordinately
     */
    @Override
    public Object BaseToPage(String columns, Object data) {
        if (data instanceof Clob) {
            Clob cData = (Clob) data; 
            try {
                Reader r = cData.getCharacterStream();
                StringBuffer buffer = new StringBuffer();
                int ch;
                while ((ch = r.read())!=-1) {
                   buffer.append(""+(char)ch);
                }
                return buffer.toString();
            } catch( Exception e) {
                logger.severe(LOGGER_LABEL+"Can't convert column["+columns+"] "+e.toString());
            }
        }
        return data;

    }

}
