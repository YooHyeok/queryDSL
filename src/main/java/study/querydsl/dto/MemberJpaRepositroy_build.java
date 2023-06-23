package study.querydsl.dto;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

/**
 * [순수 JPA 리포지토리] <br/>
 * git에 올라가지 않으므로 저장함. <br/>
 * _build제거 후 build_generated_querydsl_study_querydsl_repository로 이동시켜준다.
 */
@Repository
public class MemberJpaRepositroy_build {
    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

//    public MemberJpaRepositroy(EntityManager em) { // 순수 자바 의존성 주입
    public MemberJpaRepositroy_build(EntityManager em, JPAQueryFactory queryFactory) { // 스프링 컨테이너에 빈 등록후 의존성 주입
        this.em = em;
        this.queryFactory = queryFactory;
    }
    /** 회원 저장 (EntityManager) */
    public void save(Member member) {
        em.persist(member);
    }
    /** 회원 단건 조회 (EntityManager) */
    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }
    /** 회원 전체 조회 (JPQL) */
    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }
    /** 회원이름으로 조건 조회 (JPQL) */
    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }
    /** 회원 전체 조회 (querydsl) */
    public List<Member> findAll_Querydsl() {
        return queryFactory
                .selectFrom(member)
                .fetch();
    }
    /** 회원이름으로 조건 조회 (querydsl) */
    public List<Member> findByUsername_Querydsl(String username) {
        return queryFactory.selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }
    /** 동적 쿼리 - Builder 사용 */
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(condition.getUsername())) { //StringUtils.hasText()는 null과 "" 모두 처리 가능
            builder.and(member.username.eq(condition.getUsername()));
        }
        if (StringUtils.hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }
        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }
    /** 동적 쿼리 - Builder 사용 */
    public List<MemberTeamDto> searchByWhereCondition(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
//                        ageGoe(condition.getAgeGoe()),
//                        ageLoe(condition.getAgeLoe())
                        ageBetween(condition.getAgeLoe(),condition.getAgeGoe())
                )
                .fetch();
    }

    /**
     * ageGoe와 ageLoe 조합
     * @param ageLoe
     * @param ageGoe
     * @return BooleanExpression : Predicate와는 다르게 컴포지션이 가능해진다.
     */
    private BooleanExpression ageBetween(Integer ageGoe, Integer ageLoe) {
        if (ageGoe == null) {
            return ageLoe(ageLoe);
        }
        if (ageLoe == null) {
            return ageGoe(ageGoe);
        }
        return ageGoe(ageGoe).and(ageLoe(ageLoe));
    }
    private BooleanExpression usernameEq(String username) {
        return StringUtils.hasText(username) ? member.username.eq(username): null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return StringUtils.hasText(teamName) ? team.name.eq(teamName): null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

}
