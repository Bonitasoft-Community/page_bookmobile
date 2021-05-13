package org.bonitasoft.custompage.bookmobile;

import java.util.List;

import org.bonitasoft.custompage.bookmobile.catalog.BdmFactory;
import org.bonitasoft.custompage.bookmobile.catalog.CatalogFactory;
import org.bonitasoft.custompage.bookmobile.catalog.CatalogModel;
import org.bonitasoft.custompage.bookmobile.catalog.CatalogSearchParameter;
import org.bonitasoft.custompage.bookmobile.data.DataRecord;
import org.bonitasoft.custompage.bookmobile.data.DataSearch;
import org.bonitasoft.custompage.bookmobile.data.DataTransformerJson;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;

public class BookMobileAPI {

    protected static BEvent eventUnknowModel = new BEvent(BookMobileAPI.class.getName(), 1, Level.APPLICATIONERROR,
            "Unknow model", "The model given in parameters is unknown",
            "Data will be empty", "Check Exception");
    protected static BEvent eventModelUpdated = new BEvent(BookMobileAPI.class.getName(), 2, Level.SUCCESS,
            "Model updated", "The model is updated");

    public CatalogResult init(BookMobileParameter bookMobileParameter) {

        CatalogResult catalogResult = new CatalogResult();
        CatalogFactory catalogFactory = CatalogFactory.getInstance(bookMobileParameter.apiSession.getTenantId());
        catalogResult.listEvents.addAll(catalogFactory.loadCatalogModel());
        catalogResult.listModels = catalogFactory.getListModel();

        BdmFactory bdmFactory = BdmFactory.getInstance(bookMobileParameter.apiSession.getTenantId());
        catalogResult.listBdmAvailable = bdmFactory.getListBdm(bookMobileParameter.apiSession);
        return catalogResult;
    }

    public CatalogResult getModel(BookMobileParameter bookMobileParameter) {
        CatalogResult catalogResult = new CatalogResult();
        CatalogFactory catalogFactory = CatalogFactory.getInstance(bookMobileParameter.apiSession.getTenantId());
        catalogResult.catalogModel = catalogFactory.findById(bookMobileParameter.id);
        return catalogResult;
    }

    public CatalogResult getBdmDefinition(BookMobileParameter bookMobileParameter) {
        CatalogResult catalogResult = new CatalogResult();
        BdmFactory bdmFactory = BdmFactory.getInstance(bookMobileParameter.apiSession.getTenantId());

        catalogResult.catalogModel = bdmFactory.findByName(bookMobileParameter.name, bookMobileParameter.apiSession);
        return catalogResult;
    }

    public CatalogResult dropModel(BookMobileParameter bookMobileParameter) {

        CatalogResult catalogResult = new CatalogResult();
        CatalogFactory catalogFactory = CatalogFactory.getInstance(bookMobileParameter.apiSession.getTenantId());
        BdmFactory bdmFactory = BdmFactory.getInstance(bookMobileParameter.apiSession.getTenantId());
        catalogResult.listEvents.addAll(catalogFactory.loadCatalogModel());
        // BDM or model has a uniq ID
        CatalogModel catalogModel = catalogFactory.findById(bookMobileParameter.id);
        if (catalogModel != null)
            catalogFactory.removeModel(catalogModel);
        else
            catalogResult.listEvents.add(eventUnknowModel);
        catalogResult.listModels = catalogFactory.getListModel();
        catalogResult.listBdmAvailable = bdmFactory.getListBdm(bookMobileParameter.apiSession);
        return catalogResult;
    }

    /**
     * 
     */

    public CatalogResult populateModel(BookMobileParameter bookMobileParameter) {
        CatalogFactory catalogFactory = CatalogFactory.getInstance(bookMobileParameter.apiSession.getTenantId());
        CatalogModel catalogModel = null;
        if (bookMobileParameter.id == null) {
            catalogModel = catalogFactory.newModel();
        } else {
            catalogModel = catalogFactory.findById(bookMobileParameter.id);
            if (catalogModel == null) {
                catalogModel = catalogFactory.newModel();
            }
        }
        CatalogResult catalogResult = new CatalogResult();

        catalogResult.listEvents.addAll(catalogModel.populateFromTable(bookMobileParameter));
        catalogResult.catalogModel = catalogModel;
        return catalogResult;
    }

