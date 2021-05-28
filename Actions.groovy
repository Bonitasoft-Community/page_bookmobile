
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.Runtime;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;


import org.bonitasoft.web.extension.page.PageContext;
import org.bonitasoft.web.extension.page.PageController;
import org.bonitasoft.web.extension.page.PageResourceProvider;

import org.bonitasoft.engine.api.BusinessDataAPI;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstance;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.command.CommandCriterion;
import org.bonitasoft.engine.command.CommandDescriptor;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.session.APISession;


import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.log.event.BEvent.Level;


import org.bonitasoft.properties.BonitaProperties;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.service.TenantServiceSingleton;

import org.bonitasoft.custompage.bookmobile.BookMobileAPI;
import org.bonitasoft.custompage.bookmobile.BookMobileParameter;
import org.bonitasoft.custompage.bookmobile.tool.CastData;

public class Actions {

    private static Logger logger= Logger.getLogger("org.bonitasoft.custompage.bookmobile.groovy");
    
    
    private static EVENT_USERS_FOUND = new BEvent("org.bonitasoft.custompage.bookmobile", 1, Level.INFO, "Number of users found in the system", "", "", "");

    
      // 2018-03-08T00:19:15.04Z
    public final static SimpleDateFormat sdfJson = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    public final static SimpleDateFormat sdfHuman = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* doAction */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public static Index.ActionAnswer doAction(HttpServletRequest request, String paramJsonSt, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
                
        // logger.info("#### PingActions:Actions start");
        Index.ActionAnswer actionAnswer = new Index.ActionAnswer(); 
        List<BEvent> listEvents=new ArrayList<BEvent>();
        Object jsonParam = (paramJsonSt==null ? null : JSONValue.parse(paramJsonSt));
          
        try {
            String action=request.getParameter("action");
            logger.info("#### log:Actions  action is["+action+"] !");
            if (action==null || action.length()==0 )
            {
                actionAnswer.isManaged=false;
                logger.info("#### log:Actions END No Actions");
                return actionAnswer;
            }
            actionAnswer.isManaged=true;
            actionAnswer.isResponseMap=true; // default
            
            // Hello
            APISession apiSession = pageContext.getApiSession();
            HttpSession httpSession = request.getSession();            
            ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(apiSession);
            IdentityAPI identityAPI = TenantAPIAccessor.getIdentityAPI(apiSession);
			
            long tenantId = apiSession.getTenantId();          
            TenantServiceAccessor tenantServiceAccessor = TenantServiceSingleton.getInstance(tenantId);             
            //Make sure no action is executed if the CSRF protection is active and the request header is invalid
            if (! ("exportData".equals(action) || "importData".equals(action))) {
                if (! TokenValidator.checkCSRFToken(request, response)) {
                    logger.severe("#### log:Actions  Token Validator failed on action["+action+"] !");
                    actionAnswer.isResponseMap=false;
                    return actionAnswer;
                }
            }
            
            
            BookMobileAPI bookMobileAPI = new BookMobileAPI();
           	
			if ("init".equals(action)) {
                BookMobileParameter bookMobileParameter = BookMobileParameter.getInstanceFromApiSession( apiSession );
                actionAnswer.responseMap.putAll( bookMobileAPI.init( bookMobileParameter ).getMap());
			}
            else if ("getModel".equals(action)) {
                BookMobileParameter bookMobileParameter = BookMobileParameter.getInstanceFromApiSession( apiSession );
                bookMobileParameter.id = Long.parseLong( request.getParameter("id"));
                actionAnswer.responseMap.putAll( bookMobileAPI.getModel( bookMobileParameter ).getMap());
            }
            else if ("getBdmDefinition".equals(action)) {
                BookMobileParameter bookMobileParameter = BookMobileParameter.getInstanceFromApiSession( apiSession );
                bookMobileParameter.name = request.getParameter("name");
                actionAnswer.responseMap.putAll( bookMobileAPI.getBdmDefinition( bookMobileParameter ).getMap());
            }
            else if ("dropmodel".equals(action)) {
                BookMobileParameter bookMobileParameter = BookMobileParameter.getInstanceFromApiSession( apiSession );
                bookMobileParameter.id   = CastData.getLong( request.getParameter("id"),null);
                bookMobileParameter.name = request.getParameter("name");
                bookMobileParameter.type = request.getParameter("type");
                actionAnswer.responseMap.putAll( bookMobileAPI.dropModel( bookMobileParameter ).getMap());
            }
			
            else if ("populateFromTable".equals(action)) {
                BookMobileParameter bookMobileParameter = BookMobileParameter.getInstanceFromJsonSt( paramJsonSt, apiSession );
                actionAnswer.responseMap.putAll( bookMobileAPI.populateModel(bookMobileParameter ).getMap());
            }
            else if ("updateModel".equals(action)) {
                String accumulateJson = (String) httpSession.getAttribute("accumulate" );
                logger.info("#### log: action[updateModel] json="+accumulateJson);
                
                BookMobileParameter bookMobileParameter = BookMobileParameter.getInstanceFromJsonSt( accumulateJson, apiSession );
                actionAnswer.responseMap.putAll( bookMobileAPI.updateModel(bookMobileParameter ).getMap());
            }
            else if ("searchDatas".equals(action)) {
                String accumulateJson = (String) httpSession.getAttribute("accumulate" );
                logger.info("#### log: action[searchDatas] json="+accumulateJson);
                
                BookMobileParameter bookMobileParameter = BookMobileParameter.getInstanceFromJsonSt( accumulateJson, apiSession );
                actionAnswer.responseMap.putAll( bookMobileAPI.searchDatas( bookMobileParameter ).getMap());
            }
            else if ("addData".equals(action)) {
                    String accumulateJson = (String) httpSession.getAttribute("accumulate" );
                    logger.info("#### log: action[addData] json="+accumulateJson);
                    
                    BookMobileParameter bookMobileParameter = BookMobileParameter.getInstanceFromJsonSt( accumulateJson, apiSession );
                    actionAnswer.responseMap.putAll( bookMobileAPI.addData( bookMobileParameter ).getMap());
            }
            else if ("updateData".equals(action)) {
                String accumulateJson = (String) httpSession.getAttribute("accumulate" );
                logger.info("#### log: action[updateData] json="+accumulateJson);
                
                BookMobileParameter bookMobileParameter = BookMobileParameter.getInstanceFromJsonSt( accumulateJson, apiSession );
                actionAnswer.responseMap.putAll( bookMobileAPI.updateData( bookMobileParameter ).getMap());
            }
            else if ("deleteData".equals(action)) {
                
                String accumulateJson = (String) httpSession.getAttribute("accumulate" );
                logger.info("#### log: action[deleteData] json="+accumulateJson);
                
                BookMobileParameter bookMobileParameter = BookMobileParameter.getInstanceFromJsonSt( accumulateJson, apiSession );
                actionAnswer.responseMap.putAll( bookMobileAPI.deleteData( bookMobileParameter ).getMap());
            }
            else if ("exportData".equals(action)) {
                
                logger.info("#### log: action[exportData]");
                response.addHeader("content-disposition", "attachment; filename=Export.csv");
                response.addHeader("content-type", "application/CSV");
                OutputStream output = response.getOutputStream();

                BookMobileParameter bookMobileParameter = BookMobileParameter.getInstanceFromApiSession( apiSession );
                bookMobileParameter.id = CastData.getLong( request.getParameter( "id" ),null );
                actionAnswer.responseMap.putAll( bookMobileAPI.exportData( bookMobileParameter, output ).getMap());
          
                output.flush();
                output.close();
                actionAnswer.isResponseMap=false;
                return actionAnswer;

            }
            else if ("importData".equals(action)) {
                logger.info("#### log: action[importData]");

                BookMobileParameter bookMobileParameter = BookMobileParameter.getInstanceFromJsonSt( paramJsonSt, apiSession );
                actionAnswer.responseMap.putAll( bookMobileAPI.importData( bookMobileParameter, pageResourceProvider.getPageDirectory() ).getMap());
            
            }
                   
		     
            
            else if ("collectReset".equals(action)) {
                httpSession.setAttribute("accumulate", "" );
                String paramJsonPartial = request.getParameter("paramjsonpartial");
                logger.info("collectReset paramJsonPartial=["+paramJsonPartial+"]");

                httpSession.setAttribute("accumulate", paramJsonPartial);
                actionAnswer.responseMap.put("status", "ok");
            }
            else if ("collectAdd".equals(action)) {
                String paramJsonPartial = request.getParameter("paramjsonpartial");

                String accumulateJson = (String) httpSession.getAttribute("accumulate" );
                logger.info("collect_add paramJsonPartial=["+paramJsonPartial+"] accumulateJson=["+accumulateJson+"]");
                accumulateJson+=paramJsonPartial;
                httpSession.setAttribute("accumulate", accumulateJson );
                actionAnswer.responseMap.put("status", "ok");
            }

                
            logger.info("#### log:Actions END responseMap ="+actionAnswer.responseMap.size());
            return actionAnswer;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();
            logger.severe("#### log:Groovy Exception ["+e.toString()+"] at "+exceptionDetails);
            actionAnswer.isResponseMap=true;
            actionAnswer.responseMap.put("Error", "log:Groovy Exception ["+e.toString()+"] at "+exceptionDetails);
            
            return actionAnswer;
        }
    }

    
}
