package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
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
    void contextLoads() {
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
    public void startJPQL() {
        // member1의 나이
        String qlString = "select m from Member m where m.username = :username";
        Member findByJPQL = em.createQuery(qlString, Member.class)
            .setParameter("username", "member1")
            .getSingleResult();
        
        assertThat(findByJPQL.getAge()).isEqualTo(10);
    }
    
    @Test
    public void startQuerydsl() {
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
    public void search() {
        Member findMember = queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1")
                .and(member.age.eq(10)))
            .fetchOne();
        
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    
    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
            .selectFrom(member)
            .where(
                member.username.eq("member1"),
                member.age.eq(10))
            .fetchOne();
        
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    
    @Test
    public void resultFetch() {
        
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
     * 회원 정렬 순서 1. 회원 나이 내림타순 (desc) 2. 회원 이름 오름차순 (asc) 단 2에서 회원 이름이 없으면 마지막에 출력 (nulls last)
     */
    @Test
    public void sort() {
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
    public void paging1() {
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
    public void paging2() {
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
    public void aggregation() {
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
    public void join() {
        List<Member> result = queryFactory
            .selectFrom(member)
            .join(member.team, team)
            .where(team.name.eq("teamA"))
            .fetch();
        
        assertThat(result)
            .extracting("username")
            .containsExactly("member1", "member2");
    }
    
    /**
     * ex) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조회, 회원은 모두 조회 jpql: select m, t from Member m left join
     * m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering() {
        List<Tuple> result = queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(member.team, team).on(team.name.eq("teamA"))
            .fetch();
        
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    
    /**
     * 세타 조인 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));
        
        List<Member> result = queryFactory
            .select(member)
            .from(member, team)
            .where(member.username.eq(team.name))
            .fetch();
        
        for (Member member : result) {
            System.out.println("member = " + member);
        }
        
        assertThat(result)
            .extracting("username")
            .containsExactly("teamA", "teamB");
    }
    
    /**
     * 연관관계 없는 엔티티 외부 조인 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));
        
        List<Tuple> result = queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(team).on(member.username.eq(team.name))
            .fetch();
        
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    
    @PersistenceUnit
    EntityManagerFactory emf;
    
    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();
        
        Member findMember = queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1"))
            .fetchOne();
        
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }
    
    @Test
    public void fetchJoinUse() {
        em.flush();
        em.clear();
        
        Member findMember = queryFactory
            .selectFrom(member)
            .join(member.team, team).fetchJoin()
            .where(member.username.eq("member1"))
            .fetchOne();
        
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isTrue();
    }
    
    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() {
        QMember memberSub = new QMember("memberSub");
        
        List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.eq(
                JPAExpressions
                    .select(memberSub.age.max())
                    .from(memberSub)
            ))
            .fetch();
        
        assertThat(result).extracting("age").containsExactly(30);
    }
    
    /**
     * 나이가 평균 이상인 회원 조회
     */
    @Test
    public void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");
        
        List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.goe(
                JPAExpressions
                    .select(memberSub.age.avg())
                    .from(memberSub)
            ))
            .fetch();
        
        assertThat(result).extracting("age").containsExactly(20, 30);
    }
    
    @Test
    public void basicCase() {
        List<String> result = queryFactory
            .select(member.age
                .when(10).then("열살")
                .when(20).then("스무살")
                .otherwise("기타"))
            .from(member)
            .fetch();
        
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    
    @Test
    public void complexCase() {
        List<String> result = queryFactory
            .select(new CaseBuilder()
                .when(member.age.between(0, 20)).then("0~20살")
                .when(member.age.between(21, 30)).then("21~30살")
                .otherwise("기타"))
            .from(member)
            .fetch();
        
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    
    @Test
    public void concat() {
        List<String> result = queryFactory
            .select(member.username.concat("_").concat(member.age.stringValue()))
            .from(member)
            .where(member.username.eq("member1"))
            .fetch();
        
        for (String s : result) {
            System.out.println("s = " + s);
        }
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
    
    /**
     * jpql
     */
    @Test
    public void findDtoByJPQL() {
        List<Member> result = em
            .createQuery("select new study.querydsl.entity.Member(m.username, m.age) from Member m",
                Member.class)
            .getResultList();
        
        for (Member m : result) {
            System.out.println("member : " + m);
        }
    }
    
    /**
     * querydsl은 결과를 dto로 반화할 때 3가지 방법을 사용한다. 1. 프로퍼티 접근 - bean : setter를 사용해서 필드에 접근 2. 필드 직접 접근 -
     * fields : setter 필요없이 그냥 바로 필드에 접근 3. 생성자 사용
     */
    @Test//setter로 접근
    public void findDtoBySetter() {
        List<MemberDto> result = queryFactory
            .select(Projections.bean(MemberDto.class,
                member.username,
                member.age))
            .from(member)
            .fetch();
        
        for (MemberDto m : result) {
            System.out.println("member : " + m);
        }
    }
    
    @Test//fields로 접근
    public void findDtoByfields() {
        List<MemberDto> result = queryFactory
            .select(Projections.fields(MemberDto.class,
                member.username,
                member.age))
            .from(member)
            .fetch();
        
        for (MemberDto m : result) {
            System.out.println("member : " + m);
        }
    }
    
    @Test
    public void findDtoByConstructor() {
        List<UserDto> result = queryFactory
            .select(Projections.constructor(UserDto.class,
                member.username.as("name"),
                member.age))
            .from(member)
            .fetch();
        
        for (UserDto m : result) {
            System.out.println("member : " + m);
        }
    }
    
    @Test
    public void findDtoBySubQuery() {
        QMember memberSub = new QMember("memberSub");
        
        List<UserDto> result = queryFactory
            .select(Projections.constructor(UserDto.class,
                member.username.as("name"),
                
                ExpressionUtils.as(JPAExpressions
                    .select(memberSub.age.max())
                    .from(memberSub), "age")
            ))
            .from(member)
            .fetch();
        
        for (UserDto m : result) {
            System.out.println("member : " + m);
        }
    }
    
    /**
     * @QueryProjection을 사용해서 Q타입 dto를 만들면 쿼리 팩토리에서 쿼리를 작성할 때 타입을 검사해준다. 장점 : 컴파일 시점에서 오류를 잡아준다. 단점
     * : 1. Q파일을 하나 만들어야 한다. 2. 멤버 Dto에서 querydsl의존성을 가져야 한다.
     */
    @Test
    public void findDtoByQueryProjection() {
        
        List<MemberDto> result = queryFactory
            .select(new QMemberDto(member.username, member.age))
            .from(member)
            .fetch();
        
        for (MemberDto m : result) {
            System.out.println("memberDto : " + m);
        }
    }
    
    /**
     * 동적 쿼리를 해결하는 2가지 방법 1. booleanBuilder 2. where
     */
    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;
        
        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }
    
    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }
        
        return queryFactory
            .selectFrom(member)
            .where(builder)
            .fetch();
    }
    
    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;
        
        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }
    
    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
            .selectFrom(member)
            .where(allEq(usernameCond, ageCond))
            .fetch();
    }
    
    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }
    
    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }
    
    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }
    
    /**
     * flush는 엔티티 메니저의 sql 저장소에 남아있는 쿼리를 실행시켜준다. 여기서는 바로 디비를 변경하고 엔티티를 변경하지 않았기 때문에 아무런 쿼리를 디비로 보내지
     * 않지만 협업한다는 가정에서 다른 개발자가 작업해놓은 쿼리가 있을지 모르니 flush와 clear를 같이 하는 것이 안전하다
     */
    @Test
    @Commit
    public void bulkUpdate() {
        long count = queryFactory
            .update(member)
            .set(member.username, "dongho")
            .where(member.age.lt(28))
            .execute();
        
        em.flush();
        em.clear();
        
        List<Member> result = queryFactory
            .selectFrom(member)
            .fetch();
        
        for (Member m : result) {
            System.out.println("member : " + m);
        }
    }
    
    @Test
    public void bulkAdd() {
        long count = queryFactory
            .update(member)
            .set(member.age, member.age.add(2))
            .execute();
        
        em.flush();
        em.clear();
        
        List<Member> result = queryFactory
            .selectFrom(member)
            .fetch();
        
        for (Member m : result) {
            System.out.println("age : " + m);
        }
    }
    
    @Test
    public void bulkDelete() {
        long count = queryFactory
            .delete(member)
            .where(member.age.gt(18))
            .execute();
    
        em.flush();
        em.clear();
    
        List<Member> result = queryFactory
            .selectFrom(member)
            .fetch();
    
        for (Member m : result) {
            System.out.println("survivor : " + m);
        }
    }
    
}
