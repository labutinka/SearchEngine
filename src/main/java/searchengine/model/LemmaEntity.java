package searchengine.model;

import javax.persistence.*;

@Entity
@Table(name = "lemma")
public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "site_id")
    private int siteId;

    @Column(columnDefinition = "varchar(255)")
    private String lemma;

    private int frequency;


}
