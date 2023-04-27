package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.IndexEntity;
import java.util.ArrayList;


public interface IndexRepository extends JpaRepository<IndexEntity,Integer> {
    @Query(value = "select * from `index` where page_id =:pageId", nativeQuery = true)
    ArrayList<IndexEntity> indexesForPage(long pageId);
}
