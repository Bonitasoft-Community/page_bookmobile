package org.bonitasoft.custompage.bookmobile.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bonitasoft.custompage.bookmobile.catalog.CatalogModel;
import org.bonitasoft.custompage.bookmobile.database.TableModel;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

public class DataRecord {


    
    private Map<String,Object> data;
    
    public static DataRecord getInstance( Map<String,Object> data ) {
        DataRecord dataRecord = new DataRecord();
        dataRecord.data = data;
        return dataRecord;
    }
    
    public class SqlOperation {
        public StringBuilder sqlRequest = new StringBuilder();
        public List<Object> listParameters = new ArrayList<>();
        
        public String getSqlRequest() {
            return sqlRequest.toString();
        }
    }
    
    /**
     * Get Sql Insert function
     * @param tableModel
     * @return
     */
    public SqlOperation getSqlInsert( TableModel tableModel ) {
        SqlOperation sqlOperation = new SqlOperation();
        sqlOperation.sqlRequest.append("insert into "+tableModel.tableName+" (");
        StringBuffer listParameters = new StringBuffer();
        for(int i=0;i<tableModel.listDataColumn.size();i++) {
            if (i>0) {
                sqlOperation.sqlRequest.append(",");
                listParameters.append(",");
            }
            if (tableModel.listDataColumn.get( i ).addSqlValue !=null && tableModel.listDataColumn.get( i ).addSqlValue.length()>0) {
                sqlOperation.sqlRequest.append( tableModel.listDataColumn.get( i ).colName);
                String sqlValue = tableModel.listDataColumn.get( i ).addSqlValue;
                if (! sqlValue.trim().startsWith("("))
                    sqlValue = "("+sqlValue+")";
                listParameters.append(" "+sqlValue+" ");
            } else {
                sqlOperation.sqlRequest.append( tableModel.listDataColumn.get( i ).colName);
                listParameters.append("?");
                sqlOperation.listParameters.add( data.get(tableModel.listDataColumn.get( i ).colName));
            }
        }
        sqlOperation.sqlRequest.append(") values ("+listParameters.toString()+")");
        return sqlOperation;
    }
    
    /**
     * return the update operation. It's based on the colPersistenceid, assing there is only one colum with the key
     * @param colPersistenceId
     * @param tableModel
     * @return
     */
    public SqlOperation getSqlUpdate( String colPersistenceId, TableModel tableModel ) {
        SqlOperation sqlOperation = new SqlOperation();
        sqlOperation.sqlRequest.append("update "+tableModel.tableName+" set ");
        
        for(int i=0;i<tableModel.listDataColumn.size();i++) {
            if (i>0) {
                sqlOperation.sqlRequest.append(",");
            }
            sqlOperation.sqlRequest.append(tableModel.listDataColumn.get( i ).colName+"= ?");
            sqlOperation.listParameters.add( data.get(tableModel.listDataColumn.get( i ).colName));
        }
        sqlOperation.sqlRequest.append(" where "+colPersistenceId+"= ?");
        sqlOperation.listParameters.add( data.get(colPersistenceId));
        return sqlOperation;
    }
}
