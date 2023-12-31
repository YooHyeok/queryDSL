package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.function.LongSupplier;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

public class MemberRepositoryCustomImpl extends QuerydslRepositorySupport implements MemberRepositoryCustom {

    /**
     * 의존성 주입
     * QuerydslRepositorySupport에 대한 주입 추가 (super)
     * -> EntityManager와 querydsl 유틸리티를 함께 사용할 수 있다.
     */
    private final JPAQueryFactory queryFactory;
    public MemberRepositoryCustomImpl() {
        super(Member.class); // QuerydslRepositorySupport에 대한 주입 추가
        this.queryFactory = new JPAQueryFactory(getEntityManager());
    }

    /** 동적 쿼리 - Builder 사용 */
    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        /**
         * QuerydslRepositorySupport 사용 예시 코드
         */
        List<MemberTeamDto> result = from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageBetween(condition.getAgeGoe(), condition.getAgeLoe())
                ).
                select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .fetch();
        EntityManager em = getEntityManager();

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
     * [queryDsl - 페이징] QuerydslRepositorySupport <br/>
     * 장점 <br/>
     * getQuerydsl().applyPagination()을 통해 스프링데이터가 제공하는 페이징을 Querydsl로 편리하게 변환 가능(단, Sort오류는 발생)<br/>
     * 단점 <br/>
     * from()으로 시작 가능(최근에는 queryFactory를 사용해서 select로 시작하는것이 더 명시적) <br/>
     * -> Querydsl 3.x버전을 대상으로 만들었기 때문에 4.x버전의 JPAQueryFactory 즉, Select로 시작할 수 없다(From부터시작) <br/>
     * -> QueryFactory를 제공하지 않으며, 스프링 데이터 Sort기능이 정상 동작하지 않는다.
     * -> (스프링 데이터의 QSort를 억지로넘기면 동작하긴 하지만 권장하지 않는다)
     * @param condition
     * @param pageable
     * @return
     */
    @Override
    public Page<MemberTeamDto> searchPageSimple2(MemberSearchCondition condition, Pageable pageable) {

        JPQLQuery<MemberTeamDto> jpaQuery = from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageBetween(condition.getAgeGoe(), condition.getAgeLoe())
                ).select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ));

        QueryResults<MemberTeamDto> pageResults =
                getQuerydsl().applyPagination(pageable, jpaQuery)// offset, limit을 제공해준다.
                .fetchResults();

        List<MemberTeamDto> content = pageResults.getResults();
        long total = pageResults.getTotal();
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

        JPAQuery<Member> countQuery = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageBetween(condition.getAgeGoe(), condition.getAgeLoe())
                );
//                .fetchCount(); //Count용쿼리
        /**
         * [PagebleExcutionUtils.getPage()] <br/>
         * count 쿼리 생략 가능한 경우 생략해서 처리 <br/>
         * 페이지 시작이면서 컨텐츠 사이즈가 페이지 사이즈보다 작을 때 <br/>
         * 마지막 페이지 일 때(offset + 컨텐츠 사이즈를 더해서 전체 사이즈를 구한다) <br/>
         * # 페이지 사이즈 : 페이지에 담을 row개수 <br/>
         * -> 첫번째 페이지(100페이지) <br/>
         * -> 컨텐츠 사이즈가 페이지 사이즈 보다 작다.(쿼리 실행시 데이터 3개) <br/>
         * -> 3개를 totalCount로 사용 가능
         */
//        return PageableExecutionUtils.getPage(content, pageable, () -> countQuery.fetchCount());
//        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
        return PageableExecutionUtils.getPage(content, pageable, new LongSupplier(){
            @Override
            public long getAsLong() {
                return countQuery.fetchCount();
            }
        });
//        return new PageImpl<>(content, pageable, total); //content, pageable, total 정보 page구현체 객체에 담아 모두반환
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
