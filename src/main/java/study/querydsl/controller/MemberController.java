package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.repository.MemberJpaRepositroy;
import study.querydsl.repository.MemberRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {
    private final MemberJpaRepositroy memberJpaRepositroy;
    private final MemberRepository memberRepository;

    /**
     * [검색 조건 조회 API] - 순수 JPA 기반 쿼리 DSL 적용 <br/>
     * PostMan URL : http://localhost:8080/v1/members?teamName=teamB&ageGoe=31&ageLoe=35&username=Member31
     * @param condition
     * @return
     */
    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition) {
        return memberJpaRepositroy.searchByWhereCondition(condition);
    }

    /**
     * [검색 조건 및 페이징 조회 API] - 스프링데이터JPA 기반 쿼리DSL적용 <br/>
     * content,count 통합처리
     * PostMan URL : http://localhost:8080/v2/members?page=1&size=5
     * @param condition
     * @param pageable
     * @return
     */
    @GetMapping("/v2/members")
    public Page<MemberTeamDto> searchMemberV2(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchPageSimple(condition, pageable);
    }

    /**
     * [검색 조건 및 페이징 조회 API] - 스프링데이터JPA 기반 쿼리DSL적용 <br/>
     * content,count 분리 및 최적화 처리
     * PostMan URL : http://localhost:8080/v3/members?page=1&size=5
     * @param condition
     * @param pageable
     * @return
     */
    @GetMapping("/v3/members")
    public Page<MemberTeamDto> searchMemberV3(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchComplex(condition, pageable);
    }
}
