package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;

import java.util.ArrayList;


@Repository
public interface PageRepository extends JpaRepository<PageEntity,Integer> {

    @Query(value = "select * from page where `path` =:path", nativeQuery = true)
    PageEntity findByName(String path);

    @Query(value = " select * from page where `path` =:path and site_id =:siteId", nativeQuery = true)
    PageEntity findByPathAndId(String path, int siteId);

    @Query(value = "select * from page where site_id=:siteId", nativeQuery = true)
    ArrayList<PageEntity> findAllPagesForSite(int siteId);

    @Query(value = "select count(*) from page where site_id =:siteId", nativeQuery = true)
    int countPagesForSite(int siteId);


}
