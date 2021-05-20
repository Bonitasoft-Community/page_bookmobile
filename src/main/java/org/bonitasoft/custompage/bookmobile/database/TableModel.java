package org.bonitasoft.custompage.bookmobile.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.custompage.bookmobile.tool.CastData;

public class TableModel {

    public enum COLTYPE {
        LONG, BLOB, STRING, BOOLEAN, DECIMAL, TEXT, LOCALDATE, LOCALDATETIME,OFFSETDATETIME
    }

    public static class DataColumn {

        public String colName;
        /**
         * Label to show to the user
         */
        public String colLabel;
        
        /**
         * user can add a tip
         */
        public String colTip;
        
        /**
         * In case of Add, this is the default value proposed
         */
        public String addDefaultValue;
        /**
         * In case of Add, this is the sql value used by the request. Typically, for the PersistenceId Bdm, the persistenceId is a max+1
         * When the addSqlValue is set, then the interface move the field to addReadOnly
         */
        public String addSqlValue;
        
        public boolean addReadOnly=false;
        public boolean updateReadOnly=false;
        
        public COLTYPE colType;
        public int colSize;


        public DataColumn(String colName, COLTYPE colType, int colSize) {
            this.colName = colName.toLowerCase();
            this.colType = colType;
            this.colSize = colSize;
        }
        /*
         * for all field without a size (LONG, BLOG)
         */
        public DataColumn(String colName, COLTYPE colType) {
            this.colName = colName.toLowerCase();
            this.colType = colType;
            this.colSize = 0;
        }
        
        public DataColumn(Map<String, Object> column) {
            this.colName = (String) column.get("name");
            if (this.colName!=null)
                this.colName = this.colName.toUpperCase();
            this.colType = COLTYPE.valueOf((String) column.get("type"));
            try {
                this.colSize = Integer.parseInt( column.get("size").toString() );
            }
            catch(Exception e) {
                // normal : all the column does not have a size
            }
            this.colLabel   = (String) column.get("label");
            this.colTip     = (String) column.get("tip");
            this.addDefaultValue    =  (String) column.get("addDefaultValue");
            this.addSqlValue        =      (String) column.get("addSqlValue");
            this.addReadOnly        =      CastData.getBoolean( column.get("addReadOnly"), Boolean.FALSE);
            this.updateReadOnly     =      CastData.getBoolean( column.get("updateReadOnly"), Boolean.FALSE);
        }

        public Map<String, Object> getMap() {
            Map<String, Object> column = new HashMap<>();
            column.put("name", this.colName.toUpperCase());
            column.put("label", this.colLabel);
            column.put("addDefaultValue", this.addDefaultValue);
            column.put("addSqlValue", this.addSqlValue);
            column.put("addReadOnly", this.addReadOnly);
            column.put("updateReadOnly", this.updateReadOnly);
                    
            column.put("type", colType.toString());
            column.put("size", this.colSize);
            
            column.put("tip", this.colTip);
            return column;
        }

        public List<String> compareColumn( DataColumn colToCompare) {
            List<String> listSql = new ArrayList<>();
            String sqlRequestDifference ="";
            if (colType != colToCompare.colType) {
                listSql.add("drop colum "+colToCompare.colName );
                listSql.add("alter table add "+colToCompare.colName );
            }
            else if (colType == COLTYPE.STRING && colSize != colToCompare.colSize) {
                listSql.add("alter table "+colToCompare.colName+" size " );
            }
            return listSql;
        }
    }
    
    
   

    public boolean tableExist;
    public String tableName;
    public List<DataColumn> listDataColumn = new ArrayList<>();
    
    
    public TableModel.DataColumn addColumn(String colName, COLTYPE colType) {
        DataColumn col = new DataColumn(colName, colType);
        listDataColumn.add( col ) ;
        return col;
    }
    
    public TableModel.DataColumn getColumnByName(String colName ) {
        for( DataColumn col : listDataColumn) {
            if (col.colName.equalsIgnoreCase(colName))
                return col;
        }
        return null;
    }
    
    public Integer getColumnIndexByName(String colName ) {
        for(int i=0;i<listDataColumn.size();i++) {
            if (listDataColumn.get( i ).colName.equalsIgnoreCase(colName))
                return i;
        }
        return null;
    }
    
}
