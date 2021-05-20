package org.bonitasoft.custompage.bookmobile.catalog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.bonitasoft.custompage.bookmobile.catalog.CatalogModel.TypeModel;
import org.bonitasoft.custompage.bookmobile.database.TableModel;
import org.bonitasoft.custompage.bookmobile.database.TableModel.COLTYPE;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.api.TenantAdministrationAPI;
import org.bonitasoft.engine.bdm.BusinessObjectModelConverter;
import org.bonitasoft.engine.bdm.model.BusinessObject;
import org.bonitasoft.engine.bdm.model.BusinessObjectModel;
import org.bonitasoft.engine.bdm.model.field.Field;
import org.bonitasoft.engine.bdm.model.field.FieldType;
import org.bonitasoft.engine.bdm.model.field.SimpleField;
import org.bonitasoft.engine.session.APISession;


/**
 * The BDM factory generate a CatalogModel from the BDM. After, this catalog is manipulate in the CatalogFactory.
 * @author Firstname Lastname
 *
 */
public class BdmFactory {
    private static Logger logger = Logger.getLogger(BdmFactory.class.getName());

    private static Map<Long,BdmFactory> mapCatalogFactory = new HashMap<>();
    
    private Long tenantId;
    private BdmFactory( long tenantId ) {
        this.tenantId = tenantId;
    }
   
    /**
     * Get Instance
     * 
     * @param tenantId
     * @return
     */
    public static BdmFactory getInstance(Long tenantId) {
        BdmFactory catalog = mapCatalogFactory.get( tenantId );
        if (catalog!=null)
            return catalog;
        catalog = new BdmFactory( tenantId);
        mapCatalogFactory.put( tenantId, catalog);
        return catalog;
        
    } 
    public List<String> getListBdm(APISession apiSession) {
        List<String> listBdm = new ArrayList<>();
        try {
            TenantAdministrationAPI tenantAdministrationAPI = TenantAPIAccessor.getTenantAdministrationAPI(apiSession);
            BusinessObjectModelConverter converter = new BusinessObjectModelConverter();
    
            byte[] bdmZip = tenantAdministrationAPI.getClientBDMZip();
            byte[] bomZip = getBomFromZip(bdmZip);
    
            BusinessObjectModel bom = converter.unzip(bomZip);
    
            for (BusinessObject businessObject : bom.getBusinessObjects()) {
                listBdm.add( businessObject.getSimpleName());
            }
        }
        catch(Exception e) {
            logger.severe("BdmFactory.getListBdm "+e.getMessage());
        }
        return listBdm;
    }
    
    
    public CatalogModel findByName(String bdmName, APISession apiSession ) {
        CatalogModel catalogModel = new CatalogModel( bdmName, this); 
        try {
            TenantAdministrationAPI tenantAdministrationAPI = TenantAPIAccessor.getTenantAdministrationAPI(apiSession);
            BusinessObjectModelConverter converter = new BusinessObjectModelConverter();
    
            byte[] bdmZip = tenantAdministrationAPI.getClientBDMZip();
            byte[] bomZip = getBomFromZip(bdmZip);
    
            BusinessObjectModel bom = converter.unzip(bomZip);
    
            for (BusinessObject businessObject : bom.getBusinessObjects()) {
                if (! businessObject.getSimpleName().equals( bdmName))
                    continue;
                // Ok, we get the correct one, so now let's populate the columns, description, etc..
                catalogModel.id = businessObject.getSimpleName().hashCode();
                catalogModel.name = businessObject.getSimpleName();

                catalogModel.type = TypeModel.BDM;
                catalogModel.description = businessObject.getDescription();
                catalogModel.datasourceName = "java:comp/env/NotManagedBizDataDS";
                
                catalogModel.allowModifyStructure=false;

                /**
                 * Each data must have an uniq ID. This is the name of this unique ID
                 */
                catalogModel.colPersistenceId = "PERSISTENCEID";
                catalogModel.profileNameRead="";
                catalogModel.profileNameWrite="";

                catalogModel.tableModel = new TableModel();
                catalogModel.tableModel.tableName = businessObject.getSimpleName();
                // describe each field
                for (Field field : businessObject.getFields()) {
                    // public enum COLTYPE { LONG, BLOB, STRING, BOOLEAN, DECIMAL, TEXT
                  
                    if (field instanceof SimpleField) {
                        COLTYPE colType = getTypeFromFieldType(((SimpleField) field).getType());
                        TableModel.DataColumn column = catalogModel.tableModel.addColumn(field.getName(), colType);
                        column.colSize = ((SimpleField) field).getLength();
                        column.updateReadOnly = false;
                        column.addReadOnly = false;
                    } else {
                        // collection : just ignore it
                    }
                    
                }
             
            }
        }
        catch(Exception e) {
            logger.severe("BdmFactory.getListBdm "+e.getMessage());
        }
        return catalogModel;
        
    }
       
    
    public COLTYPE getTypeFromFieldType(FieldType fieldType) {

        if (fieldType == FieldType.CHAR)
            return COLTYPE.STRING;
        if (fieldType == FieldType.DOUBLE || fieldType == FieldType.FLOAT)
            return COLTYPE.DECIMAL;
        if (fieldType == FieldType.INTEGER || fieldType == FieldType.LONG)
            return COLTYPE.LONG;
        if (fieldType == FieldType.BOOLEAN)
            return COLTYPE.BOOLEAN;
        if ((fieldType == FieldType.DATE) || (fieldType == FieldType.LOCALDATE))
            return COLTYPE.LOCALDATE;
        if (fieldType == FieldType.LOCALDATETIME)
            return COLTYPE.LOCALDATETIME;
        if (fieldType == FieldType.OFFSETDATETIME) 
            return COLTYPE.OFFSETDATETIME;
        return COLTYPE.STRING;
    }
    /**
     * Unzip the BDM zip to get the correct bom.zip
     * 
     * @param bdmZip
     * @return
     * @throws Exception
     */
    private byte[] getBomFromZip(byte[] bdmZip) throws Exception {
        //get the zip file content
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bdmZip));
        //get the zipped file list entry
        ZipEntry ze = zis.getNextEntry();
        byte[] buffer = new byte[1024];

        while (ze != null) {

            String fileName = ze.getName();
            if (fileName.equals("bom.zip")) {
                ByteArrayOutputStream fos = new ByteArrayOutputStream();

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                return fos.toByteArray();
            }
            ze = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();
        throw new Exception("Bad bdm.zip structure: file[bom.zip] is not present");
    }
}
