package study.querydsl.statement.begginer.state10_join_fetchjoin;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

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
public class FetchJoinTest {
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
     * 패치 조인 미적용 (연관관계 엔터티가 끌려오지 않는다.)
     */
    @PersistenceUnit
    EntityManagerFactory emf;
    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear(); // 페치조인 테스트시 데이터 초기화
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("Member1"))
                .fetchOne();
        System.out.println("findMember = " + findMember);
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());// 1차캐시에 로딩된 엔터티인지 초기화안된 엔터티인지 유무 확인
        assertThat(loaded).as("패치조인 미적용").isFalse();
    }

    /**
     * 패치 조인 적용
     */
    @Test
    public void fetchJoinUse() {
        em.flush();
        em.clear(); // 페치조인 테스트시 데이터 초기화
        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("Member1"))
                .fetchOne();
        System.out.println("findMember = " + findMember);
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());// 1차캐시에 로딩된 엔터티인지 초기화안된 엔터티인지 유무 확인
        assertThat(loaded).as("패치조인 적용").isTrue();
    }
}

