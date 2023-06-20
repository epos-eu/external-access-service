package org.epos.core.beans;

import java.util.List;
import java.util.Objects;

public class DataServiceProvider {

    private String dataProviderLegalName;
    private String dataProviderUrl;
    private List<DataServiceProvider> relatedDataServiceProvider;

    public String getDataProviderLegalName() {
        return dataProviderLegalName;
    }

    public void setDataProviderLegalName(String dataProviderLegalName) {
        this.dataProviderLegalName = dataProviderLegalName;
    }

    public List<DataServiceProvider> getRelatedDataProvider() {
        return relatedDataServiceProvider;
    }

    public void setRelatedDataProvider(List<DataServiceProvider> relatedDataServiceProvider) {
        this.relatedDataServiceProvider = relatedDataServiceProvider;
    }

    public String getDataProviderUrl() {
        return dataProviderUrl;
    }

    public void setDataProviderUrl(String dataProviderUrl) {
        this.dataProviderUrl = dataProviderUrl;
    }

    @Override
    public String toString() {
        return "DataProvider{" +
                "dataProviderLegalName='" + dataProviderLegalName + '\'' +
                ", dataProviderUrl='" + dataProviderUrl + '\'' +
                ", relatedDataProvider=" + relatedDataServiceProvider +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataServiceProvider that = (DataServiceProvider) o;
        return Objects.equals(getDataProviderLegalName(), that.getDataProviderLegalName()) && Objects.equals(getDataProviderUrl(), that.getDataProviderUrl()) && Objects.equals(getRelatedDataProvider(), that.getRelatedDataProvider());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataProviderLegalName(), getDataProviderUrl(), getRelatedDataProvider());
    }
}

