package com.infinite.file.repository;

import com.infinite.file.entity.FileResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileResourceRepository extends JpaRepository<FileResource, Long> {
    
    Optional<FileResource> findByObjectPath(String objectPath);
    
    List<FileResource> findByName(String name);
    
    List<FileResource> findByParentPath(String parentPath);
    
    List<FileResource> findByNameAndPath1(String name, String path1);
    
    List<FileResource> findByNameAndPath1AndPath2(String name, String path1, String path2);
    
    boolean existsByParentPathAndName(String parentPath, String name);
    
    @Query("SELECT f FROM FileResource f WHERE " +
           "(:path1 IS NULL OR f.path1 = :path1) AND " +
           "(:path2 IS NULL OR f.path2 = :path2) AND " +
           "(:path3 IS NULL OR f.path3 = :path3) AND " +
           "(:path4 IS NULL OR f.path4 = :path4) AND " +
           "(:path5 IS NULL OR f.path5 = :path5) AND " +
           "(:path6 IS NULL OR f.path6 = :path6) AND " +
           "(:path7 IS NULL OR f.path7 = :path7) AND " +
           "(:path8 IS NULL OR f.path8 = :path8) AND " +
           "(:path9 IS NULL OR f.path9 = :path9) AND " +
           "(:path10 IS NULL OR f.path10 = :path10) AND " +
           "(:name IS NULL OR f.name = :name) AND " +
           "(:resourceType IS NULL OR f.resourceType = :resourceType) " +
           "ORDER BY f.objectPath")
    List<FileResource> searchByPaths(
            @Param("path1") String path1,
            @Param("path2") String path2,
            @Param("path3") String path3,
            @Param("path4") String path4,
            @Param("path5") String path5,
            @Param("path6") String path6,
            @Param("path7") String path7,
            @Param("path8") String path8,
            @Param("path9") String path9,
            @Param("path10") String path10,
            @Param("name") String name,
            @Param("resourceType") String resourceType
    );
    
    List<FileResource> findByNameOrderByObjectPath(String name);
}
