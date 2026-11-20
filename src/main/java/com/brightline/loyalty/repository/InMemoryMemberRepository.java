package com.brightline.loyalty.repository;

import com.brightline.loyalty.domain.GoldTier;
import com.brightline.loyalty.domain.Member;
import com.brightline.loyalty.domain.SilverTier;
import com.brightline.loyalty.domain.StandardTier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryMemberRepository implements MemberRepository {

    private final Map<String, Member> members = new HashMap<>();

    public InMemoryMemberRepository() {
        members.put("m1", new Member("m1", "Kofi Mensah", new StandardTier()));
        members.put("m2", new Member("m2", "Ama Owusu", new GoldTier()));
        members.put("m3", new Member("m3", "Yaa Boateng", new SilverTier()));
    }

    @Override
    public Optional<Member> findById(String id) {
        return Optional.ofNullable(members.get(id));
    }
}
