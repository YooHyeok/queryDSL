package study.querydsl.dto;

import lombok.Data;

/**
 * 동적 쿼리와 성능 최적화 조회 - 검색 Condition VO 클래스
 */
@Data
public class MemberSearchCondition {
    //회원명, 팀명, 나이(ageGoe, ageLoe)
    private String username;
    private String teamName;
    private Integer ageGoe;
    private Integer ageLoe;
}
