# Brightline Loyalty: refactoring and test rationale

This repository holds the Brightline points calculator after it was brought up to standard from AI-generated starter code. `ANALYSIS.md` records what was wrong before any production code changed. This file records what was done about it and why.

## How to run

| Task | Command |
|---|---|
| Build, run all tests, coverage report, mutation gate | `mvn clean verify` |
| Mutation testing on its own | `mvn clean test-compile org.pitest:pitest-maven:mutationCoverage` |
| Coverage report | `target/site/jacoco/index.html` after `mvn verify` |
| Mutation report | newest folder under `target/pit-reports`, open `index.html` |
| Sample runner | run `Demo.main` from the IDE |

Requires JDK 21 and Maven. `mvn clean verify` takes about a minute because PIT runs inside it.

## Structure

| Package | Classes | Depends on |
|---|---|---|
| `com.brightline.loyalty` | `Demo` (composition root: creates and wires the objects) | all packages |
| `...domain` | `Member` (record), `Tier`, `StandardTier`, `SilverTier`, `GoldTier`, `PlatinumTier` | nothing |
| `...exception` | `MemberNotFoundException`, `InvalidPurchaseException` | nothing |
| `...repository` | `MemberRepository`, `InMemoryMemberRepository` | `domain` |
| `...points` | `PointsCalculator`, `BasePointsRule`, `BigSpenderBonusRule` | `domain`, `repository`, `exception` |

Dependencies run one way. Classes were grouped by role after the responsibilities were separated, in a move-only commit (`refactor: group classes into packages by role`). The exceptions have their own package because they belong to the calculator's public contract and are not domain concepts. `Demo` stays in the root package because it depends on every other package and nothing depends on it.

## The rule the calculator implements

```
points = base points, rounded down (one point per GHS 10)
         -> tier rule applied (STANDARD x1, SILVER x1.5 rounded down, GOLD x2)
         -> plus a flat 50 if the amount is above GHS 500
```

## Key decisions

**Tier is an interface, not an enum.** Each tier is a class that owns its multiplier (`Tier.applyTo(int basePoints)`), and `Member` holds a `Tier` object. An enum would also remove the string typos, but each new tier would edit an existing file. Using both an enum and a rule interface would be two parallel structures. Moving the logic into `Member` would fix Feature Envy but keep the if/else chain.

**Tier type and rule chain were one change.** Because the tier is the rule, replacing the tier string removed the if/else chain in the same commit. A later check confirmed that nothing else branches on tier. No selector, factory or registry was added, since nothing needs to look a tier up by name.

**Failure model.** Absence is expected at the repository, so `findById` returns `Optional<Member>`. The calculator decides absence is an error for this operation and throws `MemberNotFoundException` carrying the id. A blank id, or a negative, NaN or infinite amount, throws `InvalidPurchaseException`. Points that do not fit in an `int` are rejected with `Math.toIntExact`, `Math.multiplyExact` and `Math.addExact`; the `ArithmeticException` is wrapped as the cause. The `try` wraps only the arithmetic, so nothing else is caught.

Both exceptions are unchecked because no caller can recover from a malformed purchase or an unknown id, and checked exceptions would force a `throws` clause through every caller that can only report the error. A repository failure propagates unchanged because the calculator can neither recover from it nor add context. The amount stays a `double`, validated at the calculator's entry, so the primitive-obsession finding is closed for the tier and left open for the amount.

**Responsibilities.** `BasePointsRule` and `BigSpenderBonusRule` are small concrete classes with no interface, because only the tier is expected to vary. All three collaborators are injected through the constructor, which only assigns fields. Printing was removed from the calculator, and `Demo` prints the returned value.

**`Member` is a record** with a null-tier check in its compact constructor. The check guards an invariant of a value object so an invalid `Member` cannot exist. A strict reading of "constructors only assign fields" could object to it, and I kept it deliberately.

**Business rules.** Behaviour was pinned first by characterization tests, refactored with the output unchanged, and changed only where a decision said so.

