package study.querydsl.statement.begginer.state11_subquery;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

/**
 *
 * queryDSL은 결과적으로 JPQL로 빌더한다.
 * application.yml에 use_sql_comments : true를 추가하면 JPQ의 힌트가 나간다 (주석형태)
 *
 */
@SpringBootTest
@Transactional
//@Commit
public class SubQueryStateTest {
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
     * 서브쿼리
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() {
        QMember memberSub = new QMember("memberSub"); //qtype생성
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                        )
                ).fetch();
        System.out.println("result = " + result);
        assertThat(result)
                .extracting("age")
                .containsExactly(40); // List타입만 사용 가능.

    }

    /**
     * 연관 서브쿼리 Goe
     * 나이가 평균 이상인 회원 조회
     */
    @Test
    public void subQueryGoe() {
        QMember memberSub = new QMember("memberSub"); //qtype생성
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                        .select(memberSub.age.avg())
                        .from(memberSub)
                        )
                ).fetch();
        System.out.println("result = " + result);
        assertThat(result)
                .extracting("age")
                .containsExactly(30, 40); // List타입만 사용 가능.
    }

    /**
     * 연관 서브쿼리 In
     * 나이가 10살보다 많은 회원 조회
     */
    @Test
    public void subQueryIn() {
        QMember memberSub = new QMember("memberSub"); //qtype생성
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions.select(memberSub.age)
                        .from(memberSub)
                        .where(memberSub.age.gt(10))
                        )
                ).fetch();
        System.out.println("result = " + result);
        assertThat(result)
                .extracting("age")
                .containsExactly(20, 30, 40); // List타입만 사용 가능.
    }

    /**
     * 스칼라 서브쿼리
     */
    @Test
    public void selectSubquery() {
        QMember memberSub = new QMember("memberSub"); //qtype생성
        List<Tuple> result = queryFactory
                .select(member.username,
                        //JPAExpressions staticImport가능
                        select(memberSub.age.avg())
                        .from(memberSub)
                )
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
        /**
         * JPQL 서브쿼리 한계점으로 from절의 subQuery 즉, 인라인뷰는 지원하지 않는다.
         * queryDsl은 JPQL을 빌더해주는 역할을 하기 때문에 JPQL에서 지원하지 않는것들은 함께 지원하지 않는다.
         * 해결방안
         * 1. 서브쿼리를 Join으로 변경한다 (불가능 할 수도있다.)
         * 2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
         * 3. nativeQuery를 사용한다.
         */
    }
}

