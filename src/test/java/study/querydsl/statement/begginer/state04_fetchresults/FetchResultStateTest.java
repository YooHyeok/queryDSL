package study.querydsl.statement.begginer.state04_fetchresults;

import com.querydsl.core.QueryResults;
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
public class FetchResultStateTest {
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

    @Test
    public void resultFetchTest() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch(); // 복수 조회 - 리스트로 반환
        Member fetchOne = queryFactory
                .selectFrom(member)
                .where(member.username.eq("Member1"))
                .fetchOne(); // 단건 조회 - 객체로 반환
        Member fetchFirst = queryFactory
                .selectFrom(member)
//                .limit(1).fetchOne();
                .fetchFirst();// 검색된 로우중 첫번째 로우 반환 limit(1).fetchOne()과 동일한 결과 반환
        
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .offset(0) // 첫번째 행부터 가져온다.
                .limit(4)  // 1개 가져온다.
                .fetchResults();

        long totalResult = results.getTotal();//totalCount를 가져온다.
        System.out.println("totalResult = " + totalResult);

        List<Member> content = results.getResults(); // result로 부터 꺼내야 데이터를 사용할 수 있다.
        System.out.println("content = " + content);

        long offset = results.getOffset();// 몇번째부터 가져와
        System.out.println("offset = " + offset);

        long limit = results.getLimit();// 몇개 가져와
        System.out.println("limit = " + limit);

        long total = queryFactory
                .selectFrom(member)
                .fetchCount();
        System.out.println("total = " + total);
    }

}

