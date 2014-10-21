package org.mule.templates;

import java.text.ParseException;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.servicenow.screquest.GetRecords;
import com.workday.hr.EffectiveAndUpdatedDateTimeDataType;
import com.workday.hr.GetWorkersRequestType;
import com.workday.hr.TransactionLogCriteriaType;
import com.workday.hr.WorkerRequestCriteriaType;
import com.workday.hr.WorkerResponseGroupType;

public class WorkersRequest {

	public static GetWorkersRequestType create(Date startDate) throws ParseException, DatatypeConfigurationException {

		/*
		 * Set data range for events
		 */

		EffectiveAndUpdatedDateTimeDataType dateRangeData = new EffectiveAndUpdatedDateTimeDataType();
		dateRangeData.setUpdatedFrom(xmlDate(startDate));
		dateRangeData.setUpdatedThrough(xmlDate(new Date()));
		/*
		 * Set event type criteria filter
		 */
		
		TransactionLogCriteriaType transactionLogCriteria = new TransactionLogCriteriaType();
		transactionLogCriteria.setTransactionDateRangeData(dateRangeData);

		WorkerRequestCriteriaType workerRequestCriteria = new WorkerRequestCriteriaType();
		workerRequestCriteria.getTransactionLogCriteriaData().add(transactionLogCriteria);
		GetWorkersRequestType getWorkersType = new GetWorkersRequestType();
		getWorkersType.setRequestCriteria(workerRequestCriteria);
		WorkerResponseGroupType resGroup = new WorkerResponseGroupType();
		resGroup.setIncludePersonalInformation(true);
		resGroup.setIncludeOrganizations(true);
		resGroup.setIncludeEmploymentInformation(true);
		resGroup.setIncludeTransactionLogData(true);
		resGroup.setIncludeReference(true);
		resGroup.setIncludeRoles(true);
		getWorkersType.setResponseGroup(resGroup);
		return getWorkersType;
	}

	private static XMLGregorianCalendar xmlDate(Date date) throws DatatypeConfigurationException {
		GregorianCalendar gregorianCalendar = (GregorianCalendar) GregorianCalendar.getInstance();
		gregorianCalendar.setTime(date);
		return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
	}
	
	public static com.servicenow.screquest.GetRecords get(){
		GetRecords get = new GetRecords();
		return get ;
	}

}
