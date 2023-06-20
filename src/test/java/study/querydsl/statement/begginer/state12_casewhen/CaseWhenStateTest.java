package study.querydsl.statement.begginer.state12_casewhen;

import com.querydsl.core.types.dsl.CaseBuilder;
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

/**
 *
 * queryDSL은 결과적으로 JPQL로 빌더한다.
 * application.yml에 use_sql_comments : true를 추가하면 JPQ의 힌트가 나간다 (주석형태)
 *
 */
@SpringBootTest
@Transactional
//@Commit
public class CaseWhenStateTest {
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
     * CASE문 : simpleCase
     */
    @Test
    public void simpleCase() {
        List<String> simpleCase = queryFactory
                .select(member.age
                        .when(10)
                        .then("열살")
                        .when(20)
                        .then("스무살")
                        .otherwise("기타") // else문
                ).from(member)
                .fetch();
        for (String s : simpleCase) {
            System.out.println("s = " + s);
            
        }
    }

    /**
     * CASE문 : complexCase (searchCase)
     */
    @Test
    public void complexCase() {
        List<String> complexCase = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0,20)).then("0~20살")
                        .when(member.age.between(21,30)).then("21~30살")
                        .otherwise("기타")
                ).from(member)
                .fetch();
        for (String s : complexCase) {
            System.out.println("s = " + s);

        }
    }
}

