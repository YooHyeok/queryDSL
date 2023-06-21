package study.querydsl.statement.middle.state01_projection;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
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
import java.util.List;

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
public class ProjectionStateTest {
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
}

