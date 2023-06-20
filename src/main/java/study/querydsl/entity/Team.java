package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) //JPA 기본스펙에서는 기본 생성자는 Protected Level까지 허용해 준다. (protected Team(){})
@ToString(of = {"id", "name"}) // of옵션을 통해 양방향 관계에 존재하는 객체는 제외한다.

public class Team {

    @Id @GeneratedValue
    @Column(name = "team_id")
    private Long id;
    private String name;

    @OneToMany(mappedBy = "team") //mappedBy : 연관관계의 주인이 되는 테이블에 존재하는 연관관계 객체
    private List<Member> members = new ArrayList<>();

    public Team(String name) {
        this.name = name;
    }

}
