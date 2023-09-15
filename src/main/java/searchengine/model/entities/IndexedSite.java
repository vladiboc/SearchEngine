package searchengine.model.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "site")
public class IndexedSite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    private IndexingStatus indexingStatus;

    @Column(name = "status_time", nullable = false)
    private LocalDateTime indexingStatusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(columnDefinition = "VARCHAR(255) NOT NULL, UNIQUE INDEX url_index (url(255))")
    private String url;

    @Column(columnDefinition = "VARCHAR(255) NOT NULL, UNIQUE INDEX name_index (name(255))")
    private String name;

}
