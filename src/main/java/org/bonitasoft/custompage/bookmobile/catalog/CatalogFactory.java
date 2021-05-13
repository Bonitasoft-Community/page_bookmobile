package org.bonitasoft.custompage.bookmobile.catalog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.custompage.bookmobile.catalog.CatalogModel.TypeModel;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

import org.bonitasoft.properties.BonitaProperties;

public class CatalogFactory {
    private static Logger logger = Logger.getLogger(CatalogFactory.class.getName());

    private final static String pageNameBookMobile= "custompage_bookmobile";
    
    private static BEvent EventErrorLoadProperties = new BEvent(CatalogFactory.class.getName(), 1, Level.ERROR, "Properties loading failed", "Properties can't be read", "No catalog model list is available", "Check the exception ");

    private static Map<Long,CatalogFactory> mapCatalogFactory = new HashMap<>();
    
    private Long tenantId;
    private CatalogFactory( long tenantId ) {
        this.tenantId = tenantId;
    }
    

    private Map<Long, CatalogModel> mapCatalogModel = new HashMap<>();
    
    
    /**
     * Get Instance
     * 
     * @param tenantId
     * @return
     */
    public static CatalogFactory getInstance(Long tenantId) {
        CatalogFactory catalog = mapCatalogFactory.get( tenantId );
        if (catalog!=null)
            return catalog;
        catalog = new CatalogFactory( tenantId);
        mapCatalogFactory.put( tenantId, catalog);
        return catalog;
        
    }
    
    public CatalogModel newModel() {
        CatalogModel catalogModel = new CatalogModel(System.currentTimeMillis(), this);
        catalogModel.id = System.currentTimeMillis();
        return catalogModel;
    }

    public CatalogModel findById(Long id ) {
        List<BEvent> listEvents = loadCatalogModel();
        return mapCatalogModel.get( id );
    }
    /**
     * Find by the bdm name. The ID is created 
     * @param name
     * @return
     */
    public CatalogModel findByBdmName(String name ) {
        List<BEvent> listEvents = loadCatalogModel();
        for (CatalogModel model : mapCatalogModel.values()) {
            if (model.type == TypeModel.BDM && model.name.equals( name))
                return model;
        }
        return null;
    }
    public List<BEvent> setModel( CatalogModel model ) {
        mapCatalogModel.put( model.getId(), model);
        return saveCatalogModel( );
    }
    
    public List<BEvent> removeModel( CatalogModel model ) {
        mapCatalogModel.remove( model.getId());
        return saveCatalogModel( );
    }
    public List<CatalogModel> getListModel() {
        return new ArrayList( mapCatalogModel.values() );
    }
    
    
    
    
    public List<BEvent> loadCatalogModel() {
        List<BEvent> listEvents = new ArrayList<>();
        try
        {
            BonitaProperties bonitaProperties = new BonitaProperties( pageNameBookMobile, tenantId );

            listEvents.addAll( bonitaProperties.load() );
            // each line is a model
            Enumeration<String> enums = (Enumeration<String>) bonitaProperties.propertyNames();
            while (enums.hasMoreElements()) {
              String key = enums.nextElement();
              String value = bonitaProperties.getProperty(key);
              CatalogModel model = CatalogModel.getFromJson( value, this );
              mapCatalogModel.put(model.getId(), model);
            }
            
        }
        catch( Exception e )
        {
            logger.severe("Exception "+e.toString());
            listEvents.add( new BEvent(EventErrorLoadProperties, e, ""));
        }
        return listEvents;

    }
    
    public List<BEvent> saveCatalogModel() {
        List<BEvent> listEvents = new ArrayList<>();
        try
        {
            BonitaProperties bonitaProperties = new BonitaProperties( pageNameBookMobile, tenantId );
            for (CatalogModel model : mapCatalogModel.values())
                bonitaProperties.setProperty( String.valueOf( model.getId()), model.getJson());
            
            listEvents.addAll( bonitaProperties.store());
        }
        catch( Exception e )
        {
            logger.severe("Exception "+e.toString());
            listEvents.add( new BEvent("com.bonitasoft.ping", 10, Level.APPLICATIONERROR, "Error using BonitaProperties", "Error :"+e.toString(), "Properties is not saved", "Check exception"));
        }
        return listEvents;

    }

}
