# Contract Worksheet

One section per milestone. Fill each one in as you go, in order. Write each
prediction before you run anything. That is the part a TA asks about.

Keep it short and specific. Point at methods, call sites, and error text.

---

## Milestone 1: The notes overload

### Prediction (write this before you run the build, and you can deliberate with your agent)

**Will the consumer, untouched, still compile and pass?** Yes or no.

Yes.

**Why.** What does the compiler do with the consumer's existing call sites once
the new overload exists?

The build succeeds since the old methods remain.



### What happened

**The result.** What the build printed for each module.

From `mvn -B clean test` (a plain `mvn -B test` reused the consumer's
already-compiled classes and printed "Nothing to compile", so `clean` was
needed to make the consumer recompile against the new API):

```
[INFO] Building lab06-api 1.0.0                                           [2/3]
[INFO] Compiling 4 source files with javac [debug deprecation release 21] to target/classes
[INFO] Compiling 1 source file with javac [debug deprecation release 21] to target/test-classes
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Building lab06-consumer 1.0.0                                      [3/3]
[INFO] Compiling 1 source file with javac [debug deprecation release 21] to target/classes
[INFO] Compiling 1 source file with javac [debug deprecation release 21] to target/test-classes
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] lab06-booking-parent ............................... SUCCESS
[INFO] lab06-api .......................................... SUCCESS
[INFO] lab06-consumer ..................................... SUCCESS
[INFO] BUILD SUCCESS
```

**If your prediction was wrong,** say what you missed.

It matched.

**Is an additive change always safe in Java?** One case where adding something
to an API still breaks a caller, if you can name one.

Not always. A new overload can make an existing call ambiguous. If we have
`api.createBooking(roomId, startMinute, endMinute, null)`, null fits both
String and Notes, and neither is a better match, so the compiler refuses:
"reference to createBooking is ambiguous." The consumer would stop compiling
even though nothing was removed.

---

## Milestone 2: The request object

### Prediction (write this before you run the build)

**Will the untouched consumer still compile and pass?** Yes or no, and if no,
which module goes red and whether at compile time or test time.

No. The consumer will not compile.

**Where.** Name the call sites you expect to be affected, if any.

The parameters given in the consumer should be changed to `BookingRequest request`.

**What about the tests in `api/`, after you update them?** And whether their
result is evidence about the consumer.

After rewriting the 5 api tests, they should pass.

### Step 1: after the fold

**What the build printed.** Paste it for each module, including file and
line for anything that failed.

**Which module's tests ran, and which did not.** And what that tells you about
who can detect a contract break.

### Step 2: the deprecation path

**What you added.** The signatures that came back, and what they delegate to.

**The warnings.** Paste one deprecation warning line from the build log (from
a `mvn -B clean test` run, since a rerun with nothing to compile prints none).

**What the deprecation path resolves.** Who can now build that could not build
during step 1, and who is on which schedule.

**What the warnings accomplish that a README note would not.** Be concrete
about where the warning shows up and who sees it without looking for it.

---

## Milestone 3: The misuse critique

Not coded. One misuse, one redesign, one cost. Discuss it with your TA.

### The misuse

**What is easy to get wrong.** One specific thing about the API surface.

**The call site.** File and line in `consumer/`, with the call. Show the
code that a reader cannot understand without opening the javadoc, or that a
caller could get wrong with the compiler still happy.

**What goes wrong when it happens.** Silent bad behavior, wrong data, a crash
somewhere far away?

### The redesign

**The proposal.** Types, enums, factories, or whatever you are proposing. Show
the new signature and the new call site.

**Why the mistake is now hard or impossible to make.** Point at the mechanism,
such as the compiler, a validating constructor, or an exhaustive switch.

### One tradeoff

**What it costs.** Something real, such as caller ceremony, migration burden
against the deprecation path you just built, or more types for a newcomer to
learn. "No real downside" does not count.

**When the price is worth paying.** A condition under which it is.
