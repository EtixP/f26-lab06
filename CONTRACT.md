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

*Refined after the build:* The old four-argument `createBooking` is still in
`BookingApi`, with the same signature and the same contract. The compiler only
considers overloads whose parameter count matches the call. Both consumer calls
(`FrontDesk.java:27` and `:33`) pass four arguments, so the new five-parameter
overload is never a candidate, and they bind to the same method as before. That
method now delegates with `notes = null`, which keeps every promise in its
javadoc, so the consumer's tests see no difference either.

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

It matched. I predicted yes, and the consumer recompiled against the new API
and passed all 7 tests.

**Is an additive change always safe in Java?** One case where adding something
to an API still breaks a caller, if you can name one.

Not always. A new overload can make an existing call ambiguous. Suppose the
notes overload had been `createBooking(String roomId, long startMinute,
long endMinute, Notes notes)`: four parameters like the old method, but with a
`Notes` type in the last position. The consumer's call at `FrontDesk.java:27`,
`api.createBooking(roomId, startMinute, endMinute, null)`, would then match both
methods. `null` converts to both `String` and `Notes`, and neither type is a
subtype of the other, so neither method is more specific. The compiler would
reject the call with "reference to createBooking is ambiguous", and the consumer
would stop compiling even though nothing was removed. Our real overload was
safe because its five parameters never compete with a four-argument call.

---

## Milestone 2: The request object

### Prediction (write this before you run the build)

**Will the untouched consumer still compile and pass?** Yes or no, and if no,
which module goes red and whether at compile time or test time.

No. The consumer will not compile.

*Refined after the build:* `api` stays green, and `consumer` goes red at
compile time, so its tests never run.

**Where.** Name the call sites you expect to be affected, if any.

The parameters given in the consumer should be changed to `BookingRequest request`.

*Refined after the build:* The two `createBooking` calls in `FrontDesk`, at line
27 in `bookWalkIn` and line 33 in `joinWaitlist`. Both pass four positional
arguments, and no four-argument `createBooking` exists anymore. The
`listBookings` and `cancelBooking` calls (lines 39, 48, and 53) are unaffected
because their signatures didn't change.

**What about the tests in `api/`, after you update them?** And whether their
result is evidence about the consumer.

After rewriting the 5 api tests, they should pass.

*Refined after the build:* They pass, but that isn't evidence about the
consumer. We rewrote them to the new call along with the change, and they never
compile or run consumer code.

### Step 1: after the fold

**What the build printed.** Paste it for each module, including file and
line for anything that failed.

From `mvn -B clean test` (absolute paths shortened to repo-relative):

```
[INFO] Building lab06-api 1.0.0                                           [2/3]
[INFO] Compiling 5 source files with javac [debug deprecation release 21] to target/classes
[INFO] Compiling 1 source file with javac [debug deprecation release 21] to target/test-classes
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Building lab06-consumer 1.0.0                                      [3/3]
[INFO] Compiling 1 source file with javac [debug deprecation release 21] to target/classes
[ERROR] COMPILATION ERROR :
[ERROR] consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[27,19] method createBooking in interface edu.cmu.cs214.booking.BookingApi cannot be applied to given types;
[ERROR]   required: edu.cmu.cs214.booking.BookingRequest
[ERROR]   found:    java.lang.String,long,long,<nulltype>
[ERROR]   reason: actual and formal argument lists differ in length
[ERROR] consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[33,19] method createBooking in interface edu.cmu.cs214.booking.BookingApi cannot be applied to given types;
[ERROR]   required: edu.cmu.cs214.booking.BookingRequest
[ERROR]   found:    java.lang.String,long,long,java.lang.String
[ERROR]   reason: actual and formal argument lists differ in length
[INFO] lab06-booking-parent ............................... SUCCESS
[INFO] lab06-api .......................................... SUCCESS
[INFO] lab06-consumer ..................................... FAILURE
[INFO] BUILD FAILURE
```

**Which module's tests ran, and which did not.** And what that tells you about
who can detect a contract break.

Only the api module's tests ran, and all 5 passed. The consumer failed in its
compile step at `FrontDesk.java:27` and `:33`, so Maven never reached its test
phase and none of its 7 tests ran.

