package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "username", "age"}) // of옵션을 통해 양방향 관계에 존재하는 객체는 제외한다.
public class Member {
    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    private String username;
    private int age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    public Member(String username) {
        this(username, 0, null);
    }

    public Member(String username, int age) {
        this(username, age, null);
    }

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if (team != null) {
            changeTeam(team);
        }
    }

    /** M:1 & 1:M 양방향 연관관계 편의 메소드<br/>
     * Member의 team초기화<br/>
     * Team의 members초기화 */
    private void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }
}
