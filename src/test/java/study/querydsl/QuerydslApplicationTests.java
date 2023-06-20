package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Hello;
import study.querydsl.entity.QHello;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

//	@Autowired //스프링 환경에서 동작 가능
	@PersistenceContext //스프링을 포함한 다른 컨테이너의경우 사용
	EntityManager em;
	@Test
	void contextLoads() {
		Hello hello = new Hello();
		em.persist(hello);

		JPAQueryFactory query = new JPAQueryFactory(em); //최신 버전에서 권장
//		QHello qHello = new QHello("h");
		QHello qHello = QHello.hello; // QHello.java에 static으로 선언됨
		Hello result = query
						.selectFrom(qHello)
						.fetchOne();

		assertThat(result).isEqualTo(result);
		assertThat(result.getId()).isEqualTo(hello.getId());
	}
}
