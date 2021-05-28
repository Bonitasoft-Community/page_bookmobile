package org.bonitasoft.custompage.bookmobile.catalog;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.custompage.bookmobile.BookMobileParameter;
import org.bonitasoft.custompage.bookmobile.CatalogResult;
import org.bonitasoft.custompage.bookmobile.data.DataRecord;
import org.bonitasoft.custompage.bookmobile.data.DataSearch;
import org.bonitasoft.custompage.bookmobile.data.DataTransformerInt;
import org.bonitasoft.custompage.bookmobile.data.DataRecord.SqlOperation;
import org.bonitasoft.custompage.bookmobile.database.DatasourceConnection;
import org.bonitasoft.custompage.bookmobile.database.DatasourceConnection.ConnectionResult;
import org.bonitasoft.custompage.bookmobile.database.TableModel;
import org.bonitasoft.custompage.bookmobile.database.TableModel.COLTYPE;
import org.bonitasoft.custompage.bookmobile.database.TableModel.DataColumn;
import org.bonitasoft.custompage.bookmobile.database.TableUpdate;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.json.simple.JSONValue;

public class CatalogModel {

    protected static BEvent eventBadInformation = new BEvent(CatalogModel.class.getName(), 1, Level.APPLICATIONERROR,
            "Bad Information", "The model expect some mandatory information to be active",
            "The model does not work", "Check and complete information");

    protected static BEvent eventCantModifyStructure = new BEvent(CatalogModel.class.getName(), 2, Level.APPLICATIONERROR,
            "Can't modify the structure", "This model is protected againts any modification",
            "Table is not modified", "Allow the modification");

    protected static BEvent eventDefinitionIncorrect = new BEvent(CatalogModel.class.getName(), 3, Level.APPLICATIONERROR,
            "Definition incorrect", "This model has an error",
            "Model can't be used", "Check analysis");

    protected static BEvent eventUpdateSuccess = new BEvent(CatalogModel.class.getName(), 4, Level.SUCCESS,
            "Update/insert success", "Update/insert is executed with success");
    
    protected static BEvent eventUpdateIncorrect = new BEvent(CatalogModel.class.getName(), 5, Level.APPLICATIONERROR,
            "Update/insert incorrect", "The update/insert is incorrect. We expected to update/insert some records, and a different number was iupdaUpdate is executed with success",
            "The operation is executed, but not with the expected result",
            "Check the result");

    
    protected static BEvent eventSqlError = new BEvent(CatalogModel.class.getName(), 6, Level.ERROR,
            "Sql Error", "Sql request have an error", "Execution failed", "Check the exception ");


    public String name;
    public enum TypeModel { MODEL, BDM }
    public TypeModel type = TypeModel.MODEL;
    public String description;
    public String datasourceName;
    
    public boolean allowModifyStructure;
    /**
     * Each data must have an uniq ID. This is the name of this unique ID
     */
    public String colPersistenceId;
    public String profileNameRead;
    public String profileNameWrite;

   public TableModel tableModel = new TableModel();
   
    public long id;
    private CatalogFactory catalogFactory;
    private BdmFactory bdmFactory;
    
