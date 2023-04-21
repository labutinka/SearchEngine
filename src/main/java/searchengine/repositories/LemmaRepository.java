package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;

import java.util.ArrayList;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity,Integer> {

    @Query(value = "select * from lemma where site_id =:siteId and lemma=:lemma", nativeQuery = true)
    LemmaEntity findLemmaByNameAndSiteId(int siteId, String lemma);

    @Query(value = "select * from lemma where site_id=:siteId", nativeQuery = true)
    ArrayList<LemmaEntity> lemmasForSite(int siteId);

    @Query(value = "select count(*) from lemma where site_id =:siteId", nativeQuery = true)
    int countLemmasForSite(int siteId);
}
