package com.brightline.loyalty.repository;

import com.brightline.loyalty.domain.Member;

import java.util.Optional;

public interface MemberRepository {
    Optional<Member> findById(String id);
}
