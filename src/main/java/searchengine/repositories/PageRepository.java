package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;


@Repository
public interface PageRepository extends JpaRepository<PageEntity,Integer> {

    @Query(value = " select * from page where `path` =:path and site_id =:siteId", nativeQuery = true)
    PageEntity findByPathAndId(String path, long siteId);

    @Query(value = "select * from page where site_id=:siteId", nativeQuery = true)
    ArrayList<PageEntity> findAllPagesForSite(long siteId);

    @Query(value = "select count(*) from page where site_id =:siteId", nativeQuery = true)
    int countPagesForSite(long siteId);

    @Query(value = "select * from page where id in (:pagesIds)", nativeQuery = true)
    ArrayList<PageEntity> findPagesById(List<Long> pagesIds);

    @Transactional
    @Modifying
    @Query(value = "ALTER TABLE page ALTER INDEX `path_index` INVISIBLE", nativeQuery = true)
    void deactivateIndex();

    @Transactional
    @Modifying
    @Query(value = "ALTER TABLE page ALTER INDEX `path_index` VISIBLE", nativeQuery = true)
    void activateIndex();

}