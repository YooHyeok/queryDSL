package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.repository.MemberJpaRepositroy;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {
    private final MemberJpaRepositroy memberJpaRepositroy;

    /**
     * [검색 조건 조회 API]
     * PostMan URL : http://localhost:8080/v1/members?teamName=teamB&ageGoe=31&ageLoe=35&username=Member31
     * @param condition
     * @return
     */
    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition) {
        return memberJpaRepositroy.searchByWhereCondition(condition);
    }
}
