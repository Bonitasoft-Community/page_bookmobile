package org.bonitasoft.custompage.bookmobile.database;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.custompage.bookmobile.catalog.CatalogModel;
import org.bonitasoft.custompage.bookmobile.database.TableModel.COLTYPE;
import org.bonitasoft.custompage.bookmobile.database.TableModel.DataColumn;

/* ******************************************************************************** */
/*                                                                                  */
/*  TableUpdate,                                                                    */
/*                                                                                  */
/*  Mechanism to update / Create table                                              */
/*                                                                                  */
/*                                                                                  */
/* ******************************************************************************** */

public class TableUpdate {
    
    static Logger logger = Logger.getLogger( TableUpdate.class.getName());
    public static final String LOGGER_LABEL = "TableUpdate:";

    protected static BEvent eventCreationDatabase = new BEvent(TableUpdate.class.getName(), 1, Level.ERROR,
            "Error during creation the table in the database", "Check Exception ",
            "The properties will not work (no read, no save)", "Check Exception");
    protected static BEvent eventTableAreIndentical = new BEvent(TableUpdate.class.getName(), 2, Level.INFO,
            "Table are identical", "Table in the model are identical with the database");
    protected static BEvent eventTableUpdated = new BEvent(TableUpdate.class.getName(), 3, Level.INFO,
            "Table Updated", "Table in the model are different, and are updated");
    protected static BEvent eventTableCreated = new BEvent(TableUpdate.class.getName(), 4, Level.INFO,
            "Table Created", "Table is created");


   
    
    public TableModel getTableDescription( final Connection con, String tableNameModel) {
        TableModel tableModel = new TableModel();
    
        try {
            final DatabaseMetaData dbm = con.getMetaData();
            tableModel.tableExist = false;
            final String databaseProductName = dbm.getDatabaseProductName();

            // check if "employee" table is there
            // nota: don't use the patern, it not give a correct result with H2
            final ResultSet tables = dbm.getTables(null, null, null, null);

            boolean exist = false;
            while (tables.next()) {
                final String tableName = tables.getString("TABLE_NAME");
                if (tableName.equalsIgnoreCase( tableNameModel )) {
                    exist = true;
                    break;
                }
            }
            // logAnalysis.append( "Table [" + model.tableName + "] exist? " + exist + ";");
            if (exist) {
                tableModel.tableExist = true;
                // Table exists : is the fields are correct ?
                final ResultSet rs = dbm.getColumns(null /* catalog */, null /* schema */, null /* cstSqlTableName */,
                        null /* columnNamePattern */);

                while (rs.next()) {
                    String tableNameCol = rs.getString("TABLE_NAME");
                    final String colName = rs.getString("COLUMN_NAME");
                    final int colSize = rs.getInt("COLUMN_SIZE");
                    final int colDatatype = rs.getInt("DATA_TYPE");
                    
                    tableNameCol = tableNameCol == null ? "" : tableNameCol;

                    if (!tableNameCol.equalsIgnoreCase(tableNameModel)) {
                        continue;
                    }
                    // final int dataType = rs.getInt("DATA_TYPE");
                    DataColumn column = tableModel.addColumn( colName, getFromDatabaseType( colDatatype, databaseProductName));
                    column.colSize = colSize;
                }
            }
        }catch(Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            final String exceptionDetails = sw.toString();
            logger.severe(LOGGER_LABEL+" Error get "+e.toString() + " at "+exceptionDetails);
        }
        return tableModel;
    }
    
    
    
    public List<BEvent> synchronize(final Connection con, CatalogModel model, TableModel tableModelReal, boolean allowModifyStructure) {

        final List<BEvent> listEvents = new ArrayList<>();
        StringBuilder logAnalysis = new StringBuilder();
        
        java.util.logging.Level logLevelAnalysis = java.util.logging.Level.INFO;

        try {
            final DatabaseMetaData dbm = con.getMetaData();
            final String databaseProductName = dbm.getDatabaseProductName();

            if (tableModelReal.tableExist) {
                
                for (DataColumn dataColumnExpected : model.tableModel.listDataColumn) {
                    DataColumn dataColumnExist = tableModelReal.getColumnByName( dataColumnExpected.colName);
                    if (dataColumnExist == null) {
                        String sqlRequest = "alter table " + model.tableModel.tableName + " add  " + getSqlField(dataColumnExpected, databaseProductName);
                        logAnalysis.append( sqlRequest + ";");
                        if (allowModifyStructure)
                            executeAlterSql(con, sqlRequest);
                    } else {
                        // same column ?
                        List<String> sqlDifferences = dataColumnExist.compareColumn( dataColumnExist);
                        if ( ! sqlDifferences.isEmpty()) {
                            logAnalysis.append( sqlDifferences.toString() + ";");
                            if (allowModifyStructure) {
                                for (String sql : sqlDifferences)
                                    executeAlterSql(con, sql);
                            }
                        }                            
                    }
                 // Now, opposite, do we have MORE table than expected ? 
                    for (DataColumn dataColumnReal : tableModelReal.listDataColumn) {
                        DataColumn dataColumnModel = model.tableModel.getColumnByName( dataColumnReal.colName);
                        if (dataColumnModel ==null) {
                            logAnalysis.append( "Column ["+dataColumnReal.colName+"] in table, not in model;");
                        }
                            
                    }
                    
                }
                if (logAnalysis.length()==0) {
                    listEvents.add( eventTableAreIndentical );
                } else {
                    listEvents.add( new BEvent( eventTableUpdated, logAnalysis.toString()));
                }
            }
            else {
                // create the table
                final StringBuilder createTableString = new StringBuilder();
                createTableString.append("create table " + model.tableModel.tableName + " (");
                for (int i=0;i<model.tableModel.listDataColumn.size();i++) {
                    createTableString.append( getSqlField( model.tableModel.listDataColumn.get( i ), databaseProductName));
                    if (i<model.tableModel.listDataColumn.size()-1)
                        createTableString.append(" , ");
                }
                createTableString.append( ")" );
                logAnalysis.append( "CheckCreateTable [" + model.tableModel.tableName + "] : NOT EXIST : create it with script[" + createTableString + "]");
                if (allowModifyStructure)
                    executeAlterSql(con, createTableString.toString());
                listEvents.add( eventTableCreated);

            }
        } catch (final SQLException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            final String exceptionDetails = sw.toString();
            logLevelAnalysis = java.util.logging.Level.SEVERE;

            logAnalysis.append( " ERROR during checkCreateDatase properties [" +  model.tableModel.tableName + "] : "
                    + e.toString() + " : " + exceptionDetails);
            listEvents.add(new BEvent(eventCreationDatabase, e, "properties name;[" + model.tableModel.tableName + "]"));

        }

        logger.log(logLevelAnalysis, logAnalysis.toString());
        return listEvents;
    }

