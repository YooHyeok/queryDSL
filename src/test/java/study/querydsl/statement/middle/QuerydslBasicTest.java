package study.querydsl.statement.middle;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
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
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
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

    /**
     * 벌크연산(update)
     */
    @Test
    @Commit
    public void bulkUpdate() {
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28)) //28살 미만 회원에 대해서 이름을 비회원으로 변경
                .execute();
        em.clear(); // 영속성 1차캐시 초기화
        /**
         * [영속성 컨텍스트]
         * Member1 = 10 -> Member1
         * Member2 = 20 -> Member2
         * Member3 = 30 -> Member3
         * Member4 = 40 -> Member4
         *
         * [bulk연산 - DB] : 영속성컨텍스트를 건너뛰고 DB에 바로 퀴리 반영
         * Member1 = 10 -> 비회원
         * Member2 = 20 -> 비회원
         * Member3 = 30 -> Member3
         * Member4 = 40 -> Member4
         *
         * Execute - 반영할때는 영속성컨텍스트를 무시하고 DB에 반영해 버리지만,
         * Select - 불러올때는 영속성컨텍스트가 항상 우선권을 가진다.
         * 다시 말해 조회해온 결과를 캐시에 엎어 쓰지 않는다.
         * 이것은 JPQL에서도 동일하게 적용된다.
         * 따라서 벌크연산 후에는 반드시 1차캐시를 초기화 해주는게 좋다.
         */
        List<Member> result = queryFactory.selectFrom(member).fetch();
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    /**
     * 벌크연산(update)
     * add(덧,뺄셈), multiply(곱하기)
     */
    @Test
    public void bulkAdd() {
        long execute = queryFactory
                .update(member)
//                .set(member.age, member.age.add(1)) // 모든 회원의 나이를 1씩 더한다.
//                .set(member.age, member.age.add(-1)) // 빼기의 경우 정수 앞에 - 를 추가하면 된다
                .set(member.age, member.age.multiply(-1)) // 곱하기
                .execute();

        em.clear(); //1차캐시 초기화

        System.out.println("execute = " + execute);
        List<Member> result = queryFactory.selectFrom(member).fetch();
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    /**
     * 벌크연산 (삭제)
     */
    @Test
    public void bulkDelete() {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
        em.clear();
        List<Member> result = queryFactory.selectFrom(member).fetch();
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    /**
     * SQL Function() 호출 (사용자정의함수)
     * username의 member라는 이름을 M으로 바꾼다.
     */
    @Test
    public void sqlFunction() {
        List<String> result = queryFactory
                .select(
                        Expressions
                                .stringTemplate("function('replace', {0}, {1}, {2})", member.username, "Member", "M") //와일드카드 사용 불가능
                ).from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
            
        }
    }

    /**
     * SQL Function() 호출 (Ansi 표준)
     * username을 소문자로 변경한 뒤 동등조건으로 비교한다.
     */
    @Test
    public void sqlFunction2() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower', {0})", member.username) //사용자정의 Function으로 H2Dialect로부터 상속받은 클래스에 등록해서 사용한다.
                        member.username.lower() //ANSI 표준은 그냥 쓴다. (앞선 예제의 replace도 가능)
                ))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}

