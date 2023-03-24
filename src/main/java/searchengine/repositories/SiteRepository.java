package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;

import javax.transaction.Transactional;
import java.util.ArrayList;

@Repository
@Transactional
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "select * from `site` where name = :name and url = :url", nativeQuery = true)
    ArrayList<SiteEntity> findByNameAndUrl(String name, String url);
}
