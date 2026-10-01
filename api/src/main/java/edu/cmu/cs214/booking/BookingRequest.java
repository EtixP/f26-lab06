package edu.cmu.cs214.booking;

/**
 * Everything needed to ask for one booking, passed to
 * {@link BookingApi#createBooking(BookingRequest)}.
 *
 * <p>The room and the half-open range {@code [startMinute, endMinute)} are
 * required and given to the constructor. The waitlist key and the notes are
 * optional, default to null, and are set with {@link #withWaitlistKey(String)}
 * and {@link #withNotes(String)}:
 *
 * <pre>{@code
 * api.createBooking(new BookingRequest("R1", 540, 600)
 *         .withWaitlistKey("party-of-four")
 *         .withNotes("needs a projector"));
 * }</pre>
 *
 * <p>Requests are immutable. Each {@code with} method returns a new request
 * and leaves the original unchanged. A request is not checked until it is
 * passed to {@link BookingApi#createBooking(BookingRequest)}.
 */
public final class BookingRequest {

    private final String roomId;
    private final long startMinute;
    private final long endMinute;
    private final String waitlistKey;
    private final String notes;

    /** A request for {@code roomId} over {@code [startMinute, endMinute)}, with no waitlist key and no notes. */
    public BookingRequest(String roomId, long startMinute, long endMinute) {
        this(roomId, startMinute, endMinute, null, null);
    }

    private BookingRequest(String roomId, long startMinute, long endMinute,
                           String waitlistKey, String notes) {
        this.roomId = roomId;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
        this.waitlistKey = waitlistKey;
        this.notes = notes;
    }

    /** A copy of this request with the given waitlist key, or null to decline waitlisting. */
    public BookingRequest withWaitlistKey(String waitlistKey) {
        return new BookingRequest(roomId, startMinute, endMinute, waitlistKey, notes);
    }

    /** A copy of this request with the given notes, or null for none. */
    public BookingRequest withNotes(String notes) {
        return new BookingRequest(roomId, startMinute, endMinute, waitlistKey, notes);
    }

    public String getRoomId() {
        return roomId;
    }

    public long getStartMinute() {
        return startMinute;
    }

    public long getEndMinute() {
        return endMinute;
    }

    /** The waitlist key, or null if waitlisting is declined. */
    public String getWaitlistKey() {
        return waitlistKey;
    }

    /** The notes, or null if none were given. */
    public String getNotes() {
        return notes;
    }

    @Override
    public String toString() {
        return "BookingRequest[" + roomId + " " + startMinute + "-" + endMinute
                + " waitlistKey=" + waitlistKey + " notes=" + notes + "]";
    }
}
