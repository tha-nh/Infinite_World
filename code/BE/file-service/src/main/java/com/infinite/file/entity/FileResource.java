package com.infinite.file.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "FILE_RESOURCE")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResource {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    
    @Column(name = "RESOURCE_TYPE", nullable = false, length = 50)
    private String resourceType;
    
    @Column(name = "NAME", nullable = false)
    private String name;
    
    @Column(name = "URL", columnDefinition = "TEXT")
    private String url;
    
    @Column(name = "IS_URL_EXPIRABLE", nullable = false)
    private Boolean isUrlExpirable = false;
    
    @Column(name = "BUCKET_NAME", nullable = false)
    private String bucketName;
    
    @Column(name = "OBJECT_PATH", nullable = false, unique = true, columnDefinition = "TEXT")
    private String objectPath;
    
    @Column(name = "PARENT_PATH", columnDefinition = "TEXT")
    private String parentPath;
    
    @Column(name = "PATH_DEPTH", nullable = false)
    private Integer pathDepth = 0;
    
    @Column(name = "PATH_1")
    private String path1;
    
    @Column(name = "PATH_2")
    private String path2;
    
    @Column(name = "PATH_3")
    private String path3;
    
    @Column(name = "PATH_4")
    private String path4;
    
    @Column(name = "PATH_5")
    private String path5;
    
    @Column(name = "PATH_6")
    private String path6;
    
    @Column(name = "PATH_7")
    private String path7;
    
    @Column(name = "PATH_8")
    private String path8;
    
    @Column(name = "PATH_9")
    private String path9;
    
    @Column(name = "PATH_10")
    private String path10;
    
    @Column(name = "EXTENSION", length = 50)
    private String extension;
    
    @Column(name = "CONTENT_TYPE")
    private String contentType;
    
    @Column(name = "FILE_SIZE")
    private Long fileSize;
    
    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;
    
    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;
    
    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;
}