    public CatalogModel(Long id, CatalogFactory catalogFactory) {
        this.id = id;
        this.catalogFactory = catalogFactory;
    }
    public CatalogModel(String name, BdmFactory bdmFactory) {
        this.name = name;
        this.bdmFactory  = bdmFactory;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* getter */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public DataColumn getColumnByName( String colName ) {
        for( DataColumn col : this.tableModel.listDataColumn) {
            if (col.colName.equalsIgnoreCase( colName))
                return col;
        }
        return null;
    }
    
    public List<DataColumn> getColumns() {
        return this.tableModel.listDataColumn;
    }
        
    public long getId() {
        return id;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Serialisation */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public Map<String, Object> getMap( boolean completeDetails ) {
        Map<String, Object> result = new HashMap<>();
        result.put( BookMobileParameter.CST_MODEL_NAME, name);
        result.put( BookMobileParameter.CST_MODEL_TYPE, type == null ? TypeModel.MODEL.toString(): type.toString() );
        result.put( BookMobileParameter.CST_MODEL_ID, id);
        result.put( BookMobileParameter.CST_MODEL_DESCRIPTION, description);
        result.put( BookMobileParameter.CST_MODEL_DATASOURCENAME, datasourceName);
        result.put( BookMobileParameter.CST_MODEL_TABLENAME, tableModel.tableName);
        result.put( BookMobileParameter.CST_MODEL_PROFILENAMEREAD, profileNameRead);
        result.put( BookMobileParameter.CST_MODEL_PROFILENAMEWRITE, profileNameWrite);
        result.put( BookMobileParameter.CST_MODEL_PERSISTENCEIDNAME, colPersistenceId.toUpperCase());
        result.put( BookMobileParameter.CST_MODEL_ALLOW_MODIFY_STRUCTURE, allowModifyStructure);
        if (completeDetails) {
            List<Map<String,Object>> listColumnMap = new ArrayList();
            for( DataColumn col : tableModel.listDataColumn) {
                listColumnMap.add( col.getMap());
            }
            result.put("columns", listColumnMap);
        }
        return result;
    }

    public void setFromMap(Map<String, Object> information) {
        name                    = (String) information.get( BookMobileParameter.CST_MODEL_NAME);
        try {
            type = TypeModel.valueOf( (String) information.get( BookMobileParameter.CST_MODEL_TYPE) );
        } catch( Exception e) {
            type = TypeModel.MODEL;
        }
        id                      = (Long) information.get( BookMobileParameter.CST_MODEL_ID);
        description             = (String) information.get( BookMobileParameter.CST_MODEL_DESCRIPTION );
        datasourceName          = (String) information.get( BookMobileParameter.CST_MODEL_DATASOURCENAME);
        tableModel.tableName    = (String) information.get( BookMobileParameter.CST_MODEL_TABLENAME);
        profileNameRead         = (String) information.get(  BookMobileParameter.CST_MODEL_PROFILENAMEREAD);
        profileNameWrite        = (String) information.get(  BookMobileParameter.CST_MODEL_PROFILENAMEWRITE);
        colPersistenceId        = (String) information.get(  BookMobileParameter.CST_MODEL_PERSISTENCEIDNAME);
        allowModifyStructure    = (Boolean) information.get(  BookMobileParameter.CST_MODEL_ALLOW_MODIFY_STRUCTURE);
        List<Map<String,Object>> listColMap = (List<Map<String,Object>>) information.get("columns");
        for (Map<String,Object> record : listColMap) {
            tableModel.listDataColumn.add( new DataColumn(record));
        }
        
    }

    @SuppressWarnings("unchecked")
    
    public static CatalogModel getFromJson(String jsonSt,CatalogFactory catalogFactory) {
        final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);
        CatalogModel catalogModel = new CatalogModel(-1L, catalogFactory);
        catalogModel.setFromMap(jsonHash);
        return catalogModel;
    }
    

    public String getJson() {
        return JSONValue.toJSONString(getMap( true ));
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Change the model */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    
    public List<BEvent> populateFromTable(BookMobileParameter bookMobileParameter) {
        List<BEvent> listEvents = new ArrayList<>();
        ConnectionResult conResult = DatasourceConnection.getConnection(datasourceName);
        listEvents.addAll(conResult.listEvents);
        TableUpdate tableUpdate = new TableUpdate();
        if (conResult.con != null) {
            TableModel tableModelReal = tableUpdate.getTableDescription( conResult.con, tableModel.tableName);
            for (DataColumn columnReal : tableModelReal.listDataColumn) {
                Integer indiceColModel = this.tableModel.getColumnIndexByName( columnReal.colName);
                if (indiceColModel==null) {
                    this.tableModel.listDataColumn.add( columnReal);
                } else {
                    // Merge it
                    DataColumn colModel = this.tableModel.listDataColumn.get( indiceColModel);
                    colModel.colSize = columnReal.colSize;
                    colModel.colType = columnReal.colType;
                    // Not really necessary in Java, but sure we update the current list
                    this.tableModel.listDataColumn.set( indiceColModel, colModel); 
                }
            }
            try {
                conResult.con.close();
            } catch (SQLException e) { }
        }
        return listEvents;
    }
    
    /**
     * Update the model definition
     * @param bookMobileParameter
     * @return
     */
    public List<BEvent> update(BookMobileParameter bookMobileParameter) {
        List<BEvent> listEvents = new ArrayList<>();
        StringBuilder analyse = new StringBuilder();
        this.name               = bookMobileParameter.name ==null ? "" : bookMobileParameter.name;
        this.description        = bookMobileParameter.description;
        this.type               = TypeModel.valueOf(bookMobileParameter.type );
        this.datasourceName     = bookMobileParameter.datasource==null ? "" : bookMobileParameter.datasource;
        this.tableModel.tableName= bookMobileParameter.tablename==null ? "" : bookMobileParameter.tablename.toUpperCase();
        this.colPersistenceId   = bookMobileParameter.persistenceidname ==null ? "" : bookMobileParameter.persistenceidname;
        this.allowModifyStructure = bookMobileParameter.allowModificationStructure;
        this.tableModel.listDataColumn.clear();
        for (Map<String, Object> column : bookMobileParameter.columns) {
            this.tableModel.listDataColumn.add( new DataColumn(column));
        }

        if (this.name.trim().length() == 0) {
            analyse.append("Name is missing;");
        }
        if (this.datasourceName.trim().length() == 0) {
            analyse.append("Datasource is missing;");
        } else {
            ConnectionResult conResult = DatasourceConnection.getConnection(datasourceName);
            listEvents.addAll(conResult.listEvents);
            if (conResult.con == null)
                analyse.append("Datasource can't be connected;");
            else
                try {
                    conResult.con.close();
                } catch (SQLException e) { }
        }
        if (this.colPersistenceId.trim().length() == 0)
            analyse.append("A Unique ID column name must be provided;");
        
        if (this.type == TypeModel.MODEL && this.getColumnByName( this.colPersistenceId) == null)
            analyse.append("the Unique ID must match an existing column;");

        if (this.type == TypeModel.MODEL &&this.tableModel.tableName.trim().length() == 0)
            analyse.append("A table name must be provided;");

        if (this.tableModel.listDataColumn.isEmpty())
            analyse.append("Columns has to be specified;");
        
        // check Column
        for (int i=0;i<this.tableModel.listDataColumn.size();i++) {
            DataColumn column = this.tableModel.listDataColumn.get( i );
            if (column.colType == COLTYPE.STRING && column.colSize<=0) {
                analyse.append("Column["+column.colName+"] is a STRING, must have a size;");
            }
            for (int j=i+1;j<this.tableModel.listDataColumn.size();j++) {
                if (this.tableModel.listDataColumn.get( j ).colName.equalsIgnoreCase( column.colName))
                    analyse.append("Column["+column.colName+"] appears multiple time in the list (position "+ (i+1)+" and "+(j+1)+";");
            }
        }
       
        // do not modify the datbase if something is wrong
        boolean doModification = this.allowModifyStructure;
        if (analyse.length()>0)
            doModification = false;
        listEvents.addAll(synchronizeModel( doModification ));
        
        if (analyse.length()>0)
            listEvents.add( new BEvent(eventDefinitionIncorrect, analyse.toString()));
        this.catalogFactory.setModel( this);
        
        return listEvents;
    }

    /**
     * 
     * @param allowModifyStructure
     * @return
     */
    public List<BEvent> synchronizeModel( boolean allowModifyStructure) {
        List<BEvent> listEvents = new ArrayList<>();
        // BDM ? No synchronization is needed.
        if (this.type == TypeModel.BDM)
            return listEvents;
        // check and modify
        TableUpdate tableUpdate = new TableUpdate();
        ConnectionResult conResult = DatasourceConnection.getConnection(datasourceName);
        listEvents.addAll(conResult.listEvents);
        if (conResult.con != null) {
            TableModel tableModelReal = tableUpdate.getTableDescription( conResult.con, tableModel.tableName);
            listEvents.addAll(tableUpdate.synchronize(conResult.con, this, tableModelReal, allowModifyStructure));
            try {
                conResult.con.close();
            } catch (SQLException e) { }
        }
        return listEvents;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Get Data */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public void dataSearch(CatalogSearchParameter searchParameters,  DataTransformerInt dataTransformer, CatalogResult catalogResult) {
        DataSearch dataSearch = new DataSearch( this );
        dataSearch.dataSearch( searchParameters, dataTransformer, catalogResult );
        
    }

    public CatalogMetaModel modelRead() {
        // do a MetaData on table
        return new CatalogMetaModel();
    }

    public List<BEvent> modelUpdate(CatalogMetaModel catalogModel) {
        List<BEvent> listEvents = new ArrayList<>();
        // do a diff between the MetaData and the new requested
        return listEvents;
    }

    
    
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Get Data */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public List<BEvent> dataInsert( DataRecord dataRecord) {
        List<BEvent> listEvents = new ArrayList<>();
        
        SqlOperation sqlRequest = dataRecord.getSqlInsert( tableModel );
        listEvents.addAll( executeSqlUpdate( sqlRequest,1 ));
        return listEvents;
    }

    public List<BEvent> dataUpdate( DataRecord dataRecord) {
        List<BEvent> listEvents = new ArrayList<>();
        SqlOperation sqlRequest = dataRecord.getSqlUpdate( colPersistenceId, tableModel );
        listEvents.addAll( executeSqlUpdate( sqlRequest,1 ));

        return listEvents;
    }

    /**
     * Delete the data.
     * @param dataRecord
     * @return
     */
    public List<BEvent> deleteData(DataRecord dataRecord) {
        List<BEvent> listEvents = new ArrayList<>();
        SqlOperation sqlRequest = dataRecord.getSqlDelete( colPersistenceId, tableModel );
        listEvents.addAll( executeSqlUpdate( sqlRequest,1 ));

        return listEvents;

    }
    
    private  List<BEvent> executeSqlUpdate(SqlOperation sqlRequest, int nbUpdateExpected) {
        List<BEvent> listEvents = new ArrayList<>();
        ConnectionResult conResult = DatasourceConnection.getConnection(datasourceName);
        listEvents.addAll(conResult.listEvents);
        if (conResult.con != null) {
            try (PreparedStatement pstmt = conResult.con.prepareStatement( sqlRequest.getSqlRequest())) {
                for (int i=0;i<sqlRequest.listParameters.size();i++) {
                    pstmt.setObject(i+1, sqlRequest.listParameters.get( i ));
                }
                int nbExecution = pstmt.executeUpdate();
                if (nbUpdateExpected == nbExecution) {
                    listEvents.add( eventUpdateSuccess);
                }
                else
                    // we expect to have one parameter updated
                    listEvents.add( eventUpdateIncorrect);
            } catch(Exception e) {
                listEvents.add( new BEvent(eventSqlError, e, e.getMessage()));
            }
            try {
                conResult.con.close();
            } catch (SQLException e) {
            }
        }
        return listEvents;
    }
   

}
