package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
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
    
        Member member3 = new Member("member3", 30, teamB);
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
         *  1. 자동으로 prepared Connection을 사용하여 쿼리 삽입 공격에 안전하다
         *  2. 컴파일 시점에 오류를 찾아낼 수 있다. -주요 사용 이유-
         */
        
//        QMember m = new QMember("m");
        
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    
    @Test
    public void search(){
        Member findMember = queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1")
                    .and(member.age.eq(10)))
            .fetchOne();
        
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    
    @Test
    public void searchAndParam(){
        Member findMember = queryFactory
            .selectFrom(member)
            .where(
                member.username.eq("member1"),
                member.age.eq(10))
            .fetchOne();
        
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    
    @Test
    public void resultFetch(){
    
        /** fetch()
         *  : 리스트 조회, 데이터 없으면 빈 리스트 반환
         */
        List<Member> fetch = queryFactory
            .selectFrom(member)
            .fetch();
        
        /** fetchOne() : 단 건 조회
         *  결과가 없으면 : null
         *  결과가 둘 이상이면 : com.querydsl.core.NonUniqueResultException
         */
        Member fetchOne = queryFactory
            .selectFrom(member)
            .fetchOne();
        
        /** fetchFirst()
         *  : limit(1).fetchOne()
         */
        Member fetchFirst = queryFactory
            .selectFrom(member)
            .fetchFirst();
        
        /** fetchResults()
         *  : 페이징 정보 포함, total count 쿼리 추가 실행
         *  select count, select ~
         *  1. 페이지를 위한 정보의 갯수와 2. 얻고자 하는 데이터
         *  총 2개의 쿼리를 보내어 데이터를얻는다.
         *
         *  성능이 중요한 페이지에서는 fetchResult를 사용해서는 안된다.
         *  쿼리 도막을 나눠서 따로 보내는 것이 더 좋다.
         */
        QueryResults<Member> results = queryFactory
            .selectFrom(member)
            .fetchResults();
        
        results.getTotal();
        List<Member> content = results.getResults();
        
        long total = queryFactory
            .selectFrom(member)
            .fetchCount();
        
    }
    
    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림타순 (desc)
     * 2. 회원 이름 오름차순 (asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력 (nulls last)
     */
    @Test
    public void sort(){
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));
    
        List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.eq(100))
            .orderBy(member.age.desc(), member.username.asc().nullsLast())
            .fetch();
        
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }
    
    @Test
    public void paging1(){
        /**
         * offset : 몇번째부터 가져올것인가 ex) offset(1) = 두번째부터 가져오겠다.
         */
        List<Member> result = queryFactory
            .selectFrom(member)
            .orderBy(member.username.desc())
            .offset(1)
            .limit(2)
            .fetch();
        
        assertThat(result.size()).isEqualTo(2);
    }
    
    @Test
    public void paging2(){
        QueryResults<Member> result = queryFactory
            .selectFrom(member)
            .orderBy(member.username.desc())
            .offset(1)
            .limit(2)
            .fetchResults();
        
        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(1);
        assertThat(result.getResults().size()).isEqualTo(2);
    }
    
    @Test
    public void aggregation(){
        List<Tuple> result = queryFactory
            .select(
                member.count(),
                member.age.sum(),
                member.age.avg(),
                member.age.max(),
                member.age.min()
            )
            .from(member)
            .fetch();
        
        Tuple tuple = result.get(0);
        
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(70);
        assertThat(tuple.get(member.age.avg())).isEqualTo(17.5);
        
    }
    
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
            .select(team.name, member.age.avg())
            .from(member)
            .join(member.team, team)
            .groupBy(team.name)
            .fetch();
        
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);
        
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(20);
    }
}
