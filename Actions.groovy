
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
            
            // Hello
            APISession apiSession = pageContext.getApiSession();
            HttpSession httpSession = request.getSession();            
            ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(apiSession);
            IdentityAPI identityAPI = TenantAPIAccessor.getIdentityAPI(apiSession);
			
            long tenantId = apiSession.getTenantId();          
            TenantServiceAccessor tenantServiceAccessor = TenantServiceSingleton.getInstance(tenantId);             
            //Make sure no action is executed if the CSRF protection is active and the request header is invalid
            if (! TokenValidator.checkCSRFToken(request, response)) {
                logger.severe("#### log:Actions  Token Validator failed on action["+action+"] !");
                actionAnswer.isResponseMap=false;
                return actionAnswer;
            }
 
             BookMobileAPI bookMobileAPI = new BookMobileAPI();
           	
			if ("init".equals(action)) {
                BookMobileParameter bookMobileParameter = BookMobileParameter.getInstanceFromTenantId( tenantId );
                actionAnswer.responseMap.putAll( bookMobileAPI.init( bookMobileParameter ).getMap());
			}
            else if ("getModel".equals(action)) {
                BookMobileParameter bookMobileParameter = BookMobileParameter.getInstanceFromTenantId( tenantId );
                bookMobileParameter.id = Long.parseLong( request.getParameter("id"));
                actionAnswer.responseMap.putAll( bookMobileAPI.getModel( bookMobileParameter ).getMap());
            }
            
            else if ("populateFromTable".equals(action)) {
                BookMobileParameter bookMobileParameter = BookMobileParameter.getInstanceFromJsonSt( paramJsonSt, tenantId );
                actionAnswer.responseMap.putAll( bookMobileAPI.populateModel(bookMobileParameter ).getMap());
            }
            else if ("updateModel".equals(action)) {
                String accumulateJson = (String) httpSession.getAttribute("accumulate" );
                logger.info("#### log: action[updateModel] json="+accumulateJson);
                
                BookMobileParameter bookMobileParameter = BookMobileParameter.getInstanceFromJsonSt( accumulateJson, tenantId );
                actionAnswer.responseMap.putAll( bookMobileAPI.updateModel(bookMobileParameter ).getMap());
            }
            else if ("searchDatas".equals(action)) {
                String accumulateJson = (String) httpSession.getAttribute("accumulate" );
                logger.info("#### log: action[searchDatas] json="+accumulateJson);
                
                BookMobileParameter bookMobileParameter = BookMobileParameter.getInstanceFromJsonSt( accumulateJson, tenantId );
                actionAnswer.responseMap.putAll( bookMobileAPI.searchDatas( bookMobileParameter ).getMap());
            }
            else if ("addData".equals(action)) {
                    String accumulateJson = (String) httpSession.getAttribute("accumulate" );
                    logger.info("#### log: action[addData] json="+accumulateJson);
                    
                    BookMobileParameter bookMobileParameter = BookMobileParameter.getInstanceFromJsonSt( accumulateJson, tenantId );
                    actionAnswer.responseMap.putAll( bookMobileAPI.addData( bookMobileParameter ).getMap());
            }
            else if ("updateData".equals(action)) {
                String accumulateJson = (String) httpSession.getAttribute("accumulate" );
                logger.info("#### log: action[updateData] json="+accumulateJson);
                
                BookMobileParameter bookMobileParameter = BookMobileParameter.getInstanceFromJsonSt( accumulateJson, tenantId );
                actionAnswer.responseMap.putAll( bookMobileAPI.updateData( bookMobileParameter ).getMap());
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

    /**
		to create a simple chart
		*/
		public static class ActivityTimeLine
		{
				public String activityName;
				public Date dateBegin;
				public Date dateEnd;
				
				public static ActivityTimeLine getActivityTimeLine(String activityName, int timeBegin, int timeEnd)
				{
					Calendar calBegin = Calendar.getInstance();
					calBegin.set(Calendar.HOUR_OF_DAY , timeBegin);
					Calendar calEnd = Calendar.getInstance();
					calEnd.set(Calendar.HOUR_OF_DAY , timeEnd);
					
						ActivityTimeLine oneSample = new ActivityTimeLine();
						oneSample.activityName = activityName;
						oneSample.dateBegin		= calBegin.getTime();
						oneSample.dateEnd 		= calEnd.getTime();
						
						return oneSample;
				}
				public long getDateLong()
				{ return dateBegin == null ? 0 : dateBegin.getTime(); }
		}
		
		
		/** create a simple chart 
		*/
		public static String getChartTimeLine(String title, List<ActivityTimeLine> listSamples){
				Logger logger = Logger.getLogger("org.bonitasoft");
				
				/** structure 
				 * "rows": [
           {
        		 c: [
        		      { "v": "January" },"
                  { "v": 19,"f": "42 items" },
                  { "v": 12,"f": "Ony 12 items" },
                ]
           },
           {
        		 c: [
        		      { "v": "January" },"
                  { "v": 19,"f": "42 items" },
                  { "v": 12,"f": "Ony 12 items" },
                ]
           },

				 */
				String resultValue="";
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy,MM,dd,HH,mm,ss,SSS");
				
				for (int i=0;i<listSamples.size();i++)
				{
					logger.info("sample [i] : "+listSamples.get( i ).activityName+"] dateBegin["+simpleDateFormat.format( listSamples.get( i ).dateBegin)+"] dateEnd["+simpleDateFormat.format( listSamples.get( i ).dateEnd) +"]");
						if (listSamples.get( i ).dateBegin!=null &&  listSamples.get( i ).dateEnd != null)
								resultValue+= "{ \"c\": [ { \"v\": \""+listSamples.get( i ).activityName+"\" }," ;
								resultValue+= " { \"v\": \""+listSamples.get( i ).activityName +"\" }, " ;
								resultValue+= " { \"v\": \"Date("+ simpleDateFormat.format( listSamples.get( i ).dateBegin) +")\" }, " ;
								resultValue+= " { \"v\": \"Date("+ simpleDateFormat.format( listSamples.get( i ).dateEnd) +")\" } " ;
								resultValue+= "] },";
				}
				if (resultValue.length()>0)
						resultValue = resultValue.substring(0,resultValue.length()-1);
				
				String resultLabel = "{ \"type\": \"string\", \"id\": \"Role\" },{ \"type\": \"string\", \"id\": \"Name\"},{ \"type\": \"datetime\", \"id\": \"Start\"},{ \"type\": \"datetime\", \"id\": \"End\"}";
				
				String valueChart = "	{"
					   valueChart += "\"type\": \"Timeline\", ";
					  valueChart += "\"displayed\": true, ";
					  valueChart += "\"data\": {";
					  valueChart +=   "\"cols\": ["+resultLabel+"], ";
					  valueChart +=   "\"rows\": ["+resultValue+"] ";
					  /*
					  +   "\"options\": { "
					  +         "\"bars\": \"horizontal\","
					  +         "\"title\": \""+title+"\", \"fill\": 20, \"displayExactValues\": true,"
					  +         "\"vAxis\": { \"title\": \"ms\", \"gridlines\": { \"count\": 100 } }"
					  */
					  valueChart +=  "}";
					  valueChart +="}";
// 				+"\"isStacked\": \"true\","
 	          
//		    +"\"displayExactValues\": true,"
//		    
//		    +"\"hAxis\": { \"title\": \"Date\" }"
//		    +"},"
				logger.info("Value1 >"+valueChart+"<");

				
				return valueChart;		
		}
    
		/**
		 public static void testProcessStartedUserName( APISession apiSession ) {
		     SearchRelationAPI searchRelation = new SearchRelationAPI( apiSession );
                
		     -- SearchOptionsBuilderRelation sob = new SearchOptionsBuilderRelation( 0,10);
		        sob.filter(org.bonitasoft.search.UserSearchDescriptorRelation.USER_NAME, "Walter.Bates");
		        sob.relation(org.bonitasoft.search.ProcessInstanceSearchDescriptorRelation.STARTED_BY);
		       --
		        SearchOptionsBuilderRelation sob = new SearchOptionsBuilderRelation(0,10);
		        sob.filter(org.bonitasoft.search.ContactDataSearchDescriptorRelation.EMAIL, "Walter.Bates@bonitasoft.com");
		        sob.filter(org.bonitasoft.search.ContactDataSearchDescriptorRelation.PERSONAL, Boolean.FALSE);
		        
		        // we don't have the filterRelation. So, here we give this kind of relation, saying "Please link the second table by the STARTEDBY
		        sob.relation(org.bonitasoft.search.ProcessInstanceSearchDescriptorRelation.STARTED_BY);
                sob.relation(org.bonitasoft.search.ProcessInstanceSearchDescriptorRelation.PROCESS_DEFINITION_ID);
                sob.relation(org.bonitasoft.search.ContactDataSearchDescriptorRelation.USERID);
		        sob.filter(org.bonitasoft.search.ProcessDeploymentInfoSearchDescriptorRelation.NAME, "TestProcess");
		        try {
		            searchRelation.search(ProcessInstance.class, sob.done());
		        } catch (Exception e) {
		            logger.severe("testProcessStartedUserName >"+e.getMessage()+"<");

		        }
		        
		    }
		    */
}
