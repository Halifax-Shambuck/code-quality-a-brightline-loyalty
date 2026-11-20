# ANALYSIS.md: Brightline Loyalty (pre-refactor)

Scope: the starter code exactly as delivered (commit `chore: import starter code`). No production code was changed before this document was first committed. Standard used: AmaliTech Engineering Constitution (section references point to it).

Correction note: corrected after review in a later commit. Changed: the JaCoCo baseline row (an estimate that was never measured), the hand-mutation result, and the amount-type wording in D5 and section 10. Findings and rankings are unchanged.

---

## 0. Summary

The service compiles and its three tests are green, but neither fact means much.

- **Code:** one class does lookup, base points, tier multiplier, bonus, console output and failure policy. It builds its own repository, so tests cannot control it. It wraps everything in `catch (Exception)` and returns 0, so unknown members, bad amounts and genuine bugs all look like "0 points". Tiers are strings in an if/else chain with no final `else`, so an unrecognised tier silently earns x1. Amounts are unvalidated `double`s: negative amounts earn negative points and large ones overflow.
- **Tests:** one asserts only "result > 0", one mocks the class under test and asserts the mock, one calls `assertNotNull` on a primitive `int`. PIT: 1 of 12 mutants killed in `PointsCalculator` (11% project-wide) while 68% of lines run.
- **Most serious problem (section 6):** the swallowed catch-all that turns every failure into a plausible-looking 0.
- **The 8 surviving mutants in `PointsCalculator`:** 7 are due to missing assertions or to output that cannot be tested while it sits inside the calculation (the `println`), and 1 is an equivalent mutant caused by a redundant no-op branch (P16). A further 3 mutants have no coverage at all.

---

## 1. Baseline results (section 0.1 of the standard)

| Item | Result |
|---|---|
| Environment | JDK 21.0.13 (Temurin), Maven 3.10.0, Git 2.56.0, Windows 11 |
| Build | Compiles (5 source files, 1 test file) |
| Existing tests | 3 tests, all pass (`mvn verify`: Tests run: 3, Failures: 0, Errors: 0) |
| Demo output | m1 / GHS 100 gives 10; m2 / GHS 250 gives 50; unknown / GHS 100 gives 0 (no "Awarded" line for unknown) |
| Console during tests | Only two "Awarded" lines for three tests (Kofi Mensah 10, Yaa Boateng 30). The Gold test printed nothing because it never ran the real code (section 4, test 2) |
| Tool warning | Mockito self-attaching inline mock maker (JDK warning). Out of scope |

## 2. Automated baseline

| Tool | Figure |
|---|---|
| PIT, whole project (4 classes) | Line coverage 68% (25/37); mutation coverage 11% (2/19); test strength 17% (2/12). Of the 19 mutants, 2 were killed, 10 survived and 7 had no coverage |
| PIT, `PointsCalculator` only | 12 mutants: 1 killed, 8 survived, 3 no coverage |
| JaCoCo | Not measured at baseline. PIT line coverage is used instead |

PIT reports no coverage for the mutants on lines 21, 22 and 27 (the GOLD branch and the bonus). The catch block (lines 32-34) is never reached by the three tests, because none passes an unknown member.

Coverage measures that code ran. Mutation score measures whether anything would notice a wrong answer. About two thirds of lines execute, yet the tests would notice roughly one bug in nine (sections 6.3 and 7.2 of the standard).

---

## 3. Production code findings

Severity scale: **Critical** (silent wrong results or hides other failures), **High** (blocks testing or the stated business change, or produces wrong money), **Medium**, **Low**.

