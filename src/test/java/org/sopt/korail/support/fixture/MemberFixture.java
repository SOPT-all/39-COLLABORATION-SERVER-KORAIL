package org.sopt.korail.support.fixture;

import org.sopt.korail.member.domain.Member;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

public final class MemberFixture {
    public static final Long MEMBER_ID = 1L;
    public static final Long OTHER_MEMBER_ID = 2L;

    private MemberFixture() {
    }

    public static Member member() {
        Member member = BeanUtils.instantiateClass(Member.class);
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        return member;
    }
}
