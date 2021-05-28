package org.bonitasoft.custompage.bookmobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.custompage.bookmobile.catalog.CatalogModel;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;

public class CatalogResult {

    public enum StatusEnum { OK };
    public StatusEnum status = StatusEnum.OK;
    public List<BEvent> listEvents = new ArrayList<>();
    public CatalogModel catalogModel; 
    public List<CatalogModel> listModels = new ArrayList<>();
    public List<String> listBdmAvailable = new ArrayList<>();
    
    private List<Map<String,Object>> listDatas = new ArrayList<>();
    
    public Map<String,Object> getMap() {
        Map<String,Object> result = new HashMap<>();
        result.put("status", status.toString());
        List<Map<String,Object>> listCatalogMap = new ArrayList();
        for (CatalogModel model : listModels)
            listCatalogMap.add( model.getMap( false ));
        result.put("listcatalog", listCatalogMap);
        
        if (catalogModel!=null) {
            result.put("model", catalogModel.getMap( true ));
        }
        if (listDatas!=null) {
            result.put("datas", listDatas);
        }
        
        result.put("listbdmavailable", listBdmAvailable);
        result.put("listevents", BEventFactory.getHtml(listEvents));
        return result;
    }
    public List<Map<String,Object>> getData() {
        return listDatas;
    }
    public void addData( Map<String,Object> data) {        
        listDatas.add( data );
    }
    public int getDataSize() {
        return listDatas.size();
    }
}
