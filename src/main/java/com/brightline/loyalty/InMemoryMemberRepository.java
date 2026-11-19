package com.brightline.loyalty;

import java.util.HashMap;
import java.util.Map;

public class InMemoryMemberRepository implements MemberRepository {

    private final Map<String, Member> members = new HashMap<>();

    public InMemoryMemberRepository() {
        members.put("m1", new Member("m1", "Kofi Mensah", new StandardTier()));
        members.put("m2", new Member("m2", "Ama Owusu", new GoldTier()));
        members.put("m3", new Member("m3", "Yaa Boateng", new SilverTier()));
    }

    @Override
    public Member findById(String id) {
        return members.get(id); // returns null when the id is not found
    }
}
