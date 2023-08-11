package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.entities.SearchIndex;

public interface SearchIndexRepository extends JpaRepository<SearchIndex, Integer> {
}
