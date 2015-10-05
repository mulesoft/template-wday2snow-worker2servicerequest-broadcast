/**
 * Mule Anypoint Template
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.integration;

import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.context.notification.NotificationException;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.templates.utils.Employee;

import com.mulesoft.module.batch.BatchTestHelper;
import com.workday.hr.EmployeeGetType;
import com.workday.hr.EmployeeReferenceType;
import com.workday.hr.ExternalIntegrationIDReferenceDataType;
import com.workday.hr.IDType;
import com.workday.staffing.EventClassificationSubcategoryObjectIDType;
import com.workday.staffing.EventClassificationSubcategoryObjectType;
import com.workday.staffing.TerminateEmployeeDataType;
import com.workday.staffing.TerminateEmployeeRequestType;
import com.workday.staffing.TerminateEventDataType;

/**
 * The objective of this class is to validate the correct behavior of the flows
 * for this Anypoint Template that make calls to external systems.
 */
public class BusinessLogicIT extends AbstractTemplateTestCase {

	private static final String TEMPLATE_PREFIX = "wday2snow-worker-broadcast";
	private static final int WDAY_WAIT_TIME = 10000;
	private static final long TIMEOUT_SECS = 600;
	private static final long DELAY_MILLIS = 500;
	private static final Logger LOGGER = LogManager.getLogger(BusinessLogicIT.class);
	protected static final String PATH_TO_TEST_PROPERTIES = "./src/test/resources/mule.test.properties";
	private static String PC_MODEL;
	private static String DESK_MODEL;
	private static String DESK_ASSIGNED_TO;
	private static String WDAY_EXT_ID;
	private static String WDAY_TERMINATION_ID;
	private static Date startingDate;

	private BatchTestHelper helper;
    private String EXT_ID, EMAIL = "bwillis@gmailtest.com";
	private Employee employee;
    private List<String> snowReqIds = new ArrayList<String>();
    
    private SubflowInterceptingChainLifecycleWrapper getSnowReqItemsSubflow;
    private SubflowInterceptingChainLifecycleWrapper hireEmployeeSubflow;
    private SubflowInterceptingChainLifecycleWrapper getSnowRequestsSubflow;
    private SubflowInterceptingChainLifecycleWrapper deleteRequestsSubflow; 
    private SubflowInterceptingChainLifecycleWrapper deleteReqItemsSubflow;
    private SubflowInterceptingChainLifecycleWrapper getWorkdayToTerminateSubflow;
    private SubflowInterceptingChainLifecycleWrapper terminateWorkdayEmployeeSubflow;
	
    
    @BeforeClass
    public static void beforeTestClass() {
        System.setProperty("poll.startDelayMillis", "20000");
        System.setProperty("poll.frequencyMillis", "60000");
        
        final Properties props = new Properties();
    	try {
    	props.load(new FileInputStream(PATH_TO_TEST_PROPERTIES));
    	} catch (Exception e) {
    		LOGGER.error("Error occured while reading mule.test.properties" + e);
    	} 
    	
    	WDAY_TERMINATION_ID = props.getProperty("wday.termination.id");
    	WDAY_EXT_ID = props.getProperty("wday.ext.id");
    	DESK_ASSIGNED_TO = props.getProperty("snow.desk.assignedTo");
    	PC_MODEL = props.getProperty("snow.pc.model");
    	DESK_MODEL = props.getProperty("snow.desk.model");
    	
    	Calendar cal = Calendar.getInstance();
    	cal.add(Calendar.MINUTE, -3);
    	startingDate = cal.getTime();   	
        System.setProperty(
        		"watermark.default.expression", 
        		"#[groovy: new GregorianCalendar("
        				+ cal.get(Calendar.YEAR) + ","
        				+ cal.get(Calendar.MONTH) + ","
        				+ cal.get(Calendar.DAY_OF_MONTH) + ","
        				+ cal.get(Calendar.HOUR_OF_DAY) + ","
        				+ cal.get(Calendar.MINUTE) + ","
        				+ cal.get(Calendar.SECOND) + ") ]");
    }

    @Before
    public void setUp() throws Exception {
    	helper = new BatchTestHelper(muleContext);
		stopFlowSchedulers(POLL_FLOW_NAME);
		registerListeners();
		initializeSubFlows();
		createTestDataInSandBox();
    }

	@After
    public void tearDown() throws MuleException, Exception {
    	deleteTestDataFromSandBox();
    }
    
	@SuppressWarnings("unchecked")
	@Test
    public void testMainFlow() throws Exception {
		Thread.sleep(WDAY_WAIT_TIME);
		runSchedulersOnce(POLL_FLOW_NAME);
		waitForPollToRun();
		helper.awaitJobTermination(TIMEOUT_SECS * 1000, DELAY_MILLIS);
		helper.assertJobWasSuccessful();	
		
		// get requests from ServiceNow
		MuleEvent response = getSnowRequestsSubflow.process(getTestEvent(DESK_ASSIGNED_TO, MessageExchangePattern.REQUEST_RESPONSE));
		
		List<Map<String, String>> snowRes = (List<Map<String, String>>) response.getMessage().getPayload();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		int count = 0;
		for (Map<String, String> request : snowRes){
			Date requestOpenedAtDate = sdf.parse(request.get("opened_at"));
			
			if (startingDate.before(requestOpenedAtDate)){
				count++;
				snowReqIds.add(request.get("sys_id"));
				// get request items
				List<Map<String, String>> reqItems = getReqItem(request.get("sys_id"));
				Assert.assertTrue("There should be 1 request item in request in ServiceNow and there is " + reqItems.size() + ".", reqItems.size() == 1);
				for (Map<String, String> reqItem  : reqItems){
					Assert.assertTrue("There should be correct catalogue item set.", reqItem.get("cat_item").equals(PC_MODEL) || reqItem.get("cat_item").equals(DESK_MODEL));
				}
			}
		}
		Assert.assertTrue("There should be two service requests in ServiceNow, but there are " + count + ".", count == 2);		
    }
	
