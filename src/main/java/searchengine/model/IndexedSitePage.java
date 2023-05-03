package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "page")
public class IndexedSitePage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "site_id", nullable = false)
    private IndexedSite site;

    @Column(name = "path", columnDefinition = "TEXT NOT NULL, INDEX path_index (path(255))")
    private String pagePath;

    @Column(name = "code", nullable = false)
    private int  httpResponseCode;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String pageContent;

}