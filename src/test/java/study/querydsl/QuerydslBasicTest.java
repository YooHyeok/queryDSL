package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
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
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

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

    /**
     * 집합(Tuple) : select 조회시 여러 데이터 타입을 조회하는 경우 tuple을 사용한다. (실무에서는 dto로 추출한다)
     * Tuple인터페이스는 Querydsl 패키지 소속 튜플이다.
     */
    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(), // 총개수
                        member.age.sum(), // 총합계
                        member.age.avg(), // 평균
                        member.age.max(), // 최대값
                        member.age.min() // 최소값
                )
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }

    /**
     * GroupBy / having
     * 팀의 이름과 각 팀의 평균 연령을 구하라.
     */
    @Test
    public void group() {
        List<Tuple> result = queryFactory
                .select(
                        team.name,
                        member.age.avg()
                )
                .from(member)
                .join(member.team, team)
                .groupBy(team.name) // 2. team.name으로 그룹핑해라.
                .having(team.id.lt(10)) // 1. having team의 id가 10보다 작은 데이터들 중
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); //Team A는 10살 20살이 존재한다.
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35); //Team B는 30살 40살이 존재한다.
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

