package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface PageRepository extends JpaRepository<PageEntity,Integer> {

    @Query(value = "select * from page where `path` = :path", nativeQuery = true)
    PageEntity findByName(String path);


}
