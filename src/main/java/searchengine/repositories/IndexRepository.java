package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.IndexEntity;

import java.util.ArrayList;



public interface IndexRepository extends JpaRepository<IndexEntity,Integer> {
    @Query(value = "select * from `index` where page_id =:pageId", nativeQuery = true)
    ArrayList<IndexEntity> indexesForPage(long pageId);

    @Query(value = "select * from `index` where lemma_id=:lemmaId", nativeQuery = true)
    ArrayList<IndexEntity> findPagesByLemma(long lemmaId);

    @Query(value = "SELECT SUM(`rank`) FROM `index` WHERE page_id =:pageId and lemma_id in (:lemmasList)", nativeQuery = true)
    float countAbsRelevanceForPage(Long pageId, ArrayList<Long> lemmasList);
}
