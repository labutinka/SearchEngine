package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "lemma")
@NoArgsConstructor
@Getter
@Setter
public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "site_id")
    SiteEntity siteId;
    @Column(columnDefinition = "varchar(255)")
    private String lemma;
    private int frequency;


}
