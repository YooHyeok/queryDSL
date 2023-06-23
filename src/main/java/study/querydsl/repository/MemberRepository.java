package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import study.querydsl.entity.Member;

import java.util.List;

/**
 * SpringDataJpaRepository <br/>
 * 스프링 데이터 JPA에서 QueryDsl을 적용하기 위한 repository <br/>
 * QueryDsl을 구현한 구현체로 implementation한 대상 인터페이스를 상속받는다. (MemberRepositoryCustom) <br/>
 * 구현체인 MemberRepositoryCustom을 implementation할 클래스를 생성한 뒤 메소드를 Override하여 구현한다. <br/>
 * <br/>
 * QuerydslPredicateExecutor 기능 추가 <br/>
 * 스프링데이터JPA 기본 지원 쿼리메소드에 QueryDsl문법을 부여하여 검색조건을 만들 수 있다. <br/>
 * (Pageable, Sort 모두 지원하고, 정상적으로 동작한다.)<br/>
 * ***한계점*** <br/>
 * -> JOIN이 되지 않는다.(묵시적 조인은 가능하지만 Left Join 불가능) <br/>
 * -> 클라이언트(Repository)가 Querydsl에 의존 -> 서비스 클래스가 Querydsl이라는 구현기술에 의존해야 한다. <br/>
 * -> 복잡한 실무 환경에서 사용하기에는 한계가 명확하다.(실무 권장 X)
 */
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom, QuerydslPredicateExecutor<Member> {

    /**
     * SpringDataJPA 쿼리메소드 <br/>
     * 회원 전체조회 : select m from Member m where m.username = :username;
     * @param username
     * @return List<Member> 회원 리스트
     */
    List<Member> findByUsername(String username);
}
