package com.brightline.loyalty;

import com.brightline.loyalty.exception.MemberNotFoundException;
import com.brightline.loyalty.points.BasePointsRule;
import com.brightline.loyalty.points.BigSpenderBonusRule;
import com.brightline.loyalty.points.PointsCalculator;
import com.brightline.loyalty.repository.InMemoryMemberRepository;

public class Demo {
    public static void main(String[] args) {
        PointsCalculator calculator = new PointsCalculator(
                new InMemoryMemberRepository(), new BasePointsRule(), new BigSpenderBonusRule());

        System.out.println("Result m1 (STANDARD), GHS 100  -> " + calculator.calculatePoints("m1", 100));
        System.out.println("Result m2 (GOLD), GHS 250      -> " + calculator.calculatePoints("m2", 250));

        try {
            System.out.println("Result unknown member, GHS 100 -> "
                    + calculator.calculatePoints("unknown", 100));
        } catch (MemberNotFoundException e) {
            System.out.println("Result unknown member, GHS 100 -> " + e.getMessage());
        }
    }
}
