package searchengine.model.entities;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "page")
public class IndexedPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(cascade = CascadeType.REFRESH)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "site_id", nullable = false)
    private IndexedSite site;

    @Column(name = "path", columnDefinition = "TEXT NOT NULL, INDEX path_index (path(255))")
    private String path;

    @Column(name = "code", nullable = false)
    private int  httpResponseCode;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

}