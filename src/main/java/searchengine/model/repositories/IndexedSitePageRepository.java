package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.IndexedPage;

@Repository
public interface IndexedSitePageRepository extends JpaRepository<IndexedPage, Integer> {
}
