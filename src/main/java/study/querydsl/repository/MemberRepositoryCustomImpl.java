package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

import javax.persistence.EntityManager;
import java.util.List;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

public class MemberRepositoryCustomImpl implements MemberRepositoryCustom {

    /**
     * 의존성 주입
     */
    private final JPAQueryFactory queryFactory;
    public MemberRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    /** 동적 쿼리 - Builder 사용 */
    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
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
                        ageBetween(condition.getAgeGoe(),condition.getAgeLoe())
                )
                .fetch();
    }

    /**
     * [queryDsl - 페이징] content, count 통합 <br/>
     * 데이터가 별로 없을경우 사용 <br/>
     * fetchResults()에 의해 content와 count 쿼리를 동시에 날림 <br/>
     * result객체로 부터 content와 count를 추출하여 Page인터페이스 구현 객체에 담아 return.
     * @param condition
     * @param pageable
     * @return
     */
    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> pageResult = queryFactory
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
                        ageBetween(condition.getAgeGoe(), condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();//Conten용쿼리 Count용쿼리 각각 2번 출력
        List<MemberTeamDto> content = pageResult.getResults();
        long total = pageResult.getTotal();
        return new PageImpl<>(content, pageable, total); //content, pageable, total 정보 page구현체 객체에 담아 모두반환
    }

    /**
     * [queryDsl - 페이징] content, count 분리 (몇천만건의 조회 최적화)<br/>
     * Count쿼리의 경우 특정 상황에는 Join이 필요하지 않을 수 있다. <br/>
     * 1) 심플화 되어있거나, <br/>
     * 2) CountData가 Db에 저장되어있거나, <br/>
     * 3) join을 줄이더라도 content쿼리와 count쿼리가 차이가 없거나 <br/>
     * 4) count 쿼리를 먼저 실행 했는데 카운트가 발생하지 않을 경우 content조회를 하지 않는다거나
     * @param condition
     * @param pageable
     * @return
     */
    @Override
    public Page<MemberTeamDto> searchComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
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
                        ageBetween(condition.getAgeGoe(), condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();//Conten용쿼리

        long total = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageBetween(condition.getAgeGoe(), condition.getAgeLoe())
                ).fetchCount(); //Count용쿼리

        return new PageImpl<>(content, pageable, total); //content, pageable, total 정보 page구현체 객체에 담아 모두반환
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
