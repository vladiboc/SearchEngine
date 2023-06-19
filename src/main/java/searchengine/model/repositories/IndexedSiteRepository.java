package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.IndexedSite;

@Repository
public interface IndexedSiteRepository extends JpaRepository<IndexedSite, Integer> {
}