| # | Finding | Where | Catalog / SOLID / section | Consequence | Severity |
|---|---|---|---|---|---|
| P2 | `catch (Exception e)` swallows every failure and returns 0 | `PointsCalculator` lines 32-34 | Section 4 (never swallow, preserve cause) | Unknown member, null id, bad amount and real bugs are indistinguishable from a legitimate 0. A member is silently denied points with no trace | Critical |
| P3 | Unknown member "works" only by accident | lines 11 and 17 | Section 4 (expected failure must be explicit) | Repository returns `null`; `member.getTier()` throws `NullPointerException`; the catch hides it. Nobody decided "unknown = 0" | Critical (same root as P2) |
| P17 | No input validation on amount | `calculatePoints` | Section 4; "malformed purchase" in the brief | -100 gives -10 points; NaN gives 0 with "Awarded 0" printed; Infinity, MAX_VALUE and 1e12 overflow `int` to negative or garbage (probe rows 16-25) | High |
| P9 | Unknown tier gets no multiplier and no error | no final `else`, lines 17-22 | Section 4; OCP | A member with an unrecognised tier is silently treated as x1: wrong money, no signal | High |
| P1 | Calculator builds its own repository with `new` in a field initialiser | line 7 | Testability Flaw #1 (section 5); DIP | Tests cannot substitute a repository: no mock, no throwing repository, no controlled member data. Blocks the failure-path tests | High |
| P4 | If/else chain on tier string | lines 17-22 | Switch / complex if-else (Object-Oriented Abusers); OCP | A new tier (the business has said one is coming) means editing this method | High |
| P5 | Several reasons to change in one class | whole class | Divergent Change; SRP | Reasons to change: (1) member lookup, (2) base-points rule, (3) tier multiplier rules, (4) bonus rule, (5) output, (6) failure policy | High |
| P7 | Primitive Obsession | tier `String`; id `String`; amount `double` | Bloaters: Primitive Obsession | Any string is a "tier"; money is a binary float; nothing stops NaN or negatives | High |
| P6 | Printing inside a calculation | line 30 | Section 1.3 (no side effects); SRP | A "calculate" method writes to the console; PIT shows removing it survives | Medium |
| P18 | `(int)` truncation after multiplying | lines 14, 20 | Accidental behaviour | Silver 30 gives 4, 50 gives 7 (4.5 and 7.5 truncated). Never documented as a rule | Medium |
| P11 | Long method | lines 9-36: 28 lines in total, 20 of them code | Long Method; section 1.3 (rarely above 20) | At the limit by code-line count and well over by total length. More importantly it does many jobs, so the pieces cannot be tested separately | Medium |
| P8 | Tier literals scattered across classes | `Member` comment, calculator, repository | Latent Shotgun Surgery | A typo or rename in one place silently disables a multiplier; no compiler help | Medium |
| P10 | Magic numbers and strings | 10, 1.5, 2, 500, 50, "STANDARD", etc. | Section 1.2 (named constants) | Business rules are unnamed and unsearchable | Medium |
| P14 | Feature Envy | calculator interrogates `Member` to decide about its tier | Couplers: Feature Envy | Behaviour belongs with the data owner or a tier abstraction | Low |
| P13 | Repeated `member.getTier()`; message chain `member.getTier().equals(...)` | lines 17, 19, 21 | Duplicate Code; Message Chain | Repetition plus a null hazard in the chain | Low |
| P12 | Comments that say *what* | lines 5-6, 13, 16, 25, 33; tier comment in `Member` | Dispensables: Comments (section 1.5) | Compensate for unnamed concepts. Only the repository comment explains a *why* | Low |
| P15 | `Member` is a Data Class | `Member` | Dispensables: Data Class | Fields and getters only; an immutable `record` fits (section 1.1) | Low |
| P16 | Redundant no-op branch | line 18 (`points * 1`) | Dispensables (redundant no-op branch) | The branch is reachable but changes nothing. See the note below | Low |

**Note on P16 and the surviving mutants on lines 17 and 18.** They are different cases.

- **Line 18** (`points * 1` to `points / 1`): both give the same result for every input. This is an equivalent mutant: no test can kill it. It is a symptom of the redundant branch, not of a weak test.
- **Line 17** (negated condition): not equivalent. Negating it sends SILVER and GOLD members into the STANDARD branch (x1) and skips their real multiplier. An exact assertion on a SILVER or GOLD result would kill it. It survives only because no test checks an exact tier result.

**Ranked priority (most to least important):** P2 and P3, P17, P9, P1, P4, P5, P7, then the Medium and Low items. The order follows the four questions in section 6: silent versus loud, blocks other fixes, hides other bugs, false confidence.

**Checked, not present:** Long Parameter List, Data Clumps, Temporary Field, Refused Bequest, Alternative Classes with Different Interfaces, Parallel Inheritance Hierarchies (in the original code), Lazy Class, Inappropriate Intimacy, Middle Man, Speculative Generality. `MemberRepository` is not speculative: it is the seam the refactor uses.

---

## 4. Hollow tests (reported separately from code problems)

Each entry gives what it exercises, what the assertion proves, a change it would not catch, and the PIT evidence.

### Test 1: `awardsPointsToAMember`

- **Exercises:** the real calculator, one STANDARD member (m1) at GHS 100.
- **Assertion proves:** only that the result is greater than zero.
- **Would not catch:** any wrong divisor, multiplier or base calculation that still gives a positive number. It never reaches SILVER, GOLD, the bonus or the failure path.
- **PIT evidence:** line 14 (division replaced by multiplication, so 100 becomes 1000) survived. Line 26 negated condition (spurious +50) survived. This test killed the only mutant killed in the class: line 31, "return replaced with 0".