	private void createTestDataInSandBox() throws MuleException, Exception {
		LOGGER.info("creating a workday employee...");
		try {
			hireEmployeeSubflow.process(getTestEvent(prepareNewHire(), MessageExchangePattern.REQUEST_RESPONSE));						
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private Employee prepareNewHire(){
		EXT_ID = TEMPLATE_PREFIX + System.currentTimeMillis();
		LOGGER.info("employee name: " + EXT_ID);
		employee = new Employee(EXT_ID, "Willis1", EMAIL, "650-232-2323", "999 Main St", "San Francisco", "CA", "94105", "US", "o7aHYfwG", 
				new Date(), new Date(), "QA Engineer", "San_Francisco_site", "Regular", "Full Time", "Salary", "USD", "140000", "Annual", "39905", "21440", EXT_ID);
		return employee;
	}
    
	@SuppressWarnings("unchecked")
	private List<Map<String, String>> getReqItem(String parentId) throws MuleException, Exception{
		MuleEvent response = getSnowReqItemsSubflow.process(getTestEvent(parentId, MessageExchangePattern.REQUEST_RESPONSE));
		return (List<Map<String, String>>) response.getMessage().getPayload();
    }
    
	private void deleteTestDataFromSandBox() throws MuleException, Exception {
    	LOGGER.info("Deleting test data...");
		
		for (String id : snowReqIds){			
			deleteReqItemsSubflow.process(getTestEvent(id, MessageExchangePattern.REQUEST_RESPONSE));			
			deleteRequestsSubflow.process(getTestEvent(id, MessageExchangePattern.REQUEST_RESPONSE));					
		}
				
    	// Terminate the created users in Workday
		try {
			MuleEvent response = getWorkdayToTerminateSubflow.process(getTestEvent(getEmployee(), MessageExchangePattern.REQUEST_RESPONSE));			
			terminateWorkdayEmployeeSubflow.process(getTestEvent(prepareTerminate(response), MessageExchangePattern.REQUEST_RESPONSE));								
		} catch (Exception e) {
			e.printStackTrace();
		}		
		LOGGER.info("Deleting test data finished...");
	}
    
    private EmployeeGetType getEmployee(){
		EmployeeGetType get = new EmployeeGetType();
		EmployeeReferenceType empRef = new EmployeeReferenceType();					
		ExternalIntegrationIDReferenceDataType value = new ExternalIntegrationIDReferenceDataType();
		IDType idType = new IDType();
		value.setID(idType);
		// use an existing external ID just for matching purpose
		idType.setSystemID(WDAY_EXT_ID);
		idType.setValue(EXT_ID);			
		empRef.setIntegrationIDReference(value);
		get.setEmployeeReference(empRef);		
		return get;
	}
	
	private TerminateEmployeeRequestType prepareTerminate(MuleEvent response) throws DatatypeConfigurationException{
		TerminateEmployeeRequestType req = (TerminateEmployeeRequestType) response.getMessage().getPayload();
		TerminateEmployeeDataType eeData = req.getTerminateEmployeeData();		
		TerminateEventDataType event = new TerminateEventDataType();
		eeData.setTerminationDate(new GregorianCalendar());
		EventClassificationSubcategoryObjectType prim = new EventClassificationSubcategoryObjectType();
		List<EventClassificationSubcategoryObjectIDType> list = new ArrayList<EventClassificationSubcategoryObjectIDType>();
		EventClassificationSubcategoryObjectIDType id = new EventClassificationSubcategoryObjectIDType();
		id.setType("WID");
		id.setValue(WDAY_TERMINATION_ID);
		list.add(id);
		prim.setID(list);
		event.setPrimaryReasonReference(prim);
		eeData.setTerminateEventData(event );
		return req;		
	}
	
	private void initializeSubFlows() throws InitialisationException {
		hireEmployeeSubflow = getSubFlow("hireEmployee");
		hireEmployeeSubflow.initialise();
    	
		getSnowReqItemsSubflow = getSubFlow("getSnowReqItems");
		getSnowReqItemsSubflow.initialise();
    	
    	getSnowRequestsSubflow = getSubFlow("getSnowRequests");
		getSnowRequestsSubflow.initialise();
		
		deleteRequestsSubflow = getSubFlow("deleteRequests");
		deleteRequestsSubflow.initialise();
		
		deleteReqItemsSubflow = getSubFlow("deleteReqItems");
		deleteReqItemsSubflow.initialise();
		
		getWorkdayToTerminateSubflow = getSubFlow("getWorkdaytoTerminateFlow");
		getWorkdayToTerminateSubflow.initialise();

		terminateWorkdayEmployeeSubflow = getSubFlow("terminateWorkdayEmployee");
		terminateWorkdayEmployeeSubflow.initialise();
	}
	
	private void registerListeners() throws NotificationException {
			muleContext.registerListener(pipelineListener);
	}
}
