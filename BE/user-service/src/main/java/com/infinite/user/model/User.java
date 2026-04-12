package com.infinite.user.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.infinite.common.util.Constants;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "APP_USER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @NotNull
    @Pattern(regexp = Constants.EMAIL_REGEX)
    @Size(min = 1, max = 50)
    @Column(name = "USERNAME", length = 50, unique = true, nullable = false)
    private String username;

    @JsonIgnore
    @NotNull
    @Size(min = 60, max = 60)
    @Column(name = "PASSWORD", length = 60, nullable = false)
    private String password;

    @Size(max = 50)
    @Column(name = "NAME", length = 50)
    private String name;

    @Email
    @Size(min = 5, max = 254)
    @Column(name = "EMAIL",length = 254, unique = true)
    private String email;

    @Size(max = 256)
    @Column(name = "IMAGE_URL", length = 256)
    private String imageUrl;

    @NotNull
    @Column(name = "ACTIVE", nullable = false)
    private Integer active = 0;

    @Column(name = "LOCK_TIME")
    private LocalDateTime lockTime;

    @NotNull
    @Column(name = "IS_DELETE", nullable = false)
    private boolean isDelete = false;

    @Size(max = 50)
    @Column(name = "CREATE_BY", length = 50)
    private String createBy;

    @CreatedDate
    @Column(name = "CREATED_TIME")
    private LocalDateTime createdTime;

    @Size(max = 50)
    @Column(name = "MODIFIED_BY", length = 50)
    private String modifiedBy;

    @LastModifiedDate
    @Column(name = "MODIFIED_TIME")
    private LocalDateTime modifiedTime;

}