package study.querydsl.statement.begginer.state08_join_basic;

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
public class BasicJoinStateTest {
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
     * join 구문 이너조인(innerJoin)
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void innerJoin() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team) //첫번째 인자와 두번째 인자의 id값 즉, team의 id가 일치하는 조건으로 조인한다.
//                (연관관계에 있는 객체 기준으로 매핑되므로 on이 자동으로 걸림.)
//                내부조인이면 where절에서 필터링하는것과 기능이 동일하다.

//                .join(team).on(member.team.id.eq(team.id)) // 이렇게도 사용할 수 있다.
                .where(team.name.eq("teamA"))
                .fetch();
        System.out.println("result = " + result);
        assertThat(result)
                .extracting("username") // 1. username이
                .containsExactly("Member1", "Member2"); // 2. Member1과 Member2 인지 검증
    }

    /**
     * join 구문 레프트조인(leftJoin)
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void leftJoin() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        System.out.println("result = " + result);
        assertThat(result)
                .extracting("username") // 1. username이
                .containsExactly("Member1", "Member2"); // 2. Member1과 Member2 인지 검증
    }

    /**
     * join 구문 세타조인(theta-cross Join)
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void thetaJoin() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        System.out.println("result = " + result);
        assertThat(result)
                .extracting("username") // 1. username이
                .containsExactly("teamA", "teamB"); // 2. Member1과 Member2 인지 검증
    }

}

