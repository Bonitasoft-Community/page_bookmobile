package org.bonitasoft.custompage.bookmobile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.custompage.bookmobile.catalog.BdmFactory;
import org.bonitasoft.custompage.bookmobile.catalog.CatalogFactory;
import org.bonitasoft.custompage.bookmobile.catalog.CatalogModel;
import org.bonitasoft.custompage.bookmobile.catalog.CatalogSearchParameter;
import org.bonitasoft.custompage.bookmobile.data.DataRecord;
import org.bonitasoft.custompage.bookmobile.data.DataSearch;
import org.bonitasoft.custompage.bookmobile.data.DataTransformerJson;
import org.bonitasoft.custompage.bookmobile.database.TableModel.DataColumn;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;

public class BookMobileAPI {

    private static Logger logger = Logger.getLogger(BookMobileAPI.class.getName());

    protected static BEvent eventUnknowModel = new BEvent(BookMobileAPI.class.getName(), 1, Level.APPLICATIONERROR,
            "Unknow model", "The model given in parameters is unknown",
            "Data will be empty", "Check Exception");
    protected static BEvent eventModelUpdated = new BEvent(BookMobileAPI.class.getName(), 2, Level.SUCCESS,
            "Model updated", "The model is updated");
    private static BEvent EventPageDirectoryImportFailed = new BEvent(BookMobileAPI.class.getName(), 3, Level.ERROR, "Export failed", "The export failed", "The zip file is not delivered", "check the exception");

    protected static BEvent eventDataImported = new BEvent(BookMobileAPI.class.getName(), 4, Level.SUCCESS,
            "Data imported", "Number of data imported with success");

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

    /**
     * @param bookMobileParameter
     * @return
     */
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

    /**
     * @param bookMobileParameter
     * @return
     */
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

    /**
     * @param bookMobileParameter
     * @return
     */
    public CatalogResult deleteData(BookMobileParameter bookMobileParameter) {
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

        catalogResult.listEvents.addAll(catalogModel.deleteData(dataRecord));

        return catalogResult;
    }

    /**
     * @param bookMobileParameter
     * @param output
     * @return
     */
    public CatalogResult exportData(BookMobileParameter bookMobileParameter, OutputStream output) {

        Writer writerOutputStream = new OutputStreamWriter(output);

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
        searchParameter.maxData = 10000;
        catalogResult.catalogModel.dataSearch(searchParameter, new DataTransformerJson(), catalogResult);

        try {
            // build the CSV from data
            // build the header
            List<DataColumn> cols = catalogResult.catalogModel.getColumns();
            for (int i = 0; i < cols.size(); i++) {
                if (i > 0)
                    writerOutputStream.write(";");
                writerOutputStream.write(cols.get(i).colName);
            }
            writerOutputStream.write("\n");
            for (Map<String, Object> datas : catalogResult.getData()) {
                for (int i = 0; i < cols.size(); i++) {
                    if (i > 0)
                        writerOutputStream.write(";");
                    Object data = datas.get(cols.get(i).colName);
                    writerOutputStream.write(data == null ? "" : data.toString());
                }
                writerOutputStream.write("\n");
            }
            writerOutputStream.flush();

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();

            logger.severe("BookMobileAPI.exportData: Exception " + e.getMessage() + " at " + exceptionDetails);
        }
        return catalogResult;
    }

    /**
     * @param bookMobileParameter
     * @param pageDirectory
     * @return
     */
    public CatalogResult importData(BookMobileParameter bookMobileParameter, File pageDirectory) {
        File completefileName = null;
        String allPathChecked = "";
        CatalogResult catalogResult = new CatalogResult();

        List<String> listParentTmpFile = new ArrayList<>();
        try {
            listParentTmpFile.add(pageDirectory.getCanonicalPath() + "/../../../tmp/");
            listParentTmpFile.add(pageDirectory.getCanonicalPath() + "/../../");
        } catch (Exception e) {
            catalogResult.listEvents.add(new BEvent(EventPageDirectoryImportFailed, e, ""));
            logger.severe("BookMobile : error get CanonicalPath of pageDirectory[" + e.toString() + "]");
            return catalogResult;
        }

        for (String pathTemp : listParentTmpFile) {
            allPathChecked += pathTemp + bookMobileParameter.fileName + ";";
            if (bookMobileParameter.fileName.length() > 0 && (new File(pathTemp + bookMobileParameter.fileName)).exists()) {
                completefileName = (new File(pathTemp + bookMobileParameter.fileName)).getAbsoluteFile();
                logger.info("BookMobile.importConfs : FOUND [" + completefileName + "]");
            }
        }
        // this is not normal, can't find the file
        if (completefileName == null)
            return catalogResult;

        CatalogFactory catalogFactory = CatalogFactory.getInstance(bookMobileParameter.apiSession.getTenantId());
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

        try {
            BufferedReader br = new BufferedReader(new FileReader(completefileName));
            String line = br.readLine();
            String[] cols = null;
            if (line != null) {
                cols = line.split(";");
                line = br.readLine();
            }
            int nbRecordsLoaded = 0;
            while (line != null) {
                String[] data = line.split(";");
                line = br.readLine();
                if (data.length == 0)
                    continue;
                if (data.length == 1 && data[0].isEmpty())
                    continue;
                // insert the record now
                DataRecord dataRecord = new DataRecord();
                for (int i = 0; i < cols.length; i++) {
                    dataRecord.setData(cols[i], i < data.length ? data[i] : null);
                }
                List<BEvent> listEvents = catalogModel.dataInsert(dataRecord);
                if (BEventFactory.isError(listEvents))
                    catalogResult.listEvents.addAll(listEvents);
                else
                    nbRecordsLoaded++;

            }
            catalogResult.listEvents.add(new BEvent(eventDataImported, nbRecordsLoaded+" records imported"));
        } catch (Exception e) {
            catalogResult.listEvents.add(new BEvent(EventPageDirectoryImportFailed, e, ""));
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();

            logger.severe("BookMobile : error during import " + e.toString() + " at " + exceptionDetails);

        }
        return catalogResult;
    }

}