### Test 2: `calculatesGoldPoints`

- **Exercises:** a Mockito mock of `PointsCalculator` itself. The real method body never runs.
- **Assertion proves:** that Mockito returns the value the test told it to return (50). It tests the mocking framework, not the code.
- **Would not catch:** any change at all: deleting the GOLD multiplier, emptying the method, returning 0 for everyone.
- **PIT evidence:** PIT's "Tests examined" lists only two of the three tests, because this one never touches mutated code. Line 22 (GOLD multiplier) is NO_COVERAGE: no test anywhere runs the GOLD branch.

### Test 3: `awardsPointsForAPurchase`

- **Exercises:** the real calculator, one SILVER member (m3) at GHS 200 (real result 30).
- **Assertion proves:** nothing. `points` is a primitive `int`; `assertNotNull(points)` boxes it into an `Integer` that can never be `null`, so the assertion cannot fail.
- **Would not catch:** any change to the returned value, including a wrong SILVER multiplier or returning 0 for everyone.
- **PIT evidence:** line 20 (SILVER multiplier, `*` replaced by `/`) is covered but survived: the line ran and nobody checked the answer. The negated conditions on lines 17 and 19 also survived.

### Which survivors are which

| Line | Mutant | Status | What would kill it |
|---|---|---|---|
| 14 | `/` to `*` | Survived | exact base points (for example 100 gives 10) |
| 17 | negate STANDARD check | Survived | exact SILVER or GOLD result |
| 18 | `* 1` to `/ 1` | Survived | equivalent mutant, cannot be killed |
| 19 | negate SILVER check | Survived | exact SILVER result |
| 20 | `* 1.5` to `/ 1.5` | Survived | exact SILVER result including truncation |
| 21 | negate GOLD check | No coverage | any GOLD test |
| 22 | `* 2` to `/ 2` | No coverage | exact GOLD result |
| 26 | negate `> 500` | Survived | an amount below 500 gets no bonus |
| 26 | boundary `>` to `>=` | Survived | an amount of exactly 500 |
| 27 | `+ 50` to `- 50` | No coverage | an amount above 500 gets the bonus |
| 30 | remove `println` | Survived | untestable until output moves out of the calculation |
| 31 | return to 0 | Killed | (by test 1) |

### Hand mutation check

Mutation considered: SILVER multiplier on line 20 changed from `1.5` to `3`.

| Test | Prediction | Reason |
|---|---|---|
| `awardsPointsToAMember` | pass | m1 is STANDARD, so the SILVER line never runs |
| `calculatesGoldPoints` | pass | it only exercises the mock |
| `awardsPointsForAPurchase` | pass | m3 at 200 gives 60 instead of 30, and `assertNotNull` on an `int` cannot fail |

**Actual:** not run by hand against the old tests. PIT's line 20 mutant (`*` replaced by `/`) survived all three tests, which shows the same gap: no generated test reads a SILVER result. After the characterization tests were written, the same `1.5` to `3` change failed all five SILVER rows (expected 1, 4, 7, 30, 140; actual 3, 9, 15, 60, 230).

---

## 5. Missing tests

None of the following is covered by any existing test:

- GOLD happy path (lines 21-22 have no coverage)
- Bonus branch, and both sides of the boundary at exactly 500 (line 27 no coverage; line 26 mutants survived)
- Exact values for every tier, including truncation (SILVER 30 gives 4, 50 gives 7)
- Unknown member, null id, blank id (the catch block, lines 32-34, never runs)
- A repository that throws (impossible today because of P1)
- Zero, negative, NaN, infinite and very large amounts
- Unrecognised tier
- Any check of printed output, once output has a proper home

---

## 6. The single most serious problem

**The swallowed catch-all (P2, together with the accidental unknown-member path P3).**

Tested against the four questions:

1. **Silent or loud?** Silent. Every failure returns `0`, and the only trace is the absence of the "Awarded" line.
2. **Blocks other fixes?** Yes, for effective verification. Because the catch-all converts failures into 0, tests cannot reliably distinguish a correctly handled failure from a swallowed exception while this behaviour remains. The catch can be removed independently, but leaving it in place undermines verification of the other failure-handling fixes.
3. **Hides other bugs?** Yes, all of them. A real programming error (a null, an arithmetic problem) is caught by the same block and turned into 0.
4. **False confidence?** Yes. A member who is not found, a malformed purchase and a genuine zero all produce the same `0`, and the suite is green while this goes on. The test suite cannot even see this path (the catch block has no coverage).

