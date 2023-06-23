package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

import java.util.List;

/**
 * SpringDataJpaRepository
 * 스프링 데이터 JPA에서 QueryDsl을 적용하기 위한 repository
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * SpringDataJPA 쿼리메소드 <br/>
     * 회원 전체조회 : select m from Member m where m.username = :username;
     * @param username
     * @return List<Member> 회원 리스트
     */
    List<Member> findByUsername(String username);
}
