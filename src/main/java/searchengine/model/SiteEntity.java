package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "site")
@NoArgsConstructor
@Getter
@Setter
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING','INDEXED','FAILED')", nullable = false)
     IndexingStatus status;

    @Column(name = "status_time")
     LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
     String lastError;

    @Column(columnDefinition = "varchar(255)")
     String url;

    @Column(columnDefinition = "varchar(255)")
     String name;

    @OneToMany(mappedBy = "siteId")
    List<PageEntity> pagesList;

    @OneToMany(mappedBy = "siteId")
    List<PageEntity> lemmaList;


}
