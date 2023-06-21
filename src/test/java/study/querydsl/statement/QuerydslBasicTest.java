package study.querydsl.statement;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
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

    /**
     * 상수 문자 더하기
     * tuple에 담긴 상태로 연산된다.
     * , 구분자가 추가된 형태로 [username, A]와 같이 출력된다.
     */
    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 문자 더하기 concat
     * stringValue() 사용 -> cast(member.age as char)
     * username_age -> Member1_10
     */
    @Test
    public void concat() {
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("Member1"))
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /** -------------------------------------------- 중급 문법 ----------------------------------------------- */

    /**
     * 단일컬럼 프로젝션
     * 프로젝션 : Select할 대상 지정 <br/>
     * 조회할 대상이 하나면 대상 타입으로 지정*/
    @Test
    public void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * Tuple(튜플) 프로젝션
     * 프로젝션 : Select할 대상 지정 <br/>
     * 조회할 대상이 하나면 대상 타입으로 지정*/

    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }


    /**
     * JPQL 프로젝션 DTO
     * new Operation을 사용한다.
     * 풀 패키지명을 기재해야만 한다.
     * 생성자 방식만 지원한다.
     */
    @Test
    public void findDtoByJPQL() {
        List<MemberDto> resultList = em.createQuery(
                "select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m",
                        MemberDto.class
                )
                .getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * queryDsl 프로젝션 DTO - 프로퍼티 접근 <br/>
     * Projections.bean(DTO클래스명.class, qType.field1, qType.field2)
     * @Data에 의한 Setter에 영향을 받아 값을 주입한다.<br/>
     * DTO에 Setter가 없으면 값 주입이 불가능하다.
     */
    @Test
    public void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(
                        Projections.bean(
                                MemberDto.class,
                                member.username,
                                member.age
                        ))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * queryDsl 프로젝션 DTO - 필드 접근 <br/>
     * Projections.field(DTO클래스명.class, qType.field1, qType.field2) <br/>
     * 필드명이 일치하는 필드에 매핑된다. (일치하지 않으면 일치하는 필드만 매핑)
     * @Data에 의한 Constructor나 Setter에 영향을 받지 않고 바로 필드에 값을 주입한다.<br/>
     * DTO에 Setter와 Contructor가 존재하지 않아도 값 주입이 된다. <br/>
     */
    @Test
    public void findMemberDtoByField() {
        List<MemberDto> result = queryFactory
                .select(
                        Projections.fields(
//                                UserDto.class, // username 필드명이 맞지 않아 age에만 값이 매핑됨
                                MemberDto.class,
                                member.username,
                                member.age)
                ).from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * queryDsl 프로젝션 DTO - 필드 접근 <br/>
     * 필드명이 일치하지 않는 필드에 .as()메소드를 사용하여 별칭을 붙혀 매핑할 수 있다.
     * alias 기준은 DTO 필드명으로 기준을잡는다.
     * 서브쿼리에서는 ExpressionUtils.as()를 활용한다.
     * 메소드 매개변수로 JPAExpressions 서브쿼리를 담는다.
     */
    @Test
    public void findUserDtoByField() {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
                .select(
                        Projections.fields(
                                UserDto.class, //필드명이 맞지 않아 age에만 값이 매핑됨
                                member.username.as("name"), //username을 name의 별칭을 붙혀 매핑시킬 수 있다.
                                ExpressionUtils.as(
                                        JPAExpressions
                                                .select(memberSub.age.max())
                                                .from(memberSub)
                                        , "age"
                                )
                        )
                ).from(member)
                .fetch();
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }
    

    /**
     * queryDsl 프로젝션 DTO - 생성자 접근 <br/>
     * Projections.constructor(DTO클래스명.class, qType.field1, qType.field2) <br/>
     * @Data에 의한 Constructor를 통해 값을 주입한다. <br/>
     * DTO에 Constructor가 존재하지 않으면 값을 주입할 수 없다.<br/>
     */
    @Test
    public void findDtoByConstructor() {
        List<UserDto> result = queryFactory
                .select(
                        Projections.constructor(
                                UserDto.class,
                                member.username,
                                member.age)
                ).from(member)
                .fetch();
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * 프로젝션 : @QueryProjection <br/>
     * @QueryProjection : DTO의 생성자를 QType으로 Build한다. <br/>
     * Projection.Constructor처럼 생성자를 사용하는 방식이다. <br/>
     * 장점 : 컴파일 시점에 에러 캐치 (생성자 필드 타입, 갯수 등) <br/>
     * 단점1 : DTO에 애노테이션을 선언하고 compileQuerydsl 빌드 해줘야한다. <br/>
     * 단점2 : 아키텍처 문제(의존관계) 애노테이션을 선언하는 순간 Dto가 QueryDsl에 의존성을 갖게 된다.  <br/>
     * DTO라는것은 기본적으로 값을 반환 받아서 Service나 Controller까지 도달할 수 있도록 해주는 순수한 객체인데,
     * queryDsl에 대한 의존성을 갖게 된다는것이 순수하지 못한 객체가 되어버린다.
     */
    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 동적 쿼리 - BooleanBuilder
     */
    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "Member1";
        Integer ageParam = 10;
        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

//        BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond)); //초기값 세팅 가능.
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) { // 조건이 null이 아니면 조건추가
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }
        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    /**
     * 동적쿼리 - where 다중파라미터
     * 장점1 : 쿼리의 재사용이 가능해진다.
     * 장점2 : 쿼리의 가독성이 좋아진다.
     * 장점3 : 쿼리의 조합이 가능해진다..
     */
    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "Member1";
        Integer ageParam = 10;
        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
//                .where(usernameEq(usernameCond), ageEq(ageCond))
                .where(allAndEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) { // Predicate도 가능
        return usernameCond == null ? null : member.username.eq(usernameCond); // 조건절에 null이 오면 무시된다.
    }
    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond == null ? null : member.age.eq(ageCond);
    }

    /** and연산 */
    private BooleanExpression allAndEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    /** or연산 */
    private BooleanExpression allOrEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).or(ageEq(ageCond));
    }
}

