package searchengine.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndexedSiteRepository extends CrudRepository<IndexedSite, Integer> {
}