Why not the hard-wired repository (P1), the next candidate? P1 is a structural blocker: it prevents testing, but it fails loudly (you notice you cannot test it) and it does not by itself deny anyone points. P2 can cost members points with no log line, no error and no test to notice. P1 is therefore the first thing to fix to enable tests, but P2 is the most serious flaw in what the code does.

---

## 7. What the AI got wrong (for the team)

The code and tests are plausible, which is what makes them risky. Patterns to watch for in future AI output (section 7 of the standard):

- **A catch-all that returns a default value.** It reads as "robust" and hides every failure.
- **Dependencies built inside the class** (`new` in a field). Convenient for the generator, unusable for testing.
- **A mock of the class under test.** The test asserts what it told the mock to say. It is a green test that tests nothing.
- **`assertNotNull` on a primitive, and `> 0` as the only check.** Assertions that are weak or always true.
- **Happy-path-only tests**, with no boundary, failure or per-branch values.
- **Money as `double`, strings for fixed sets, magic numbers.**
- **A "calculate" method that prints.**
- **Confident-looking comments** that describe what the code does, not why.

---

## 8. Decisions on business rules

These are requirements, not bugs. Behaviour-changing decisions are pinned first by characterization tests, then changed deliberately in their own commit. Items marked **(confirm)** need the lead's or product owner's agreement.

| # | Question | Decision | Reasoning | Behaviour change? |
|---|---|---|---|---|
| D1 | Does exactly GHS 500 earn the bonus? | **Preserve the current rule: the bonus applies above GHS 500 (`> 500`).** Open question for the product owner. | The brief is silent and the code says `> 500`, so changing it on the strength of "the usual reading" would silently change what members earn at exactly 500 (50 points would become 100). Pin the current behaviour with tests (500 gives 50, 500.01 gives 100). If `>= 500` is intended, it is a one-operator change in its own commit, with the product owner's sign-off recorded. **(confirm)** | No |
| D2 | Bonus before or after the tier multiplier? | **After; flat +50, not multiplied** | Matches current behaviour (probe rows 13-15), so nothing changes. Documented as a rule | No |
| D3 | Rounding | **Points are whole numbers, rounded down** after the base-points step and after the tier multiplier, as today. **(confirm)** | The "2 decimal places" convention applies to currency amounts, not points. For currency, the usual convention is half-up (0.5 and above rounds up; 0.134 rounds down to 0.13). Points have no universal convention, so the existing round-down behaviour is kept rather than inventing a change | No |
| D4 | Unknown member | **Throw a typed exception** (`MemberNotFoundException`, carrying the id). The repository returns `Optional<Member>`, and the calculator turns an absent value into the exception | Section 4: explicit failure, no swallowing. `Optional` models expected absence at the repository; the calculator decides it is an error for this operation | Yes (0 becomes an exception) |
| D5 | Invalid amount | **Throw** for negative, NaN or infinite. Zero is allowed and earns 0 points **(confirm)**. Overflow rule: use `Math.toIntExact`, `Math.multiplyExact` and `Math.addExact`; a total that does not fit in an `int` is rejected with the domain exception, and the `ArithmeticException` is kept as the cause | A malformed purchase must fail loudly (brief, section 4). The exact-arithmetic methods avoid inventing a business limit and give tests a definite boundary: a total of exactly `Integer.MAX_VALUE` is accepted, one more is rejected. The amount stays a `double`, validated at the calculator's entry. A money type would hold no rule beyond that check (YAGNI) | Yes |
| D6 | Member id matching | **Exact, case-sensitive, no trimming.** Null or blank id is rejected with a validation exception | Ids are identifiers, not free text; fuzzy lookup could return the wrong member. Normalising user input belongs at the boundary that accepts it. A dedicated `MemberId` type is not needed yet (YAGNI) | Yes (null or blank) |
| D7 | Printing | **Removed from the calculator.** Callers (`Demo`, the application layer) report results; real auditing would go through a logger, not `System.out` | Section 1.3: queries must not have side effects; SRP | Yes (output moves) |
| D8 | Tier representation | **A single `Tier` abstraction (an interface) whose implementations carry their own multiplier, held by `Member`.** The repository creates members with their `Tier` object; there is no tier string anywhere in the domain | One structure, not two. An enum alongside a rule interface would be two parallel structures (the Parallel Inheritance smell), and an enum-only design needs the enum edited for each new tier. With the interface, a new tier is a new class. An unrecognised tier is impossible by type inside the domain, so nothing in the calculator needs to throw for it. If external strings are ever parsed into tiers, that parsing boundary must fail loudly | No (structure only) |

