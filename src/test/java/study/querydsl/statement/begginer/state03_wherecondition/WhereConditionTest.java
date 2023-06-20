package study.querydsl.statement.begginer.state03_wherecondition;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

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
public class WhereConditionTest {
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

    /** 검색조건 쿼리
     * eq() / and() / or() / isNotNull() / in() / notIn() / between()
     * like("value%") / contains("value") : '%value%' 검색
     * startWith("value") : 'value%' 검색 / endWith("value") : '%value'
     * goe(value) : column >= value / loe(value) : column <= value
     * gt(value) : column > value / lt(value) : column < value
     * */
    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where( // username = "Member1" and (age between 10 and 30) / or연산도 가능
                        member.username.eq("Member1")
                                .and(member.age.between(10,30))
                ).fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("Member1");


    }

    /**
     * and() 대신 ,(쉼표)로 대체 가능
     */
    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where( // username = "Member1" and (age between 10 and 30) / or연산도 가능
                        member.username.eq("Member1"), // chain으로 .and() 대신 ,로 대체해서 사용할 수 있다.
                        member.age.eq(10)
                ).fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("Member1");
    }
}

