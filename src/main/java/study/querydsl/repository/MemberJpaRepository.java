package study.querydsl.repository;

import static org.springframework.util.StringUtils.hasText;
import static org.springframework.util.StringUtils.isEmpty;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QTeam;

@Repository
public class MemberJpaRepository {
    
    private final EntityManager em;
    private final JPAQueryFactory queryFactory;
    
    public MemberJpaRepository(EntityManager em, JPAQueryFactory jpaQueryFactory){
        this.em = em;
        this.queryFactory = jpaQueryFactory;
    }
    
    public void save(Member member){
        em.persist(member);
    }
    
    public Optional<Member> findById(Long id){
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }
    
    public List<Member> findAll(){
        return em.createQuery("select m from Member m", Member.class)
            .getResultList();
    }
    
    public List<Member> findAll_Querydsl(){
        return queryFactory
            .selectFrom(member)
            .fetch();
    }
    
    public List<Member> findByUsername(String username){
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
            .setParameter("username", username)
            .getResultList();
    }
    
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition){
    
        BooleanBuilder builder = new BooleanBuilder();
        if(hasText(condition.getUsername())){
            builder.and(member.username.eq(condition.getUsername()));
        }
        
        if(hasText(condition.getTeamName())){
            builder.and(team.name.eq(condition.getTeamName()));
        }
        
        if(condition.getAgeGoe() != null){
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        
        if(condition.getAgeLoe() != null){
            builder.and(member.age.loe(condition.getAgeLoe()));
        }
        
        return queryFactory
            .select(new QMemberTeamDto(
                member.id,
                member.username,
                member.age,
                team.id,
                team.name))
            .from(member)
            .leftJoin(member.team, team)
            .where(builder)
            .fetch();
    }
    
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
































