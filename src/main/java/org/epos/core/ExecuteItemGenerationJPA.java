package org.epos.core;

import org.epos.api.beans.Distribution;
import org.epos.api.beans.ServiceParameter;
import org.epos.api.beans.TemporalCoverage;
import org.epos.api.utility.Utils;
import org.epos.eposdatamodel.State;
import org.epos.handler.dbapi.model.*;
import org.epos.handler.dbapi.service.DBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import javax.persistence.EntityManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static org.epos.handler.dbapi.util.DBUtil.getFromDB;

public class ExecuteItemGenerationJPA {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteItemGenerationJPA.class);

	public static Distribution generate(Map<String,Object> parameters) {

		LOGGER.info("Parameters {}", parameters);

		EntityManager em = new DBService().getEntityManager();

		List<EDMDistribution> distributionSelectedList = getFromDB(em, EDMDistribution.class,
				"distribution.findAllByMetaId",
				"METAID", parameters.get("id"));

		if (distributionSelectedList.stream().noneMatch(distrSelected -> distrSelected.getState().equals("PUBLISHED")))
			return null;

		EDMDistribution distributionSelected = distributionSelectedList.stream().filter(distrSelected -> distrSelected.getState().equals("PUBLISHED")).collect(Collectors.toList()).get(0);

		if (distributionSelected == null) return null;

		EDMDataproduct dp;
		if (distributionSelected.getIsDistributionsByInstanceId() != null &&
				!distributionSelected.getIsDistributionsByInstanceId().isEmpty()) {
			dp = distributionSelected.getIsDistributionsByInstanceId().stream()
					.map(EDMIsDistribution::getDataproductByInstanceDataproductId)
					.filter(edmDataproduct -> edmDataproduct.getState().equals(State.PUBLISHED.toString()))
					.findFirst().orElse(null);
			if (dp == null) return null;
		} else {
			return null;
		}

		EDMWebservice ws = distributionSelected.getWebserviceByAccessService() != null && distributionSelected.getWebserviceByAccessService().getState().equals(State.PUBLISHED.toString()) ?
				distributionSelected.getWebserviceByAccessService() : null;
		if (ws == null && distributionSelected.getAccessService() != null) return null;

		EDMOperation op = null;
		if (distributionSelected.getAccessURLByInstanceId() != null) {
			op = distributionSelected.getAccessURLByInstanceId().stream()
					.map(EDMDistributionAccessURL::getOperationByInstanceOperationId).collect(Collectors.toList()).get(0);
		} else {
			return null;
		}

		if (op == null && distributionSelected.getAccessService() != null) return null;


		Distribution distribution = new Distribution();

		if (distributionSelected.getType() != null) {
			String[] type = distributionSelected.getType().split("\\/");
			distribution.setType(type[type.length - 1]);
		}

		distribution.setId(Optional.ofNullable(distributionSelected.getMetaId()).orElse(null));

		if (distributionSelected.getDistributionDownloadurlsByInstanceId() != null) {
			distribution.setDownloadURL(
					Optional.of(
							distributionSelected.getDistributionDownloadurlsByInstanceId().stream()
									.map(EDMDistributionDownloadurl::getDownloadurl).collect(Collectors.joining("."))
					).orElse(null)
			);
		}

		if (distributionSelected.getAccessURLByInstanceId() != null) {
			distribution.setEndpoint(
					Optional.of(
							distributionSelected.getAccessURLByInstanceId().stream()
									.map(EDMDistributionAccessURL::getOperationByInstanceOperationId).map(EDMOperation::getTemplate).collect(Collectors.joining("."))
					).orElse(null)
			);
		}

		distribution.setLicense(Optional.ofNullable(distributionSelected.getLicense()).orElse(null));

		distribution.setParameters(new ArrayList<>());
		// OPERATION AND PARAMETERS
		if (Objects.nonNull(op)) {
			LOGGER.info("TEMPLATE HERE: "+op.getTemplate());
			distribution.setEndpoint(op.getTemplate());
			if(op.getTemplate()!=null) distribution.setServiceEndpoint(op.getTemplate().split("\\{")[0]);
			distribution.setOperationid(op.getUid());
			if (op.getMappingsByInstanceId() != null) {
				for (EDMMapping mp : op.getMappingsByInstanceId()) {
					ServiceParameter sp = new ServiceParameter();
					sp.setDefaultValue(mp.getDefaultvalue());
					sp.setEnumValue(
							mp.getMappingParamvaluesById() != null ?
									mp.getMappingParamvaluesById().stream().map(EDMMappingParamvalue::getParamvalue).collect(Collectors.toList())
									: new ArrayList<>()
					);
					sp.setName(mp.getVariable());
					sp.setMaxValue(mp.getMaxvalue());
					sp.setMinValue(mp.getMinvalue());
					sp.setLabel(mp.getLabel() != null ? mp.getLabel().replaceAll("@en", "") : null);
					sp.setProperty(mp.getProperty());
					sp.setRequired(mp.getRequired());
					sp.setType(mp.getRange() != null ? mp.getRange().replace("xsd:", "") : null);
					sp.setValue(null);
					if (parameters.containsKey("useDefaults") && Boolean.getBoolean(parameters.get("useDefaults").toString())) {
						if (mp.getDefaultvalue() != null) {
							if (mp.getProperty() != null && mp.getValuepattern() != null) {
								if (mp.getProperty().equals("schema:startDate") || mp.getProperty().equals("schema:endDate")) {
									try {
										sp.setValue(Utils.convertDateUsingPattern(mp.getDefaultvalue(), null, mp.getValuepattern()));
									} catch (ParseException e) {
										LOGGER.error(e.getLocalizedMessage());
									}
								}
							} else sp.setValue(mp.getDefaultvalue());
						} else sp.setValue(null);
					} else {
						if(parameters.containsKey("params")) {
							JsonObject params = Utils.gson.fromJson(parameters.get("params").toString(), JsonObject.class);
							if (params.has(mp.getVariable()) && !params.get(mp.getVariable()).getAsString().equals("")) {
								if (mp.getProperty() != null && mp.getValuepattern() != null) {
									if (mp.getProperty().equals("schema:startDate") || mp.getProperty().equals("schema:endDate")) {
										try {
											sp.setValue(Utils.convertDateUsingPattern(params.get(mp.getVariable()).getAsString(), null, mp.getValuepattern()));
										} catch (ParseException e) {
											LOGGER.error(e.getLocalizedMessage());
										}
									}
								} else if (mp.getProperty() == null && mp.getValuepattern() != null) {
									if (Utils.checkStringPattern(params.get(mp.getVariable()).getAsString(), mp.getValuepattern()))
										sp.setValue(params.get(mp.getVariable()).getAsString());
									else if (!Utils.checkStringPattern(params.get(mp.getVariable()).getAsString(), mp.getValuepattern()) && Utils.checkStringPatternSingleQuotes(mp.getValuepattern()))
										sp.setValue("'" + params.get(mp.getVariable()).getAsString() + "'");
									else sp.setValue(params.get(mp.getVariable()).getAsString()); //return new JsonObject();
								} else sp.setValue(params.get(mp.getVariable()).getAsString());
							} else sp.setValue(null);
						}
					}
					if(sp.getValue()!=null)
						try {
							sp.setValue(URLEncoder.encode(sp.getValue(), StandardCharsets.UTF_8.toString()));
						} catch (UnsupportedEncodingException e) {
							LOGGER.error(e.getLocalizedMessage());
						}

					sp.setValuePattern(mp.getValuepattern());
					sp.setVersion(null);
					sp.setReadOnlyValue(mp.getReadOnlyValue());
					sp.setMultipleValue(mp.getMultipleValues());
					distribution.getParameters().add(sp);
				}
			}
		}
		em.close();

		return distribution;
	}

}