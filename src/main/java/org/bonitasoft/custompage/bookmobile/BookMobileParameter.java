package org.bonitasoft.custompage.bookmobile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.custompage.bookmobile.tool.CastData;
import org.bonitasoft.engine.session.APISession;
import org.json.simple.JSONValue;

public class BookMobileParameter {
    public static final String CST_MODEL_ALLOW_MODIFY_STRUCTURE = "allowModifyStructure";
    public static final String CST_MODEL_PERSISTENCEIDNAME = "colPersistenceIdName";
    public static final String CST_MODEL_TABLENAME = "tableName";
    public static final String CST_MODEL_DATASOURCENAME = "datasourceName";
    public static final String CST_MODEL_DESCRIPTION = "description";
    public static final String CST_MODEL_NAME = "name";
    public static final String CST_MODEL_TYPE = "type";
    public static final String CST_MODEL_ID = "id";
    public static final String CST_MODEL_PROFILENAMEREAD= "profileNameRead";
    public static final String CST_MODEL_PROFILENAMEWRITE= "profileNameWrite";

    public static final String CST_DATA = "data";
    
    public static final String CST_DATA_FORM = "form";
    public static final String CST_DATA_MAXDATA = "maxdata";
    
    public String name;
    public String type;
    public Long id;
    public String description;
    public String datasource;
    public String tablename;
    public String persistenceidname;
    public boolean allowModificationStructure;
    public Map<String,Object> dataForm;
    public int maxData;
    public APISession apiSession;
    
    public List<Map<String,Object>> columns;
    
    public Map<String,Object> data;
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static BookMobileParameter getInstanceFromJsonSt( String jsonSt, APISession apiSession) {
        BookMobileParameter bookMobileParameter = new BookMobileParameter();
        bookMobileParameter.apiSession = apiSession;
        final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);
        bookMobileParameter.id                          = CastData.getLong( jsonHash.get( CST_MODEL_ID ),null);
        bookMobileParameter.name                        = (String) jsonHash.get( CST_MODEL_NAME);
        bookMobileParameter.type                        = (String) jsonHash.get( CST_MODEL_TYPE);
        bookMobileParameter.description                 = (String) jsonHash.get( CST_MODEL_DESCRIPTION);
        bookMobileParameter.datasource                  = (String) jsonHash.get( CST_MODEL_DATASOURCENAME);
        bookMobileParameter.tablename                   = (String) jsonHash.get( CST_MODEL_TABLENAME);
        bookMobileParameter.persistenceidname           = (String) jsonHash.get( CST_MODEL_PERSISTENCEIDNAME);
        bookMobileParameter.data                        = (Map) jsonHash.get( CST_DATA );
        bookMobileParameter.allowModificationStructure  =false;
        if (jsonHash.get( CST_MODEL_ALLOW_MODIFY_STRUCTURE)!=null)
            bookMobileParameter.allowModificationStructure  = "true".equalsIgnoreCase( jsonHash.get( CST_MODEL_ALLOW_MODIFY_STRUCTURE).toString());
        bookMobileParameter.id = (Long) jsonHash.get( "id");
        bookMobileParameter.columns = (List) jsonHash.get( "columns");
        
        if (jsonHash.get( CST_DATA_FORM)!=null)
            bookMobileParameter.dataForm = (Map<String,Object>) jsonHash.get( CST_DATA_FORM );
            
        if (jsonHash.containsKey( CST_DATA_MAXDATA ))
            bookMobileParameter.maxData = (Integer) jsonHash.get( CST_DATA_MAXDATA );
        else
            bookMobileParameter.maxData =20;
        
        if (bookMobileParameter.maxData >1000)
            bookMobileParameter.maxData=1000;

        return bookMobileParameter;
    }
    
    public static BookMobileParameter getInstanceFromApiSession( APISession apiSession) {
        BookMobileParameter bookMobileParameter = new BookMobileParameter();
        bookMobileParameter.apiSession = apiSession;
        return bookMobileParameter;
    }
}
