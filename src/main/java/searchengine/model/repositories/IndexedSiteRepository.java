package searchengine.model.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.IndexedSite;

@Repository
public interface IndexedSiteRepository extends CrudRepository<IndexedSite, Integer> {
}
