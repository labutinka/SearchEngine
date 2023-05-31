package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity,Integer> {

    @Query(value = "select * from lemma where site_id =:siteId and lemma=:lemma", nativeQuery = true)
    Optional<LemmaEntity> findLemmaByNameAndSiteId(long siteId, String lemma);

    @Query(value = "select * from lemma where site_id=:siteId", nativeQuery = true)
    ArrayList<LemmaEntity> lemmasForSite(long siteId);

    @Query(value = "select count(*) from lemma where site_id =:siteId", nativeQuery = true)
    int countLemmasForSite(long siteId);

    @Query(value = "select * from lemma where lemma in (:lemmasList) and site_id =:siteId and frequency < :restriction order by frequency", nativeQuery = true)
    ArrayList<LemmaEntity> findLemmasAndFrequency(List<String> lemmasList, long siteId, int restriction);

    @Query(value = "select id from lemma where lemma in (:lemmasList)", nativeQuery = true)
    ArrayList<Long> findLemmaIdByName(List<String> lemmasList);
}
