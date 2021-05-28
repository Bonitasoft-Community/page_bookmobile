package org.bonitasoft.custompage.bookmobile.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bonitasoft.custompage.bookmobile.BookMobileParameter;
import org.bonitasoft.custompage.bookmobile.CatalogResult;
import org.bonitasoft.custompage.bookmobile.catalog.CatalogModel;
import org.bonitasoft.custompage.bookmobile.catalog.CatalogSearchParameter;
import org.bonitasoft.custompage.bookmobile.database.DatasourceConnection;
import org.bonitasoft.custompage.bookmobile.database.DatasourceConnection.ConnectionResult;
import org.bonitasoft.custompage.bookmobile.database.TableModel.COLTYPE;
import org.bonitasoft.custompage.bookmobile.database.TableModel.DataColumn;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

public class DataSearch {

    protected static BEvent eventErrorSearch = new BEvent(DataSearch.class.getName(), 1, Level.APPLICATIONERROR,
            "Error during search", "The Search failed, no data retrieved",
            "Data will be empty", "Check Exception");

    private CatalogModel catalogModel;

    public DataSearch(CatalogModel catalogModel) {
        this.catalogModel = catalogModel;
    }

    /**
     * Search data
     */
    public void dataSearch(CatalogSearchParameter searchParameter, DataTransformerInt dataTransformer, CatalogResult catalogResult) {
        // build the request
        StringBuilder sqlRequest = new StringBuilder();
        sqlRequest.append(" select * from " + catalogModel.tableModel.tableName + " where 1=1 ");
        List<Object> listParameter = new ArrayList<>();
        if (searchParameter.dataForm != null) {
            for (Entry<String, Object> entryForm : searchParameter.dataForm.entrySet()) {

                DataColumn col = catalogModel.getColumnByName(entryForm.getKey());
                if (col == null) {
                    // not normal, the column should exist
                    continue;
                }
                boolean isLike = (col.colType == COLTYPE.STRING || col.colType == COLTYPE.TEXT);

                if (entryForm.getValue() != null && entryForm.getValue().toString().trim().length() > 0) {
                    listParameter.add(isLike ? "%" + entryForm.getValue() + "%" : entryForm.getValue());
                    sqlRequest.append(" and " + entryForm.getKey().toUpperCase() + (isLike ? " like " : " = ") + "?");
                }
            }
        }
        sqlRequest.append(" order by " + catalogModel.colPersistenceId);

        ConnectionResult conResult = DatasourceConnection.getConnection(catalogModel.datasourceName);
        PreparedStatement pstmt = null;
        try {
            catalogResult.listEvents.addAll(conResult.listEvents);
            if (conResult.con != null) {

                pstmt = conResult.con.prepareStatement(sqlRequest.toString());
                for (int i = 0; i < listParameter.size(); i++) {
                    pstmt.setObject(i + 1, listParameter.get(i));
                }
                ResultSet rs = pstmt.executeQuery();
                ResultSetMetaData resultSetMetaData = rs.getMetaData();
                dataTransformer.setMetaData(resultSetMetaData);
                while (rs.next()) {

                    Map<String, Object> record = new HashMap<>();
                    for (int column = 1; column <= resultSetMetaData.getColumnCount(); column++) {

                        record.put(resultSetMetaData.getColumnName(column),
                                dataTransformer.BaseToPage(resultSetMetaData.getColumnName(column), rs.getObject(resultSetMetaData.getColumnName(column))));
                    }
                    catalogResult.addData(record);

                    if (catalogResult.getDataSize() > searchParameter.maxData)
                        break;
                }
            }
        } catch (Exception e) {
            catalogResult.listEvents.add(new BEvent(eventErrorSearch, e, "Exception " + e.getMessage()));
        } finally {
            if (pstmt != null)
                try {
                    pstmt.close();
                } catch (SQLException e) {
                }
            if (conResult.con != null)
                try {
                    conResult.con.close();
                } catch (SQLException e) {
                }
        }
    }

}