| Rule | Decision | Behaviour change? |
|---|---|---|
| Bonus threshold | Kept `> 500`, so exactly 500 earns no bonus. The brief is silent on whether the threshold is inclusive. Pinned by tests at 499.99, 500 and 500.01. Open question for the product owner; changing it is one operator and five test rows. | No |
| Bonus order | Flat +50 added after the tier rule, never multiplied | No |
| Rounding | Points are whole numbers, rounded down after the base calculation and after the SILVER multiplier. The two-decimal convention applies to money amounts, not points. Half-up is the usual rule for currency, but points have no convention, so the existing round-down was kept. | No |
| Unknown member | Throws `MemberNotFoundException` | Yes (was 0) |
| Invalid amount | Throws `InvalidPurchaseException`. Zero is valid and earns 0. | Yes |
| Overflow | Rejected, with the cause preserved | Yes (was a wrapped negative number) |
| Member id | Exact match, no trimming or case folding. Null or blank is rejected. | Yes (null or blank) |
| Printing | Removed from the calculator | Yes |

## How the design absorbs the new tier

`PlatinumTier` was added as an example (multiplier 3, chosen for the proof; the business has not set Platinum rules). The change was **3 files added and 0 existing files modified**: `PlatinumTier`, `PlatinumTierTest` and `PlatinumMemberPointsTest`. `PointsCalculator`, `Member`, `Tier` and the rule classes are untouched.

Caveat: to give the running application a Platinum member, the seed data in `InMemoryMemberRepository` needs one line. That is data, not rule logic, and a real repository would read it from storage. The test uses a mocked repository to supply a Platinum member to the real calculator.

## What I kept, fixed and rewrote

| Item | Outcome | Reason |
|---|---|---|
| Rule arithmetic (divide by 10, multipliers 1, 1.5, 2, flat +50 after) | Kept | Pinned by tests before refactoring |
| `MemberRepository` interface and seed members m1, m2, m3 | Kept, return type changed to `Optional` | The seam was already right; the null contract was not |
| `Demo` scenarios | Kept, rewired | Now builds the object graph and handles the unknown-member failure |
| `Member` | Fixed | Tier is a `Tier`, and the class became a record |
| `PointsCalculator` | Rewrote | Built its own repository, swallowed every exception, printed, and held every rule |
| If/else chain on tier strings | Rewrote | Replaced by one class per tier |
| `awardsPointsToAMember` | Deleted | Asserted only `points > 0` |
| `calculatesGoldPoints` | Deleted | Mocked the class under test and asserted the mock |
| `awardsPointsForAPurchase` | Deleted | `assertNotNull` on a primitive `int` cannot fail |

The three generated tests were removed only after every behaviour they claimed was pinned by exact-value tests.

## Tests

90 tests, all passing.

| Class | Tests | Style |
|---|---|---|
| `PointsCalculatorCharacterizationTest` | 18 | Sociable, real repository and rules, tier by tier |
| `PointsCalculatorBoundaryTest` | 16 | Parameterized: 499, 500, 501 for every tier, rounding edges, zero |
| `PointsCalculatorFailureTest` | 16 | Message and cause assertions, `Integer.MAX_VALUE` boundary |
| `PointsCalculatorSolitaryTest` | 6 | Mocked repository, including one that throws and a tier defined by the test |
| `TierTest`, `PlatinumTierTest` | 15, 5 | Each tier directly, including overflow |
| `BasePointsRuleTest`, `BigSpenderBonusRuleTest` | 7, 5 | Rules in isolation |
| `PlatinumMemberPointsTest` | 1 | New tier through the real calculator |
| `MemberTest` | 1 | Null tier rejected |

Every expected value was worked out by hand from the rule, not copied from program output. `verify()` appears only after a value assertion (§6.2).

### Mutation results

| Measure | Starter code | Now |
|---|---|---|
| Mutants killed | 2 of 19 project-wide; 1 of 12 in `PointsCalculator` | 21 of 21 |
| PIT line coverage | 68% (25/37) | 100% (41/41) |
| Test strength | 17% | 100% |
| Survivors | 10 survived, 7 not covered | 0 |

The two runs are not like-for-like: the code was restructured in between and `Demo` is now excluded from PIT because it is a runner with no logic. The old equivalent mutant (`points * 1` to `points / 1`) no longer exists because the no-op branch was deleted.

JaCoCo on the final code: 100% line and branch coverage on every class except `Demo` (a runner, 10 lines, no test). Branches: 12 of 12.

Limits of the score. PIT does not mutate calls such as `Math.addExact` or `Objects.requireNonNull`, and the PIT report lists no mutants for `Member` or `InvalidPurchaseException`. I covered those with hand mutations, each of which failed the expected test:

