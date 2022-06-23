package nostr.postr.examples

import org.junit.jupiter.api.Test

/**
 * This "Unit Test" only serves to quickly try all the examples if they still work.
 *
 * TODO: Make it actually work. Apparently Client being a singleton doesn't play well with starting
 *       and stopping.
 */
internal class ExamplesTest {
    @Test
    fun testLoadFollowsOfFollowsFromSingleRelay() {
        LoadFollowsOfFollowsFromSingleRelay.main()
    }

    @Test
    fun testLoadJesterEvents() {
        LoadJesterEvents.main()
    }

    @Test
    fun testLoadOneContactListTrackingRelays() {
        LoadOneContactListTrackingRelays.main()
    }

    @Test
    fun testLoadOneUserProfile() {
        LoadOneUserProfile.main()
    }

    @Test
    fun testLoadShortSimpleTextNotes() {
        LoadShortSimpleTextNotes.main()
    }
}