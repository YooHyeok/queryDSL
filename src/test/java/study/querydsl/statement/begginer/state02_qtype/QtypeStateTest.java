package study.querydsl.statement.begginer.state02_qtype;

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
public class QtypeStateTest {
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


}

