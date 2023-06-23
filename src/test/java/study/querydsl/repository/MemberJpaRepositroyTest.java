package study.querydsl.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositroyTest {
    @Autowired
    EntityManager em;

    @Autowired MemberJpaRepositroy memberJpaRepositroy;

    /**
     * 순수 JPA 테스트 <br/>
     * jpql과 queryDsl
     */
    @Test
    public void basicTest() {
        Member member = new Member("member1", 10);
        memberJpaRepositroy.save(member);
        Member findMember = memberJpaRepositroy.findById(member.getId()).get();
        // jpql테스트
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberJpaRepositroy.findAll();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = memberJpaRepositroy.findByUsername("member1");
        assertThat(result2).containsExactly(member);

        // queryDsl테스트
        List<Member> result3 = memberJpaRepositroy.findAll_Querydsl();
        assertThat(result3).containsExactly(member);

        List<Member> result4 = memberJpaRepositroy.findByUsername_Querydsl("member1");
        assertThat(result4).containsExactly(member);
    }

    /**
     * 동적쿼리와 성능 최적화 조회 - Builder
     */
    @Test
    public void  searchTest() {
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

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(20);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");
        List<MemberTeamDto> result = memberJpaRepositroy.searchByBuilder(condition);
        assertThat(result).extracting("username").containsExactly("Member3","Member4");
    }
}