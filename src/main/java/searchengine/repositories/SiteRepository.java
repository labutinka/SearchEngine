package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;

import java.util.Optional;
import java.util.Set;


@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    @Query(value = "select * from `site` where name =:name and url =:url", nativeQuery = true)
    Optional<SiteEntity> findByNameAndUrl(String name, String url);

    SiteEntity findSiteEntityByUrlContaining(String url);

    @Query(value = "select * from `site` where status = 'INDEXED'", nativeQuery = true)
    Set<SiteEntity> findIndexedSites();
}
