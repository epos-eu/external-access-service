package org.epos.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.epos.handler.dbapi.dbapiimplementation.SoftwareApplicationDBAPI;
import org.epos.handler.dbapi.dbapiimplementation.SoftwareSourceCodeDBAPI;
import org.epos.api.beans.Plugin;
import org.epos.api.utility.Utils;
import org.epos.eposdatamodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


public class PluginGeneration {

	private static final Logger LOGGER = LoggerFactory.getLogger(PluginGeneration.class); 
	
	public static JsonArray generate(JsonObject response, JsonObject parameters, String pluginType) {
		List<SoftwareApplication> softwareApplicationList = new SoftwareApplicationDBAPI().getAllByState(State.PUBLISHED);
		List<SoftwareSourceCode> softwareSourceCodeList = new SoftwareSourceCodeDBAPI().getAllByState(State.PUBLISHED);

		List<SoftwareApplication> softwareToHave = new ArrayList<>();
		List<SoftwareSourceCode> softwareSCToHave = new ArrayList<>();
		
		for(SoftwareApplication sw : softwareApplicationList) {
			String identifier = sw.getUid();
			if(identifier.toLowerCase().contains(pluginType)) softwareToHave.add(sw);
		}

		for(SoftwareSourceCode sw : softwareSourceCodeList) {
			String identifier = sw.getUid();
			if(identifier.toLowerCase().contains(pluginType)) softwareSCToHave.add(sw);
		}
		

		HashMap<String,ArrayList<SoftwareApplication>> tempSoftwares = new HashMap<>();
		HashMap<String,ArrayList<SoftwareSourceCode>> tempSoftwaresSC = new HashMap<>();
		for(SoftwareApplication softwareResults : softwareToHave) {
			String identifier = softwareResults.getUid().replace("SoftwareSourceCode/", "").replace("SoftwareApplication/", "");
			if(tempSoftwares.containsKey(identifier)) {
				tempSoftwares.get(identifier).add(softwareResults);
			}else {
				tempSoftwares.put(identifier, new ArrayList<>());
				tempSoftwares.get(identifier).add(softwareResults);
			}
		}
		for(SoftwareSourceCode softwareResults : softwareSCToHave) {
			String identifier = softwareResults.getUid().replace("SoftwareSourceCode/", "").replace("SoftwareApplication/", "");
			if(tempSoftwaresSC.containsKey(identifier)) {
				tempSoftwaresSC.get(identifier).add(softwareResults);
			}else {
				tempSoftwaresSC.put(identifier, new ArrayList<>());
				tempSoftwaresSC.get(identifier).add(softwareResults);
			}
		}

		softwareToHave.clear();
		softwareSCToHave.clear();
		tempSoftwares.entrySet().stream().forEach(e->{
			if(e.getValue().size()>1) {
				SoftwareApplication sw = new SoftwareApplication();
				for(int i = 0; i<e.getValue().size();i++)
					sw =Utils.mergeObjects(sw, e.getValue().get(i));
				e.getValue().clear();
				e.getValue().add(sw);
			}
		});
		tempSoftwaresSC.entrySet().stream().forEach(e->{
			if(e.getValue().size()>1) {
				SoftwareSourceCode sw = new SoftwareSourceCode();
				for(int i = 0; i<e.getValue().size();i++)
					sw =Utils.mergeObjects(sw, e.getValue().get(i));
				e.getValue().clear();
				e.getValue().add(sw);
			}
		});
		
		ArrayList<Plugin> pluginList = new ArrayList<>();
		for(ArrayList<SoftwareApplication> items : tempSoftwares.values()) {
			for(SoftwareApplication item : items) {
				String uid = item.getUid().replaceAll("/SoftwareApplication", "").replaceAll("file:///", "");
				Plugin p = pluginList.stream().filter(plugin->plugin.getIdentifier().equals(uid)).findAny().orElse(null);
				if(p == null) {
					p = new Plugin(uid);
					pluginList.add(p);
				}

				String[] requirements = null;
				String req =null;
				if(item.getRequirements()!=null) {
					requirements = item.getRequirements().split("\\;");
					req = item.getRequirements().replace(requirements[0]+";", "");
				}
				// convert list of parameter to a map
				Map<String, Map<String, String>> action = new HashMap<>();
				item.getParameter().forEach( parm -> {
					if(action.containsKey(parm.getAction())){
						action.get(parm.getAction()).put("encodingFormat", parm.getEncodingFormat());
						action.get(parm.getAction()).put("conformsTo", parm.getConformsTo());
					} else {
						Map<String, String> tmpMap = new HashMap<>();
						tmpMap.put("encodingFormat", parm.getEncodingFormat());
						tmpMap.put("conformsTo", parm.getConformsTo());
						action.put(parm.getAction().toString(), tmpMap);
					}
				});
				p.setOperations(item.getRelation().stream().map(LinkedEntity::getUid).collect(Collectors.toList()));
				p.setAction(action);
				p.setProxyType(requirements[0]);
				p.setRequirements(req);
			}
		}
		
		for(ArrayList<SoftwareSourceCode> items : tempSoftwaresSC.values()) {
			for(SoftwareSourceCode item : items) {
				String uid = item.getUid().replaceAll("/SoftwareSourceCode", "").replaceAll("file:///", "");
				Plugin p = pluginList.stream().filter(plugin->plugin.getIdentifier().equals(uid)).findAny().orElse(null);
				if(p == null) {
					p = new Plugin(uid);
					pluginList.add(p);
				}
				p.setName(item.getName());
				p.setDescription(item.getDescription());
				p.setDownloadURL(item.getCodeRepository());
				p.setSoftwareVersion(item.getSoftwareVersion());
				p.setDocumentationURL(item.getMainEntityofPage());
				p.setLicense(item.getLicenseURL());
				p.setLocation("/"+p.getIdentifier()+"/"+p.getSoftwareVersion()+"/");
			}
		}
		
		if(parameters.has("operation")) {
			Plugin singlePlugin = pluginList.stream().filter(e->e.getOperations().contains(parameters.get("operation").getAsString())).findFirst().orElse(null);
			pluginList.clear();
			pluginList.add(singlePlugin);
		}

		return pluginList.isEmpty()? new JsonArray() : Utils.gson.toJsonTree(pluginList).getAsJsonArray();
		
	}

}
