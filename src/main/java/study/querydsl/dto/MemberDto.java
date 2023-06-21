package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
//@NoArgsConstructor
public class MemberDto {
    private String username;
    private int age;

    public MemberDto() {
    }

    @QueryProjection //Gradle -> CompileQuerydsl적용 : DTO의 생성자도 Q파일로 생성된다.
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