So only the consumer's side can detect this break. Our own tests can't: we
rewrote them to match the change, so they check the new contract, and they never
touch consumer code. The break showed up only when the consumer's code was
compiled against the new API. That happened here only because the consumer's
build runs inside ours. Normally the front desk team builds on their own
schedule, so they would discover the break after we shipped, and we would hear
about it from them, not from our build.

### Step 2: the deprecation path

**What you added.** The signatures that came back, and what they delegate to.

Both positional overloads came back on `BookingApi` as `@Deprecated` default
methods, each with a `@deprecated` javadoc tag pointing to
`createBooking(BookingRequest)`:

```java
@Deprecated
default Booking createBooking(String roomId, long startMinute, long endMinute,
                              String waitlistKey)
// delegates to createBooking(new BookingRequest(roomId, startMinute, endMinute)
//         .withWaitlistKey(waitlistKey))

@Deprecated
default Booking createBooking(String roomId, long startMinute, long endMinute,
                              String waitlistKey, String notes)
// delegates to createBooking(new BookingRequest(roomId, startMinute, endMinute)
//         .withWaitlistKey(waitlistKey).withNotes(notes))
```

**The warnings.** Paste one deprecation warning line from the build log (from
a `mvn -B clean test` run, since a rerun with nothing to compile prints none).

```
[WARNING] consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[27,19] createBooking(java.lang.String,long,long,java.lang.String) in edu.cmu.cs214.booking.BookingApi has been deprecated
```

What changed in the build output compared with step 1: the consumer now
compiles, with the warning above plus the same one at `FrontDesk.java:[33,19]`,
its tests run (`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`), and the
build ends with three SUCCESS rows and `BUILD SUCCESS`.

**What the deprecation path resolves.** Who can now build that could not build
during step 1, and who is on which schedule.

The front desk team can build again with no changes. `FrontDesk.java` compiles
against the deprecated overloads, and all 7 consumer tests pass. The two
schedules are now separate:

- **Us (producer):** we ship `createBooking(BookingRequest)` now, and our tests
  and any new callers already use it. We have to keep the deprecated overloads
  working until we announce a version that removes them.
- **Front desk (consumer):** they migrate their two calls to `BookingRequest`
  whenever they choose, as long as it's before that removal. The old calls
  behave identically in the meantime, because they delegate to the new method.

One thing it doesn't fix: a class outside `api/` that implements `BookingApi`
still breaks, because `createBooking(BookingRequest)` is a new abstract method it
doesn't implement. The deprecation path protects callers, not implementers.

**What the warnings accomplish that a README note would not.** Be concrete
about where the warning shows up and who sees it without looking for it.

A README note reaches only someone who goes looking in our repo, and this team
doesn't even answer our messages. A warning reaches them in their own tools,
with no effort on their part:

- **Where it shows up:** in the consumer's build output, including CI, every
  time `FrontDesk.java` is compiled, and as a strikethrough on the call in their
  IDE.
- **What it says:** the exact file, line, and column of each old call
  (`FrontDesk.java:[27,19]` and `[33,19]`) and the deprecated signature. With
  `showDeprecation` on, as in the parent `pom.xml`, it's line-level. Without it,
  javac still prints a note naming the file.
- **Where it leads:** the `@deprecated` javadoc on the old method names
  `createBooking(BookingRequest)`, so the warning points straight to the fix.

It also doesn't block them, since the build still succeeds. A README note can be
missed and goes stale. The warning stays attached to every old call until that
call is changed.

---

## Milestone 3: The misuse critique

Not coded. One misuse, one redesign, one cost. Discuss it with your TA.

### The misuse

**What is easy to get wrong.** One specific thing about the API surface.

The boolean `notifyWaitlist` flag on
`cancelBooking(long bookingId, boolean notifyWaitlist)`. At the call site it's a
bare `true` or `false`, and both values are legal, so swapping them compiles
fine.

**The call site.** File and line in `consumer/`, with the call. Show the
code that a reader cannot understand without opening the javadoc, or that a
caller could get wrong with the compiler still happy.

```java
// consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:48 (cancelAndOfferToWaitlist)
return api.cancelBooking(bookingId, true);

// consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:53 (cancelQuietly)
return api.cancelBooking(bookingId, false);
```

