package study.querydsl.statement.middle.state03_bulkoperation;

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
public class BulkOperationStateTest {
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
}