    private void executeAlterSql(final Connection con, final String sqlRequest) throws SQLException {
        logger.info( LOGGER_LABEL + "executeAlterSql : Execute [" + sqlRequest + "]");

        try ( Statement stmt = con.createStatement() ) {
            stmt.executeUpdate(sqlRequest);

            if (!con.getAutoCommit()) {
                con.commit();
            }
        } catch (Exception e) {
            throw e;
        }
    }
    

/* ******************************************************************************** */
/*                                                                                  */
/*  Manage data type,                                                                 */
/*                                                                                  */
/*                                                                                  */
/*                                                                                  */
/* ******************************************************************************** */

    private static final String CST_DRIVER_H2 = "H2";
    private static final String CST_DRIVER_ORACLE = "oracle";
    private static final String CST_DRIVER_POSTGRESQL = "PostgreSQL";
    private static final String CST_DRIVER_MYSQL = "MySQL";
    private static final String CST_DRIVER_SQLSERVER = "Microsoft SQL Server";
    
 
    public class TypeTranslation {

        public COLTYPE colType;
        public Map<String, String> translationTable = new HashMap<>();

        public TypeTranslation(COLTYPE colType, String oracle, String postGres, String h2, String mySql, String sqlServer, String def) {
            this.colType = colType;
            translationTable.put(CST_DRIVER_ORACLE, oracle);
            translationTable.put(CST_DRIVER_POSTGRESQL, postGres);
            translationTable.put(CST_DRIVER_H2, h2);
            translationTable.put(CST_DRIVER_MYSQL, mySql);
            translationTable.put(CST_DRIVER_SQLSERVER, sqlServer);
            translationTable.put("def", def);
        }

        public String getValue(String databaseName) {
            if (translationTable.get(databaseName) != null)
                return translationTable.get(databaseName);
            return translationTable.get("def");
        }
    }
    /* String oracle, String postGres, String h2, String mySql, String sqlServer, String def */
    private List<TypeTranslation> allTransations = Arrays.asList(
            new TypeTranslation(COLTYPE.LONG, "NUMBER", "BIGINT", "BIGINT", "BIGINT", "NUMERIC(19, 0)", "NUMBER"),
            new TypeTranslation(COLTYPE.BOOLEAN, "NUMBER(1)", "BOOLEAN", "BOOLEAN", "BOOLEAN", "BIT", "BOOLEAN"),
            new TypeTranslation(COLTYPE.DECIMAL, "NUMERIC(19,5)", "NUMERIC(19,5)", "NUMERIC(19,5)", "NUMERIC(19,5)", "NUMERIC(19,5)", "NUMERIC(19,5)"),
            new TypeTranslation(COLTYPE.BLOB, "BLOB", "BYTEA", "MEDIUMBLOB", "MEDIUMBLOB", "VARBINARY(MAX)", "BLOB"),
            new TypeTranslation(COLTYPE.TEXT, "CLOB", "TEXT", "CLOB", "MEDIUMTEXT", "NVARCHAR(MAX)", "TEXT"),
            new TypeTranslation(COLTYPE.STRING, "VARCHAR2(%d CHAR)", "VARCHAR(%d)", "VARCHAR(%d)", "VARCHAR(%d)", "NVARCHAR(%d)", "VARCHAR(%d)")
            );

    private COLTYPE getFromDatabaseType( int dataType, String databaseProductName ) {
        if (dataType == Types.BIGINT)
            return COLTYPE.LONG;
        if (dataType == Types.BIT || dataType == Types.BOOLEAN)
            return COLTYPE.BOOLEAN;
        if (dataType == Types.DECIMAL || dataType == Types.FLOAT || dataType == Types.DOUBLE)
            return COLTYPE.DECIMAL;
        if (dataType == Types.BLOB || dataType == Types.CLOB)
            return COLTYPE.BLOB;
        if (dataType == Types.CHAR || dataType == Types.NCHAR )
            return COLTYPE.TEXT;
        if (dataType == Types.NVARCHAR)
            return COLTYPE.STRING;
        return COLTYPE.STRING;
    }
    private String getSqlField(final DataColumn col, final String databaseProductName) {
        for (TypeTranslation typeTranslation : allTransations) {
            if (typeTranslation.colType == col.colType) {
                String value = String.format(typeTranslation.getValue(databaseProductName), col.colSize);
                return col.colName + " " + value;
            }
        }
        return "";
    }

}
