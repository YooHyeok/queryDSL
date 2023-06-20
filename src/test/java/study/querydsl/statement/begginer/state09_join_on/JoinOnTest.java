package study.querydsl.statement.begginer.state09_join_on;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

/**
 *
 * queryDSL은 결과적으로 JPQL로 빌더한다.
 * application.yml에 use_sql_comments : true를 추가하면 JPQ의 힌트가 나간다 (주석형태)
 *
 */
@SpringBootTest
@Transactional
//@Commit
public class JoinOnTest {
    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory;
    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em); // 동시성 문제 : 동시에 여러 멀티스레드로 접근하는것에 대해 문제없게 설계되어있다.

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);
        Member member1 = new Member("Member1", 10, teamA);
        Member member2 = new Member("Member2", 20, teamA);
        Member member3 = new Member("Member3", 30, teamB);
        Member member4 = new Member("Member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        //초기화
        /*em.flush();
        em.clear();*/
    }

     /**
     * 조인 - on절
     * 조인 대상 필터링, 연관관계 없는 엔티티 외부 조인
     * 예) 회원과 팀을 조인 하면서, 팀 이름이 teamA인 팀만 조인하고, 회원은 모두 조회한다.
     * JPQL : select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void joinOnFiltering() {
        List<Tuple> joinResult = queryFactory
                .select(member, team)
                .from(member)
//                .leftJoin(member.team, team) //첫번째 인자와 두번째 인자의 id값 즉, team의 id가 일치하는 조건으로 조인한다.
//                (연관관계에 있는 객체 기준으로 매핑되므로 on이 자동으로 걸림.)
                .leftJoin(team).on(member.team.id.eq(team.id)) // 이렇게도 사용할 수 있다.

                .on(team.name.eq("teamA"))
                .fetch();
        for (Tuple tuple : joinResult) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 연관관계가 없는 엔티티 외부 조인 (join구문에 마스터 테이블을 지정하고 on절을 통해 비교 조회)
     * 예) 회원의 이름과 팀의 이름이 같은 대상 외부조인
     * JPQL : select m, t from Member m left join Team t on m.username = t.name;
     * SQL : select m.*, t.* from member m left join team t on m.username = t.name;
     */

    @Test
    public void joinOnNoRelation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
}

