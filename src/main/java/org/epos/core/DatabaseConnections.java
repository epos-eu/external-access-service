package org.epos.core;


import commonapis.AddressAPI;
import commonapis.IdentifierAPI;
import commonapis.SpatialAPI;
import commonapis.TemporalAPI;
import metadataapis.*;
import model.Dataproduct;
import model.StatusType;
import org.epos.eposdatamodel.*;

import java.util.List;
import java.util.stream.Collectors;

public class DatabaseConnections {

	private DataProductAPI dataProductAPI = new DataProductAPI(EntityNames.DATAPRODUCT.name(), Dataproduct.class);
	private DistributionAPI distributionAPI = new DistributionAPI(EntityNames.DISTRIBUTION.name(), model.Distribution.class);
	private OrganizationAPI organizationAPI = new OrganizationAPI(EntityNames.ORGANIZATION.name(), model.Organization.class);
	private WebServiceAPI webServiceAPI = new WebServiceAPI(EntityNames.WEBSERVICE.name(), model.Webservice.class);
	private OperationAPI operationAPI = new OperationAPI(EntityNames.OPERATION.name(), model.Operation.class);
	private CategoryAPI categoryAPI = new CategoryAPI(EntityNames.CATEGORY.name(), model.Category.class);
	private SoftwareApplicationAPI softwareApplicationAPI = new SoftwareApplicationAPI(EntityNames.SOFTWAREAPPLICATION.name(), model.Softwareapplication.class);
	private SoftwareSourceCodeAPI softwareSourceCodeAPI = new SoftwareSourceCodeAPI(EntityNames.SOFTWARESOURCECODE.name(), model.Softwaresourcecode.class);
	private AddressAPI addressAPI = new AddressAPI(EntityNames.ADDRESS.name(), model.Address.class);
	private SpatialAPI spatialAPI = new SpatialAPI(EntityNames.LOCATION.name(), model.Spatial.class);
	private TemporalAPI temporalAPI = new TemporalAPI(EntityNames.PERIODOFTIME.name(), model.Temporal.class);
	private IdentifierAPI identifierAPI = new IdentifierAPI(EntityNames.IDENTIFIER.name(), model.Identifier.class);
	private MappingAPI mappingAPI = new MappingAPI(EntityNames.MAPPING.name(), model.Mapping.class);
	private FacilityAPI facilityAPI = new FacilityAPI(EntityNames.FACILITY.name(), model.Mapping.class);
	private EquipmentAPI equipmentAPI = new EquipmentAPI(EntityNames.EQUIPMENT.name(), model.Mapping.class);


	private List<DataProduct> dataproducts;
	private List<SoftwareApplication> softwareApplications;
	private List<SoftwareSourceCode> softwareSourceCodes;
	private List<Organization> organizationList;
	private List<Category> categoryList;
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



	private DatabaseConnections() {

		List tempDataproducts  = dataProductAPI.retrieveAll().stream().filter(item -> item.getStatus().equals(StatusType.PUBLISHED)).collect(Collectors.toList());
		List tempSoftwareApplications = softwareApplicationAPI.retrieveAll().stream().filter(item -> item.getStatus().equals(StatusType.PUBLISHED)).collect(Collectors.toList());
		List tempSoftwareSourceCode = softwareSourceCodeAPI.retrieveAll().stream().filter(item -> item.getStatus().equals(StatusType.PUBLISHED)).collect(Collectors.toList());
		List tempOrganizationList = organizationAPI.retrieveAll().stream().filter(item -> item.getStatus().equals(StatusType.PUBLISHED)).collect(Collectors.toList());
		List tempCategoryList = categoryAPI.retrieveAll().stream().filter(item -> item.getStatus().equals(StatusType.PUBLISHED)).collect(Collectors.toList());
		List tempDistributionList = distributionAPI.retrieveAll().stream().filter(item -> item.getStatus().equals(StatusType.PUBLISHED)).collect(Collectors.toList());
		List tempOperationList = operationAPI.retrieveAll().stream().filter(item -> item.getStatus().equals(StatusType.PUBLISHED)).collect(Collectors.toList());
		List tempWebServiceList = webServiceAPI.retrieveAll().stream().filter(item -> item.getStatus().equals(StatusType.PUBLISHED)).collect(Collectors.toList());
		List tempAddressList = addressAPI.retrieveAll().stream().filter(item -> item.getStatus().equals(StatusType.PUBLISHED)).collect(Collectors.toList());
		List tempLocationList = spatialAPI.retrieveAll().stream().filter(item -> item.getStatus().equals(StatusType.PUBLISHED)).collect(Collectors.toList());
		List tempPeriodOfTimeList = temporalAPI.retrieveAll().stream().filter(item -> item.getStatus().equals(StatusType.PUBLISHED)).collect(Collectors.toList());
		List tempIdentifierList = identifierAPI.retrieveAll().stream().filter(item -> item.getStatus().equals(StatusType.PUBLISHED)).collect(Collectors.toList());
		List tempMappingList = mappingAPI.retrieveAll().stream().filter(item -> item.getStatus().equals(StatusType.PUBLISHED)).collect(Collectors.toList());
		List tempFacilityList = facilityAPI.retrieveAll().stream().filter(item -> item.getStatus().equals(StatusType.PUBLISHED)).collect(Collectors.toList());
		List tempEquipmentList = equipmentAPI.retrieveAll().stream().filter(item -> item.getStatus().equals(StatusType.PUBLISHED)).collect(Collectors.toList());

		dataproducts = tempDataproducts;
		softwareApplications = tempSoftwareApplications;
		softwareSourceCodes = tempSoftwareSourceCode;
		organizationList = tempOrganizationList;
		categoryList = tempCategoryList;
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

	public List<SoftwareSourceCode> getSoftwareSourceCodes() {
		return softwareSourceCodes;
	}

	public List<Organization> getOrganizationList() {
		return organizationList;
	}

	public List<Category> getCategoryList() {
		return categoryList;
	}

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