**Pinning policy for characterization tests.** Pin what is correct and kept: probe rows 1-15, including 500 gives 50 and 500.01 gives 100 (D1), and the tier results. Do not pin as correct: -10 for a negative amount, the overflow values, NaN giving 0, or 0 for unknown members. Record those as known defects fixed later.

---

## 9. SOLID mapping

| Principle | Verdict | Justification |
|---|---|---|
| S: Single Responsibility | **Violated** | `PointsCalculator` has six reasons to change (P5): lookup, base points, tier rules, bonus, output and failure policy |
| O: Open-Closed | **Violated** | Adding a tier means editing the if/else chain; an unrecognised tier is silently accepted (P4, P9) |
| L: Liskov Substitution | **Not applicable, with one latent risk** | There is no inheritance to break. `InMemoryMemberRepository` is the only implementation, but the interface does not specify what "not found" means (it returns `null`), so a second implementation could behave differently. `Optional` fixes the contract |
| I: Interface Segregation | **Not violated** | `MemberRepository` has one method; no client depends on something it does not use. Splitting it further would be speculative |
| D: Dependency Inversion | **Violated** | The abstraction exists but is bypassed: the high-level calculator constructs the concrete `InMemoryMemberRepository` itself (P1) |

---

## 10. From finding to fix (traceability)

| Finding | Fix | Area |
|---|---|---|
| P7, P8, P9 | `Tier` abstraction held by `Member`; amount validated at the calculator's entry | Domain types |
| P2, P3, P17 | Typed exceptions, `Optional` at the repository, remove the catch-all, exact-arithmetic overflow rule | Failure model |
| P1 | Inject `MemberRepository` through the constructor | Dependency injection |
| P4, P5, P9 | Tier implementations carry their multiplier; separate base points, tier and bonus | Tier rules, responsibilities |
| P6 | Move output out of the calculation | Responsibilities |
| P10-P13, P15, P16, P18 | Named constants, remove the no-op branch, `record` for `Member`, document the rounding rule | Names and constants |
| Hollow tests | Solitary and sociable tests, parameterized boundaries, failure tests, PIT re-run | Test suite, quality gates |

P7 is closed for the tier and left open for the amount, which stays a `double` (see D5).

---

## Appendix: probe results on the untouched code

Actual outputs from the scratch run (30 inputs). They document what the code did before the refactor, not what it should do.

| # | Member | Amount | Returned | Printed "Awarded"? |
|---|---|---|---|---|
| 1 | m1 | 0 | 0 | yes |
| 2 | m1 | 9.99 | 0 | yes |
| 3 | m1 | 10 | 1 | yes |
| 4 | m1 | 19.99 | 1 | yes |
| 5 | m1 | 499.99 | 49 | yes |
| 6 | m1 | 500 | 50 | yes |
| 7 | m1 | 500.01 | 100 | yes |
| 8 | m1 | 501 | 100 | yes |
| 9 | m3 | 10 | 1 | yes |
| 10 | m3 | 30 | 4 | yes |
| 11 | m3 | 50 | 7 | yes |
| 12 | m2 | 250 | 50 | yes |
| 13 | m2 | 600 | 170 | yes |
| 14 | m3 | 600 | 140 | yes |
| 15 | m1 | 600 | 110 | yes |
| 16 | m1 | -100 | -10 | yes |
| 17 | m1 | -5 | 0 | yes |
| 18 | m1 | -0.0 | 0 | yes |
| 19 | m1 | NaN | 0 | yes |
| 20 | m1 | +Infinity | -2147483599 | yes |
| 21 | m2 | +Infinity | 48 | yes |
| 22 | m1 | -Infinity | -2147483648 | yes |
| 23 | m1 | Double.MAX_VALUE | -2147483599 | yes |
| 24 | m1 | 1e12 | -2147483599 | yes |
| 25 | m2 | 1e12 | 48 | yes |
| 26 | null | 100 | 0 | no |
| 27 | "" | 100 | 0 | no |
| 28 | "unknown" | 100 | 0 | no |
| 29 | "M1" | 100 | 0 | no |
| 30 | "m1 " | 100 | 0 | no |

Patterns: a one-pence difference at 500 doubles the reward (rows 6-7); the bonus is added after the multiplier and is never multiplied (rows 13-15); truncation after the multiplier (rows 10-11); negative amounts give negative points; unvalidated large values overflow `int`; ids are case- and whitespace-sensitive; and failure is indistinguishable from a legitimate zero (rows 1 and 26-30).
