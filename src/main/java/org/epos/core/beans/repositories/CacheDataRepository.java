package org.epos.core.beans.repositories;

import org.epos.core.beans.CacheData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface CacheDataRepository extends CrudRepository<CacheData, String> {
    
}