Without the javadoc, a reader can't tell whether `true` means "promote someone
off the waitlist", "send a notification", or "force the cancel".

**What goes wrong when it happens.** Silent bad behavior, wrong data, a crash
somewhere far away?

Silent bad behavior, with no exception:

- **`cancelQuietly` passing `true`:** the desk cancels a mistyped booking
  intending to re-enter it correctly. In between, the first eligible
  waitlisted guest is promoted to CONFIRMED and takes the room, so the
  corrected booking is turned away. The API has no "demote", so the only way
  back is to cancel that guest's booking outright.
- **`cancelAndOfferToWaitlist` passing `false`:** the room would be freed, but
  the waitlisted guest would stay WAITLISTED while the room sat empty.

The compiler accepts both mistakes, and nothing fails at the call. The only
symptom is a wrong status on the schedule later. A test that checks the status
after a cancel catches it, like `FrontDeskTest.quietCancelLeavesTheWaitlistWhereItWas`,
but a new call site without such a test would ship the bug.

### The redesign

**The proposal.** Types, enums, factories, or whatever you are proposing. Show
the new signature and the new call site.

Replace the boolean with an enum that names both behaviors:

```java
public enum CancelMode {
    /** Promote the first eligible overlapping waitlisted booking. */
    PROMOTE_FROM_WAITLIST,
    /** Cancel without promoting anyone. */
    QUIET
}

boolean cancelBooking(long bookingId, CancelMode mode);
```

New call sites in `FrontDesk`:

```java
return api.cancelBooking(bookingId, CancelMode.PROMOTE_FROM_WAITLIST); // cancelAndOfferToWaitlist
return api.cancelBooking(bookingId, CancelMode.QUIET);                 // cancelQuietly
```

**Why the mistake is now hard or impossible to make.** Point at the mechanism,
such as the compiler, a validating constructor, or an exhaustive switch.

The compiler does the enforcing. No design can stop a caller from deliberately
choosing the wrong mode. What the enum removes is the accidental, unreadable
version of the mistake:

- **No bare flag:** `cancelBooking` no longer takes a boolean, so the caller must
  write a named constant, and the call site says what will happen. `QUIET`
  inside `cancelAndOfferToWaitlist` is obviously wrong in review, where
  `false` was not.
- **Can't misread it:** a reader no longer needs the javadoc to know what the
  argument means.
- **New modes get handled:** the implementation switches over the modes with a
  `switch` expression with no `default`. A mode added later (say, notify
  without promoting) won't compile until every such switch handles it.

Two limits. During migration, the deprecated `cancelBooking(long, boolean)`
(see the tradeoff below) still accepts `true` and `false`, so enforcement is
complete only once it's removed, and until then each old call gets a
deprecation warning. Also, `null` still compiles; the implementation rejects it
at runtime with `IllegalArgumentException`.

### One tradeoff

**What it costs.** Something real, such as caller ceremony, migration burden
against the deprecation path you just built, or more types for a newcomer to
learn. "No real downside" does not count.

The main cost is migration. Removing `cancelBooking(long, boolean)` is a
breaking change to a method the front desk calls twice (`FrontDesk.java:48` and
`:53`). To avoid breaking them, we would repeat Milestone 2's deprecation path:
keep the boolean version as a `@Deprecated` method that maps `true` and `false`
to the enum, and maintain both forms in the contract until the front desk
migrates. While that lasts, the boolean mistake is still possible, as noted
above.

There's also smaller, permanent ceremony. Callers have one more type to import
and learn, and a caller that computes the choice, like
`cancelBooking(id, shouldPromote)`, now has to write
`shouldPromote ? CancelMode.PROMOTE_FROM_WAITLIST : CancelMode.QUIET`.

**When the price is worth paying.** A condition under which it is.

It's worth paying when both of these hold:

1. The method has callers we don't control and can't review, like the front
   desk team.
2. A wrong choice is costly and hard to reverse, like a wrong `true` that gives
   a room away for good.

It's even more worth it if a third mode is likely, since an enum can grow and a
boolean can't. It isn't worth it for a private helper with one caller in our
own code, where we can read every call site and the migration cost buys nothing.
