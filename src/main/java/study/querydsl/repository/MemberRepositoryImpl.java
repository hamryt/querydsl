package study.querydsl.repository;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

/**
 * QuerydslRepositorySupport
 * 장점:
 * 1. EntityManager를 알아서 주입해준다.
 * 2. getQuerydel().applyPagination() 스프링 데이터가 제공하는 페이징을 Querydsl로 편리하게 변환가능(단, sort는 오류 발생할 수 있음)
 *
 * 단점:
 * 1. from으로 시작
 * 2. queryfactory를 제공하지 않음
 * 3. 스프링 데이터 sort 기능이 정상 동작하지 않음
 */

public class MemberRepositoryImpl extends QuerydslRepositorySupport implements MemberRepositoryCustom {
    
    private final JPAQueryFactory queryFactory;
    
    public MemberRepositoryImpl(EntityManager em) {
        super(Member.class);
        this.queryFactory = new JPAQueryFactory(em);
    }
    
    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        
        EntityManager entityManager = getEntityManager();
        
        List<MemberTeamDto> result = from(member)
            .leftJoin(member.team, team)
            .where(usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe())
            )
            .select(new QMemberTeamDto(
                member.id.as("memberId"),
                member.username,
                member.age,
                team.id.as("teamId"),
                team.name.as("teamName")))
            .fetch();
            
        return queryFactory
            .select(new QMemberTeamDto(
                member.id,
                member.username,
                member.age,
                team.id,
                team.name))
            .from(member)
            .leftJoin(member.team, team)
            .where(usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe()))
            .fetch();
    }
    
    /**
     * count 쿼리는 join이 있으면 그것을 사용하지 않고 최적화를 하고 간단히 얻을 수 있는 정보다.
     * FetchResult()는 count와 content를 모두 얻게 해준다. 단점은 쉽게 값을 얻을 수 있는 count쿼리도
     * 본 쿼리가 join이 무더기로 있는 등 복잡하면 똑같은 조건을 붙혀서 count 값을 얻는다. 따라서
     * join과 같은 복잡한 연산을 빼고 count값을 따로 얻는 것이 더 성능에 좋을 것이다.
     *
     * @param condition
     * @param pageable
     * @return
     */
    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition,
        Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
            .select(new QMemberTeamDto(
                member.id,
                member.username,
                member.age,
                team.id,
                team.name))
            .from(member)
            .leftJoin(member.team, team)
            .where(
                usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
        
        long total = queryFactory
            .select(member)
            .from(member)
            .leftJoin(member.team, team)
            .where(
                usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe()))
            .fetchCount();
        
        return new PageImpl<>(content, pageable, total);
    }
    
    public Page<MemberTeamDto> searchPageSimple2(MemberSearchCondition condition,
        Pageable pageable) {
            JPQLQuery<MemberTeamDto> jpaQuery = from(member)
            .leftJoin(member.team, team)
            .where(
                usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe()))
            .select(new QMemberTeamDto(
                    member.id,
                    member.username,
                    member.age,
                    team.id,
                    team.name));
            
        JPQLQuery<MemberTeamDto> query = getQuerydsl().applyPagination(pageable, jpaQuery);
        
        long total = queryFactory
            .select(member)
            .from(member)
            .leftJoin(member.team, team)
            .where(
                usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe()))
            .fetchCount();
        
        return new PageImpl<>(query.fetch(), pageable, total);
    }
    
    /**
     * count쿼리가 생략 가능한 경우
     * 1. 페이지 시작이면서 컨텐츠 사이가 페이지 사이즈보다 작을 때
     * 2. 마지막 페이지 일 때 (offset + 컨텐츠 사이즈를 더해서 전체 사이즈 구함)
     * @param condition
     * @param pageable
     * @return
     */
    
    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition,
        Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
            .select(new QMemberTeamDto(
                member.id,
                member.username,
                member.age,
                team.id,
                team.name))
            .from(member)
            .leftJoin(member.team, team)
            .where(
                usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
    
        JPAQuery<Member> countQuery = queryFactory
            .select(member)
            .from(member)
            .leftJoin(member.team, team)
            .where(
                usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe())
            );
    
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
    }
    
    private Predicate ageLoe(Integer ageLoe) {
        return ageLoe == null ? null : member.age.loe(ageLoe);
    }
    
    private Predicate ageGoe(Integer ageGoe) {
        return ageGoe == null ? null : member.age.goe(ageGoe);
    }
    
    private Predicate teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }
    
    private Predicate usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }
}