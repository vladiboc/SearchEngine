package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.entities.SiteLemma;

public interface SiteLemmaRepository extends JpaRepository<SiteLemma, Integer> {
}
