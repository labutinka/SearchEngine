package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "`index`")
@NoArgsConstructor
@Getter
@Setter
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @ManyToOne
    @JoinColumn (name = "page_id")
    private PageEntity pageId;
    @ManyToOne
    @JoinColumn(name = "lemma_id")
    private LemmaEntity lemmaId;

    @Column(name = "`rank`")
    private float rank;

}
