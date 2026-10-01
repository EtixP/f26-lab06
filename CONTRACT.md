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

Not always. A new overload can make an existing call ambiguous. Suppose the
new overload had been `createBooking(String roomId, long startMinute,
long endMinute, Notes notes)`, with a `Notes` type in the last slot instead of
a fifth parameter. If we have
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

So the producer can't detect this break from its own side. The api tests were
rewritten along with the change, so they check the new contract, and they never
touch consumer code. Their passing says nothing about the consumer. The break
showed up only in the consumer's build, and the compiler caught it before the
consumer's tests could even run. Normally the consumer builds on their own
schedule, so they would find the break after we shipped, unless their build runs
inside ours, as it does in this repo.

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
against the deprecated overloads, and all 7 consumer tests pass. Nobody has to
move at the same moment. We, the producer, switched to
`createBooking(BookingRequest)` now, and new callers use it. The front desk team
can switch to `BookingRequest` on their own schedule, any time before a later
version removes the deprecated overloads. The warnings remind them until they
do.

One thing it doesn't fix: a class outside `api/` that implements `BookingApi`
still breaks, because `createBooking(BookingRequest)` is a new abstract method it
doesn't implement. The deprecation path protects callers, not implementers.

**What the warnings accomplish that a README note would not.** Be concrete
about where the warning shows up and who sees it without looking for it.

The warning appears in the consumer's own build output, including CI, every
time `FrontDesk.java` compiles. It gives the exact file, line, and column of
each old call (`FrontDesk.java:[27,19]` and `[33,19]`) and the deprecated
signature, and IDEs strike through the call. The front desk developers see it
during their normal build without reading anything we wrote. A README note
reaches only someone who goes looking in our repo, and this team doesn't even
answer our messages. The `@deprecated` javadoc on the old method names the
replacement, so the warning leads straight to the fix. And it doesn't block
them, since the build still succeeds.

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

Silent bad behavior, with no exception. If `cancelQuietly` passed `true` by
mistake, fixing a typo at the desk would give the room to the first eligible
waitlisted guest, who becomes CONFIRMED, and that's hard to undo. If
`cancelAndOfferToWaitlist` passed `false`, the room would be freed but the
waitlisted guest would stay WAITLISTED while the room sat empty. Either way, the
only symptom is a wrong schedule later on.

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

The compiler does the enforcing. `cancelBooking` no longer accepts a boolean, so
the caller has to write a named constant, and that name says at the call site
what will happen. A reviewer can spot `QUIET` in `cancelAndOfferToWaitlist` at a
glance. The implementation handles the modes with a `switch` expression with no
`default`, so a mode added later (say, notify without promoting) won't compile
until it's handled. The one remaining hole is `null`, which the implementation
rejects with `IllegalArgumentException`.

### One tradeoff

**What it costs.** Something real, such as caller ceremony, migration burden
against the deprecation path you just built, or more types for a newcomer to
learn. "No real downside" does not count.

It's another breaking change, to a method the front desk already calls in two
places. Shipping it without breaking them means repeating Milestone 2's
deprecation path: keep `cancelBooking(long, boolean)` as a `@Deprecated` method
that maps `true` and `false` to the enum, live with the warnings, and keep both
forms in the contract until the front desk migrates. It also adds one more type
for every caller to import and learn. A caller that computes the choice, like
`cancelBooking(id, shouldPromote)`, now has to write
`shouldPromote ? CancelMode.PROMOTE_FROM_WAITLIST : CancelMode.QUIET`.

**When the price is worth paying.** A condition under which it is.

It's worth it when callers we don't control use the method and a wrong choice
has costly, hard-to-reverse effects, as here, where a wrong `true` gives a room
away. It's also worth it when a third mode is likely, since an enum can grow and
a boolean can't. It's not worth it for a private helper with one caller in our
own code, where we can read every call site.
