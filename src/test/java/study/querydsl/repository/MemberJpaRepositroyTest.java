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

}