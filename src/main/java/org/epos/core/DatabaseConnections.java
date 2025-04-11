package org.epos.core;

import static abstractapis.AbstractAPI.retrieveAPI;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.epos.eposdatamodel.Address;
import org.epos.eposdatamodel.Category;
import org.epos.eposdatamodel.CategoryScheme;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.Equipment;
import org.epos.eposdatamodel.Facility;
import org.epos.eposdatamodel.Identifier;
import org.epos.eposdatamodel.Location;
import org.epos.eposdatamodel.Mapping;
import org.epos.eposdatamodel.Operation;
import org.epos.eposdatamodel.Organization;
import org.epos.eposdatamodel.PeriodOfTime;
import org.epos.eposdatamodel.SoftwareApplication;
import org.epos.eposdatamodel.SoftwareApplicationParameter;
import org.epos.eposdatamodel.SoftwareSourceCode;
import org.epos.eposdatamodel.WebService;
import org.epos.handler.dbapi.service.EntityManagerService;

import metadataapis.EntityNames;

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

	// Lock for thread-safety
	private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	// Maximum number of concurrent connections (can be adjusted as needed)
	private int maxDbConnections = 17;

	private DatabaseConnections() {
	}

	public void syncDatabaseConnections() {
		if (EntityManagerService.getInstance() != null) {
			EntityManagerService.getInstance().getCache().evictAll();
		}

		ExecutorService executor = Executors.newFixedThreadPool(maxDbConnections);

		// Submit each API query as a separate task
		CompletableFuture<List<DataProduct>> tempDataproductsFuture = CompletableFuture
				.supplyAsync(() -> retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveAll(), executor);

		CompletableFuture<List<SoftwareApplication>> tempSoftwareApplicationsFuture = CompletableFuture
				.supplyAsync(() -> retrieveAPI(EntityNames.SOFTWAREAPPLICATION.name()).retrieveAll(), executor);

		CompletableFuture<List<SoftwareSourceCode>> tempSoftwareSourceCodesFuture = CompletableFuture
				.supplyAsync(() -> retrieveAPI(EntityNames.SOFTWARESOURCECODE.name()).retrieveAll(), executor);

		CompletableFuture<List<SoftwareApplicationParameter>> tempApplicationParametersFuture = CompletableFuture
				.supplyAsync(() -> retrieveAPI(EntityNames.SOFTWAREAPPLICATIONOUTPUTPARAMETER.name()).retrieveAll(),
						executor);

		CompletableFuture<List<Organization>> tempOrganizationListFuture = CompletableFuture
				.supplyAsync(() -> retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveAll(), executor);

		CompletableFuture<List<Category>> tempCategoryListFuture = CompletableFuture
				.supplyAsync(() -> retrieveAPI(EntityNames.CATEGORY.name()).retrieveAll(), executor);

		CompletableFuture<List<CategoryScheme>> tempCategorySchemeListFuture = CompletableFuture
				.supplyAsync(() -> retrieveAPI(EntityNames.CATEGORYSCHEME.name()).retrieveAll(), executor);

		CompletableFuture<List<Distribution>> tempDistributionListFuture = CompletableFuture
				.supplyAsync(() -> retrieveAPI(EntityNames.DISTRIBUTION.name()).retrieveAll(), executor);

		CompletableFuture<List<Operation>> tempOperationListFuture = CompletableFuture
				.supplyAsync(() -> retrieveAPI(EntityNames.OPERATION.name()).retrieveAll(), executor);

		CompletableFuture<List<WebService>> tempWebServiceListFuture = CompletableFuture
				.supplyAsync(() -> retrieveAPI(EntityNames.WEBSERVICE.name()).retrieveAll(), executor);

		CompletableFuture<List<Address>> tempAddressListFuture = CompletableFuture
				.supplyAsync(() -> retrieveAPI(EntityNames.ADDRESS.name()).retrieveAll(), executor);

		CompletableFuture<List<Location>> tempLocationListFuture = CompletableFuture
				.supplyAsync(() -> retrieveAPI(EntityNames.LOCATION.name()).retrieveAll(), executor);

		CompletableFuture<List<PeriodOfTime>> tempPeriodOfTimeListFuture = CompletableFuture
				.supplyAsync(() -> retrieveAPI(EntityNames.PERIODOFTIME.name()).retrieveAll(), executor);

		CompletableFuture<List<Identifier>> tempIdentifierListFuture = CompletableFuture
				.supplyAsync(() -> retrieveAPI(EntityNames.IDENTIFIER.name()).retrieveAll(), executor);

		CompletableFuture<List<Mapping>> tempMappingListFuture = CompletableFuture
				.supplyAsync(() -> retrieveAPI(EntityNames.MAPPING.name()).retrieveAll(), executor);

		CompletableFuture<List<Facility>> tempFacilityListFuture = CompletableFuture
				.supplyAsync(() -> retrieveAPI(EntityNames.FACILITY.name()).retrieveAll(), executor);

		CompletableFuture<List<Equipment>> tempEquipmentListFuture = CompletableFuture
				.supplyAsync(() -> retrieveAPI(EntityNames.EQUIPMENT.name()).retrieveAll(), executor);

		// Wait for all tasks to complete
		CompletableFuture<Void> allFutures = CompletableFuture.allOf(
				tempDataproductsFuture,
				tempSoftwareApplicationsFuture,
				tempSoftwareSourceCodesFuture,
				tempApplicationParametersFuture,
				tempOrganizationListFuture,
				tempCategoryListFuture,
				tempCategorySchemeListFuture,
				tempDistributionListFuture,
				tempOperationListFuture,
				tempWebServiceListFuture,
				tempAddressListFuture,
				tempLocationListFuture,
				tempPeriodOfTimeListFuture,
				tempIdentifierListFuture,
				tempMappingListFuture,
				tempFacilityListFuture,
				tempEquipmentListFuture);

		allFutures.join();

		// Retrieve the results
		List<DataProduct> tempDataproducts = tempDataproductsFuture.join();
		List<SoftwareApplication> tempSoftwareApplications = tempSoftwareApplicationsFuture.join();
		List<SoftwareSourceCode> tempSoftwareSourceCodes = tempSoftwareSourceCodesFuture.join();
		List<SoftwareApplicationParameter> tempApplicationParameters = tempApplicationParametersFuture.join();
		List<Organization> tempOrganizationList = tempOrganizationListFuture.join();
		List<Category> tempCategoryList = tempCategoryListFuture.join();
		List<CategoryScheme> tempCategorySchemeList = tempCategorySchemeListFuture.join();
		List<Distribution> tempDistributionList = tempDistributionListFuture.join();
		List<Operation> tempOperationList = tempOperationListFuture.join();
		List<WebService> tempWebServiceList = tempWebServiceListFuture.join();
		List<Address> tempAddressList = tempAddressListFuture.join();
		List<Location> tempLocationList = tempLocationListFuture.join();
		List<PeriodOfTime> tempPeriodOfTimeList = tempPeriodOfTimeListFuture.join();
		List<Identifier> tempIdentifierList = tempIdentifierListFuture.join();
		List<Mapping> tempMappingList = tempMappingListFuture.join();
		List<Facility> tempFacilityList = tempFacilityListFuture.join();
		List<Equipment> tempEquipmentList = tempEquipmentListFuture.join();

		// Atomically update the instance fields using a write lock
		lock.writeLock().lock();
		try {
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
		} finally {
			lock.writeLock().unlock();
		}

		executor.shutdown();
	}

	private static DatabaseConnections connections;

	public static DatabaseConnections getInstance() {
		lock.readLock().lock();
		try {
			if (connections == null) {
				connections = new DatabaseConnections();
			}
			return connections;
		} finally {
			lock.readLock().unlock();
		}
	}

	// Getter methods, each guarded by a read lock

	public List<DataProduct> getDataproducts() {
		lock.readLock().lock();
		try {
			return dataproducts;
		} finally {
			lock.readLock().unlock();
		}
	}

	public List<SoftwareApplication> getSoftwareApplications() {
		lock.readLock().lock();
		try {
			return softwareApplications;
		} finally {
			lock.readLock().unlock();
		}
	}

	public List<SoftwareSourceCode> getSoftwareSourceCodes() {
		lock.readLock().lock();
		try {
			return softwareSourceCodes;
		} finally {
			lock.readLock().unlock();
		}
	}

	public List<SoftwareApplicationParameter> getSoftwareApplicationParameters() {
		lock.readLock().lock();
		try {
			return softwareApplicationParameters;
		} finally {
			lock.readLock().unlock();
		}
	}

	public List<Organization> getOrganizationList() {
		lock.readLock().lock();
		try {
			return organizationList;
		} finally {
			lock.readLock().unlock();
		}
	}

	public List<Category> getCategoryList() {
		lock.readLock().lock();
		try {
			return categoryList;
		} finally {
			lock.readLock().unlock();
		}
	}

	public List<CategoryScheme> getCategorySchemesList() {
		lock.readLock().lock();
		try {
			return categorySchemesList;
		} finally {
			lock.readLock().unlock();
		}
	}

	public List<Distribution> getDistributionList() {
		lock.readLock().lock();
		try {
			return distributionList;
		} finally {
			lock.readLock().unlock();
		}
	}

	public List<Operation> getOperationList() {
		lock.readLock().lock();
		try {
			return operationList;
		} finally {
			lock.readLock().unlock();
		}
	}

	public List<WebService> getWebServiceList() {
		lock.readLock().lock();
		try {
			return webServiceList;
		} finally {
			lock.readLock().unlock();
		}
	}

	public List<Address> getAddressList() {
		lock.readLock().lock();
		try {
			return addressList;
		} finally {
			lock.readLock().unlock();
		}
	}

	public List<Location> getLocationList() {
		lock.readLock().lock();
		try {
			return locationList;
		} finally {
			lock.readLock().unlock();
		}
	}

	public List<PeriodOfTime> getPeriodOfTimeList() {
		lock.readLock().lock();
		try {
			return periodOfTimeList;
		} finally {
			lock.readLock().unlock();
		}
	}

	public List<Identifier> getIdentifierList() {
		lock.readLock().lock();
		try {
			return identifierList;
		} finally {
			lock.readLock().unlock();
		}
	}

	public List<Mapping> getMappingList() {
		lock.readLock().lock();
		try {
			return mappingList;
		} finally {
			lock.readLock().unlock();
		}
	}

	public List<Facility> getFacilityList() {
		lock.readLock().lock();
		try {
			return facilityList;
		} finally {
			lock.readLock().unlock();
		}
	}

	public List<Equipment> getEquipmentList() {
		lock.readLock().lock();
		try {
			return equipmentList;
		} finally {
			lock.readLock().unlock();
		}
	}
}