    /**
     * @param bookMobileParameter
     * @return
     */
    public CatalogResult updateModel(BookMobileParameter bookMobileParameter) {
        CatalogFactory catalogFactory = CatalogFactory.getInstance(bookMobileParameter.apiSession.getTenantId());
        CatalogModel catalogModel = null;
        if (bookMobileParameter.id == null) {
            catalogModel = catalogFactory.newModel();
        } else {
            catalogModel = catalogFactory.findById(bookMobileParameter.id);
            if (catalogModel == null) {
                catalogModel = catalogFactory.newModel();
            }
        }
        CatalogResult catalogResult = new CatalogResult();
        catalogResult.catalogModel = catalogModel;
        List<BEvent> listEvents = catalogModel.update(bookMobileParameter);
        if (BEventFactory.isError(listEvents)) {
            catalogResult.listEvents.addAll(listEvents);
        } else {
            catalogResult.listEvents.add(eventModelUpdated);
        }
        catalogResult.listModels = catalogFactory.getListModel();
        return catalogResult;
    }

    /**
     * @param bookMobileParameter
     * @return
     */
    public CatalogResult searchDatas(BookMobileParameter bookMobileParameter) {
        CatalogFactory catalogFactory = CatalogFactory.getInstance(bookMobileParameter.apiSession.getTenantId());
        CatalogResult catalogResult = new CatalogResult();
        CatalogModel catalogModel = null;
        if (bookMobileParameter.id == null) {
            catalogResult.listEvents.add(eventUnknowModel);
            return catalogResult;
        }
        catalogModel = catalogFactory.findById(bookMobileParameter.id);
        if (catalogModel == null) {
            catalogResult.listEvents.add(eventUnknowModel);
            return catalogResult;
        }
        catalogResult.catalogModel = catalogModel;
        CatalogSearchParameter searchParameter = new CatalogSearchParameter();
        searchParameter.dataForm = bookMobileParameter.dataForm;
        searchParameter.maxData = bookMobileParameter.maxData;
        catalogResult.catalogModel.dataSearch(searchParameter, new DataTransformerJson(), catalogResult);
        return catalogResult;
    }

    public CatalogResult addData(BookMobileParameter bookMobileParameter) {
        CatalogFactory catalogFactory = CatalogFactory.getInstance(bookMobileParameter.apiSession.getTenantId());
        CatalogResult catalogResult = new CatalogResult();
        CatalogModel catalogModel = null;
        if (bookMobileParameter.id == null) {
            catalogResult.listEvents.add(eventUnknowModel);
            return catalogResult;
        }
        catalogModel = catalogFactory.findById(bookMobileParameter.id);
        if (catalogModel == null) {
            catalogResult.listEvents.add(eventUnknowModel);
            return catalogResult;
        }
        DataRecord dataRecord = DataRecord.getInstance(bookMobileParameter.data);
        catalogResult.listEvents.addAll(catalogModel.dataInsert(dataRecord));
        return catalogResult;

    }

    public CatalogResult updateData(BookMobileParameter bookMobileParameter) {
        CatalogFactory catalogFactory = CatalogFactory.getInstance(bookMobileParameter.apiSession.getTenantId());
        CatalogResult catalogResult = new CatalogResult();
        CatalogModel catalogModel = null;
        if (bookMobileParameter.id == null) {
            catalogResult.listEvents.add(eventUnknowModel);
            return catalogResult;
        }
        catalogModel = catalogFactory.findById(bookMobileParameter.id);
        if (catalogModel == null) {
            catalogResult.listEvents.add(eventUnknowModel);
            return catalogResult;
        }
        DataRecord dataRecord = DataRecord.getInstance(bookMobileParameter.data);
        catalogResult.listEvents.addAll(catalogModel.dataUpdate(dataRecord));
        return catalogResult;

    }
}
