package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;


@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    @Query(value = "select * from `site` where name =:name and url =:url", nativeQuery = true)
    SiteEntity findByNameAndUrl(String name, String url);

    SiteEntity findSiteEntityByUrlContaining(String name);
}
