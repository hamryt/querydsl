package study.querydsl.repository;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

public class MemberRepositoryImpl implements MemberRepositoryCustom{
    
    private final JPAQueryFactory queryFactory;
    
    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }
    
    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
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
    
    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition,
        Pageable pageable) {
        return null;
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
