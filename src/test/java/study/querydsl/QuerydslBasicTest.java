package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;
    
    JPAQueryFactory queryFactory;
    
    @BeforeEach
    void contextLoads(){
        queryFactory = new JPAQueryFactory(em);
        
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        
        em.persist(teamA);
        em.persist(teamB);
    
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
    
        Member member3 = new Member("member3", 10, teamB);
        Member member4 = new Member("member4", 10, teamB);
    
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }
    
    @Test
    public void startJPQL(){
        // member1의 나이
        String qlString = "select m from Member m where m.username = :username";
        Member findByJPQL = em.createQuery(qlString, Member.class)
            .setParameter("username", "member1")
            .getSingleResult();
        
        assertThat(findByJPQL.getAge()).isEqualTo(10);
    }
    
    @Test
    public void startQuerydsl(){
        /** 쿼리dsl의 장점
         *  1.자동으로 prepared Connection을 사용하여 쿼리 삽입 공격에 안전하다
         *  2. 컴파일 시점에 오류를 찾아낼 수 있다. -주요 사용 이유-
         */
        
        QMember m = new QMember("m");
        
        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();
        
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
}
