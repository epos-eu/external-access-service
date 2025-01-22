package org.epos.core;

import metadataapis.*;
import org.epos.eposdatamodel.*;
import org.epos.handler.dbapi.service.EntityManagerService;

import java.util.List;

import static abstractapis.AbstractAPI.*;

public class DatabaseConnections {

	private List<DataProduct> dataproducts;
	private List<SoftwareApplication> softwareApplications;
	private List<SoftwareSourceCode> softwareSourceCodes;
	private List<SoftwareApplicationParameter> softwareApplicationParameters;
	private List<Organization> organizationList;
	private List<Category> categoryList;
	private List<CategoryScheme> categorySchemesList;
	private List<Distribution> distributionList;
	private List<Operation> operationList;
	private List<WebService> webServiceList;
	private List<Address> addressList;
	private List<Location> locationList;
	private List<PeriodOfTime> periodOfTimeList;
	private List<Identifier> identifierList;
	private List<Mapping> mappingList;
	private List<Equipment> equipmentList;
	private List<Facility> facilityList;

	private DatabaseConnections() {}

	public void syncDatabaseConnections() {
		if(EntityManagerService.getInstance()!=null) EntityManagerService.getInstance().getCache().evictAll();

		List<DataProduct> tempDataproducts  = retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveAll();
		List<SoftwareApplication> tempSoftwareApplications = retrieveAPI(EntityNames.SOFTWAREAPPLICATION.name()).retrieveAll();
		List<SoftwareSourceCode> tempSoftwareSourceCodes = retrieveAPI(EntityNames.SOFTWARESOURCECODE.name()).retrieveAll();
		List<SoftwareApplicationParameter> tempApplicationParameters = retrieveAPI(EntityNames.SOFTWAREAPPLICATIONOUTPUTPARAMETER.name()).retrieveAll();
		List<Organization> tempOrganizationList = retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveAll();
		List<Category> tempCategoryList = retrieveAPI(EntityNames.CATEGORY.name()).retrieveAll();
		List<CategoryScheme> tempCategorySchemeList = retrieveAPI(EntityNames.CATEGORYSCHEME.name()).retrieveAll();
		List<Distribution> tempDistributionList = retrieveAPI(EntityNames.DISTRIBUTION.name()).retrieveAll();
		List<Operation> tempOperationList = retrieveAPI(EntityNames.OPERATION.name()).retrieveAll();
		List<WebService> tempWebServiceList = retrieveAPI(EntityNames.WEBSERVICE.name()).retrieveAll();
		List<Address> tempAddressList = retrieveAPI(EntityNames.ADDRESS.name()).retrieveAll();
		List<Location> tempLocationList = retrieveAPI(EntityNames.LOCATION.name()).retrieveAll();
		List<PeriodOfTime> tempPeriodOfTimeList = retrieveAPI(EntityNames.PERIODOFTIME.name()).retrieveAll();
		List<Identifier> tempIdentifierList = retrieveAPI(EntityNames.IDENTIFIER.name()).retrieveAll();
		List<Mapping> tempMappingList = retrieveAPI(EntityNames.MAPPING.name()).retrieveAll();
		List<Facility> tempFacilityList = retrieveAPI(EntityNames.FACILITY.name()).retrieveAll();
		List<Equipment> tempEquipmentList = retrieveAPI(EntityNames.EQUIPMENT.name()).retrieveAll();

		dataproducts = tempDataproducts;
		softwareApplications = tempSoftwareApplications;
		softwareSourceCodes = tempSoftwareSourceCodes;
		softwareApplicationParameters = tempApplicationParameters;
		organizationList = tempOrganizationList;
		categoryList = tempCategoryList;
		categorySchemesList = tempCategorySchemeList;
		distributionList = tempDistributionList;
		operationList = tempOperationList;
		webServiceList = tempWebServiceList;
		addressList = tempAddressList;
		locationList = tempLocationList;
		periodOfTimeList = tempPeriodOfTimeList;
		identifierList = tempIdentifierList;
		mappingList = tempMappingList;
		facilityList = tempFacilityList;
		equipmentList = tempEquipmentList;
	}

	private static DatabaseConnections connections;

	public static DatabaseConnections getInstance() {
		if(connections==null) connections = new DatabaseConnections();
		return connections;
	}

	public List<DataProduct> getDataproducts() {
		return dataproducts;
	}

	public List<SoftwareApplication> getSoftwareApplications() {
		return softwareApplications;
	}

	public List<SoftwareSourceCode> getSoftwareSourceCodes() {return softwareSourceCodes;}

	public List<SoftwareApplicationParameter> getSoftwareApplicationParameters() {return softwareApplicationParameters;}

	public List<Organization> getOrganizationList() {
		return organizationList;
	}

	public List<Category> getCategoryList() {
		return categoryList;
	}

	public List<CategoryScheme> getCategorySchemesList() {return categorySchemesList;}

	public List<Distribution> getDistributionList() {
		return distributionList;
	}

	public List<Operation> getOperationList() {
		return operationList;
	}

	public List<WebService> getWebServiceList() {
		return webServiceList;
	}

	public List<Address> getAddressList() {
		return addressList;
	}

	public List<Location> getLocationList() {
		return locationList;
	}

	public List<PeriodOfTime> getPeriodOfTimeList() {
		return periodOfTimeList;
	}

	public List<Identifier> getIdentifierList() {
		return identifierList;
	}

	public List<Mapping> getMappingList() { return mappingList;}

	public List<Facility> getFacilityList() { return facilityList;}

	public List<Equipment> getEquipmentList() { return equipmentList;}

}
