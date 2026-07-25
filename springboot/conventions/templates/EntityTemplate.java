package com.restaurant.api.conventions.templates;

import com.example.entity.base.BaseFullTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity 템플릿
 *
 * 사용법:
 * 1. 클래스명을 도메인명으로 변경 (예: Product)
 * 2. 테이블명을 snake_case로 변경 (예: product)
 * 3. 필드 정의
 * 4. 생성자 정의 (UUID 생성 포함)
 * 5. update 메서드 정의
 */
@Entity
@Table(name = "template")  // TODO: 테이블명 변경
@Getter
@NoArgsConstructor
public class EntityTemplate extends BaseFullTimeEntity {

    // ===== ID =====
    @Id
    @Column(updatable = false, nullable = false)
    private String id;

    // ===== 기본 필드 =====
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    // ===== Enum 필드 =====
    // @Enumerated(EnumType.STRING)
    // @Column(name = "status", nullable = false)
    // private Status status;

    // ===== 연관관계 (ManyToOne) =====
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "user_id")
    // private User user;

    // ===== 연관관계 (OneToMany) =====
    // @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<TemplateItem> itemList = new ArrayList<>();

    // ===== 생성자 =====
    public EntityTemplate(String name, String description) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
    }

    // 연관관계가 있는 생성자 예시
    // public EntityTemplate(String name, User user) {
    //     this.id = UUID.randomUUID().toString();
    //     this.name = name;
    //     this.user = user;
    // }

    // ===== Update 메서드 =====
    public void update(String name, String description) {
        if (name != null) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
    }

    // 단일 필드 업데이트
    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        this.name = name;
    }

    // ===== 연관관계 편의 메서드 =====
    // public void addItem(TemplateItem item) {
    //     this.itemList.add(item);
    // }

    // public void clearItemList() {
    //     this.itemList.clear();
    // }
}
