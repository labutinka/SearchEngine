package searchengine.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "site")
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

    @OneToMany(mappedBy = "siteId", cascade = CascadeType.REMOVE)
    List<PageEntity> pagesList;

}
