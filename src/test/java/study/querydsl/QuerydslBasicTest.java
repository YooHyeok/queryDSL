package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;

/**
 *
 * queryDSL은 결과적으로 JPQL로 빌더한다.
 * application.yml에 use_sql_comments : true를 추가하면 JPQ의 힌트가 나간다 (주석형태)
 *
 */
@SpringBootTest
@Transactional
//@Commit
public class QuerydslBasicTest {
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
    public void entitymanager() {
        em.flush();
        em.clear();
        Member findMember = em.find(Member.class, 3L);
        assertThat(findMember.getId()).isEqualTo(3L);

    }

    @Test
    public void startJPQL() {
        // Member1 검색 (자동으로 flush해준다. JPQL은 직접 쿼리를 날리는것이기 때문에 clear 즉, 1차캐시와는 상관이 없다.)
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "Member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("Member1");
    }

    @Test
    public void startQuerydsl() {
//        JPAQueryFactory queryFactory = new JPAQueryFactory(em); // 전역변수로 선언 후 EachBefore 에서 초기화한다.
        QMember m = new QMember("m"); // m은 alias로 사용된다. 동일 테이블을 조인할 경우 바꿔서 사용한다.
        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("Member1")) //querydsl은 preparedStatement에의해 자동으로 파라미터 바인딩 해준다.
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("Member1");
    }

    /** qType을 new연산자 대신 QMember.member로 사용 */
    @Test
    public void qTypeDeclaration() {
        QMember m = QMember.member; //static Import로 사용 가능
        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("Member1")) //querydsl은 preparedStatement에의해 자동으로 파라미터 바인딩 해준다.
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("Member1");
    }

    /** QMember.member 직접 주입 및 스태틱 임포트 */
    @Test
    public void qTypeStaticImport() {
//        QMember m = QMember.member; //static Import로 사용 가능
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("Member1")) //querydsl은 preparedStatement에의해 자동으로 파라미터 바인딩 해준다.
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("Member1");
    }

    /** 검색조건 쿼리
     * eq() / and() / or() / isNotNull() / in() / notIn() / between()
     * like("value%") / contains("value") : '%value%' 검색
     * startWith("value") : 'value%' 검색 / endWith("value") : '%value'
     * goe(value) : column >= value / loe(value) : column <= value
     * gt(value) : column > value / loe(value) : column < value
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

    /**
     * 정렬
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 오름차순(ASC
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("Member5", 100));
        em.persist(new Member("Member6", 100));

        List<Member> fetchResult = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast()) //nullsFirst null을 처음으로 정렬
                .fetch();
        System.out.println("fetchResult = " + fetchResult);
        Member member5 = fetchResult.get(0);
        Member member6 = fetchResult.get(1);
        Member memberNull = fetchResult.get(2);
        assertThat(member5.getUsername()).isEqualTo("Member5");
        assertThat(member6.getUsername()).isEqualTo("Member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    /**
     * 페이징1
     * username 내림차순 정렬 후 2번째 행 부터 2개
     * 결과 : id 가 4, 3 인 데이터 반환
     */
    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(2) // 2번째 행부터
                .limit(2) // 2개 가져와
                .fetch();
        assertThat(result.size()).isEqualTo(2);
        System.out.println("result = " + result);
    }

    /**
     * 페이징2
     * fetchResult로 페이징 정보 조회
     */
    @Test
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(2) // 2번째 행부터
                .limit(2) // 2개 가져와
                .fetchResults();
        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(2);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
        System.out.println("content = " + queryResults.getResults());
    }

}

