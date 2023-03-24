package searchengine.model;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "page", indexes = {@Index(columnList = "path, site_id", name = "path_index")})
@Data
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    int id;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "site_id")
    SiteEntity siteId;

    String path;

    int code;

    @Column(columnDefinition = "MEDIUMTEXT")
    String content;



}