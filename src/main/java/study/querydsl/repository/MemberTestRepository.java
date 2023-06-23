package study.querydsl.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.entity.Member;
import study.querydsl.repository.support.Querydsl4RepositorySupport;

import java.util.List;

import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

public class MemberTestRepository extends Querydsl4RepositorySupport {
    public MemberTestRepository() {
        super(Member.class);
    }

    public List<Member> basicSelect() {
        return select(member)
                .from(member)
                .fetch();
    }

    public List<Member> basicSelectFrom() {
        return selectFrom(member)
                .fetch();
    }

    /**
     * Querydsl4RepositorySupport 페이징
     * Querydsl클래스 활용한 순수코드
     * @param condition
     * @param pageable
     * @return
     */
    public Page<Member> searchPageByApplyPage(MemberSearchCondition condition, Pageable pageable) {
        JPAQuery<Member> query = selectFrom(member)
                .join(member.team, team)
                .where(usernameEq(condition.getUsername())
                        , teamNameEq(condition.getTeamName())
                        , ageBetween(condition.getAgeGoe(), condition.getAgeLoe())
                );

        List<Member> content = getQuerydsl().applyPagination(pageable, query).fetch();
        return PageableExecutionUtils.getPage(content, pageable, query::fetchCount);
    }
    /**
     * Querydsl4RepositorySupport 페이징 <br/>
     * applyPagination(pagable, totalQuery) 구현 기능 활용 (코드 단축)
     * @param condition
     * @param pageable
     * @return
     */
    public Page<Member> applyPagenation(MemberSearchCondition condition, Pageable pageable) {
        /**
         * applyPagination(pagable, totalQuery)
         * Querydsl4RepositorySupport에 추가한 기능
         * List<Member> content = getQuerydsl().applyPagination(pageable, query).fetch();
         * PageableExecutionUtils.getPage(content, pageable, query::fetchCount);
         */
        return applyPagination(pageable, query -> query
                .selectFrom(member)
                .join(member.team, team)
                .where(usernameEq(condition.getUsername())
                    , teamNameEq(condition.getTeamName())
                    , ageBetween(condition.getAgeGoe(), condition.getAgeLoe())
                )// where
        ); //applyPagination
    }

    /**
     * Querydsl4RepositorySupport 페이징2 <br/>
     * applyPagination(pagable, contentQuery, countQuery)
     * @param condition
     * @param pageable
     * @return
     */
    public Page<Member> applyPagenation2(MemberSearchCondition condition, Pageable pageable) {

        return applyPagination(pageable
                //ContentQuery
                , contentQuery -> contentQuery
                .selectFrom(member)
                .join(member.team, team)
                .where(usernameEq(condition.getUsername())
                        , teamNameEq(condition.getTeamName())
                        , ageBetween(condition.getAgeGoe(), condition.getAgeLoe())
                )
                //CountQuery
                , countQuery -> countQuery
                .select(member.id)
                .from(member)
                .where(usernameEq(condition.getUsername())
                        , teamNameEq(condition.getTeamName())
                        , ageBetween(condition.getAgeGoe(), condition.getAgeLoe())
                )
        ); //applyPagination
    }

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
