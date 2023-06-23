package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

import java.util.List;

/**
 * SpringDataJpaRepository <br/>
 * 스프링 데이터 JPA에서 QueryDsl을 적용하기 위한 repository <br/>
 * QueryDsl을 구현한 구현체로 implementation한 대상 인터페이스를 상속받는다. (MemberRepositoryCustom) <br/>
 * 구현체인 MemberRepositoryCustom을 implementation할 클래스를 생성한 뒤 메소드를 Override하여 구현한다.
 *
 */
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom{

    /**
     * SpringDataJPA 쿼리메소드 <br/>
     * 회원 전체조회 : select m from Member m where m.username = :username;
     * @param username
     * @return List<Member> 회원 리스트
     */
    List<Member> findByUsername(String username);
}
