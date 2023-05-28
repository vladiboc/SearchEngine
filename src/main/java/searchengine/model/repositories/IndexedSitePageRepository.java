package searchengine.model.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.IndexedPage;

@Repository
public interface IndexedSitePageRepository extends CrudRepository<IndexedPage, Integer> {
}
