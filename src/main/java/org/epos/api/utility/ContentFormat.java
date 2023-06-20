package org.epos.api.utility;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public enum ContentFormat {

	XML("xml", "xsd", "xml"),
	JSON("json", "json", "json"),
	YAML("yaml", "json", "yaml", "yml");
	
	final String name;
	final Set<String> fileExtension;
	final String schemaFileExtension;
	
	private ContentFormat(String name, String schemaFileExtension, String... fileExtension) {
		this.name = name;
		this.fileExtension = Set.of(fileExtension);
		this.schemaFileExtension = schemaFileExtension;
	}

	public String getName() {
		return name;
	}
	
	public String getSchemaFileExtension() {
		return schemaFileExtension;
	}
	
	public Set<String> getFileExtensions() {
		return fileExtension;
	}
	
	public static Optional<ContentFormat> getInstance(String id) {
		return StringUtils.isBlank(id) ? Optional.empty() : 
					Arrays.stream(ContentFormat.values())
						.filter(v -> StringUtils.isNotBlank(v.getName()) && v.getName().equals(id))
						.findAny();
	}
}