| # | Mutation | Result |
|---|---|---|
| 1 | `amount < 0` to `<= 0` | Failed as expected |
| 2 | NaN check removed | Failed as expected |
| 3 | `Math.addExact` to `+` | Failed as expected |
| 4 | cause dropped from the rethrown exception | Failed as expected |
| 5 | `orElseThrow` to `.get()` | Failed as expected |

Before the rewrite, no generated test read a SILVER result, and PIT's line 20 mutant (`* 1.5` to `/ 1.5`) survived all three. After the characterization tests were written, changing the SILVER multiplier from 1.5 to 3 failed all five SILVER rows.

## The one improvement: a mutation gate in the build

`mutationThreshold` is set to 100 and PIT is bound to the `verify` phase, so `mvn verify` fails when a mutant survives.

- **Value for this code:** the original suite was green while catching one bug in nine. Without a gate, a later change can add an unchecked branch and nothing fails. The gate blocks tests that run code without checking the result.
- **Why 100:** the score is 100 today, so any drop is a regression. If an equivalent mutant ever appears, the threshold can be lowered with the reason recorded.
- **Limits:** `Demo` is excluded, PIT skips some calls (see above), and the build takes longer.
- **Evidence it fails:** with `PlatinumTierTest` and `PlatinumMemberPointsTest` removed, all 84 remaining tests passed, but PIT reported 1 mutant with no coverage (the `PlatinumTier` return value) and 20 of 21 killed, below the threshold of 100. With the files restored, 21 of 21 were killed and the build passed.

## What the AI got wrong

### In the starter code and tests

- A `catch (Exception)` that returns 0, so every failure looked like a legitimate result.
- A dependency built with `new` inside the class, which made the failure paths untestable.
- A test that mocks the class under test, a test with `assertNotNull` on a primitive, and a test whose only check was `points > 0`.
- Wildcard imports and comments that restate the assertion (`// it awarded some points`) in the generated test.
- Money as `double`, tiers as strings, magic numbers, and a calculation method that prints.
- Happy-path-only tests. None covered GOLD, the bonus, or the failure path.

### In the assistant that helped me during the lab

Each of these was caught by checking the output against the code or the standard.

| Claim | What was wrong | How I confirmed |
|---|---|---|
| Surviving mutants on lines 17 and 18 are both "likely equivalent" | Only line 18 is. Negating line 17 sends a SILVER member through the x1 branch, so m3 at 200 gives 20 instead of 30. The generated test could not tell, because `assertNotNull` cannot distinguish 20 from 30. | Traced m3 at 200 by hand |
| P16 labelled "Dead Code" | The branch is reachable. It multiplies by 1. Renamed "redundant no-op branch". | Traced m1 at 100 |
| P11: 28-line long method | The count is right, but 8 lines are blank or comments, leaving 20. The real problem is mixed jobs (P5). | Counted the lines |
| "Large-ish class" | Not a catalog smell. Divergent Change and SRP are the accurate labels. | Checked the catalog |
| Delete `Scratch.java` and rerun PIT | The stale `Scratch.class` stayed in `target/classes` and inflated the run to 52 mutants. A clean build gives 19. | Compared the two runs |
| JaCoCo branch estimate of 5 of 8 | By reasoning it is 4 of 8, because m1 matches STANDARD first and never reaches the SILVER check. The figure was an estimate and was never measured, because the baseline JaCoCo run was not completed. | Reasoned through the conditions |
| Helper named `pointsFor` | Methods start with a verb (§1.2). | Checked naming rules |
| Deleting one assertion in `PlatinumTierTest` would trip the mutation gate | `PlatinumMemberPointsTest` also kills those mutants, so both files must go. | Reasoned through which tests cover the class |
| D8: a typed tier identifier plus a rule interface | Two parallel structures, and the enum edit breaks "add a tier by adding files". Replaced by one `Tier` interface. | Checked against the lab step description |

I want to be clear about one pattern: several of these errors came from presenting a first reading as a finished one. The checks that caught them were tracing a value by hand, rerunning the tool from a clean build, and comparing against the standard.

## Process notes

- Commits are small and logical, with test commits alongside the refactors they cover.
- `ANALYSIS.md` was committed before any production change and was corrected after review in a later commit (figures only, findings unchanged). Its D1 (preserve `> 500`) matches the final code.
- Mockito prints a JDK self-attach warning on every run. It is harmless and out of scope.