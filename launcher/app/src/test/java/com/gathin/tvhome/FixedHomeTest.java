package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class FixedHomeTest {
    private static final String[] HOME = {
            "看电影|org.jellyfin.androidtv",
            "看照片|com.synology.projectkailash,com.synology.projectkailash.cn",
            "看电视|input:HDMI 1",
            "看央视|com.newtv.cboxtv"
    };

    @Test public void installRemoveAndUpdateKeepTheFourSlotsAndNeverOfferOtherApps() {
        Map<String, String> installed = new HashMap<>();
        installed.put("com.example.store", "unrelated store");
        List<FixedHome.Slot> slots = FixedHome.slots(HOME);
        List<String> queries = new ArrayList<>();
        for (FixedHome.Slot slot : slots) {
            assertNull(slot.resolve(pkg -> { queries.add(pkg); return installed.get(pkg); }));
        }
        assertFalse(queries.contains("com.example.store"));
        assertEquals(4, slots.size());
        installed.put("org.jellyfin.androidtv", "version 1");
        assertEquals("version 1", slots.get(0).resolve(installed::get));
        installed.put("org.jellyfin.androidtv", "version 2");
        assertEquals("version 2", slots.get(0).resolve(installed::get));
        installed.remove("org.jellyfin.androidtv");
        assertNull(slots.get(0).resolve(installed::get));
        assertEquals("看电影", slots.get(0).label);
        assertEquals("看照片", slots.get(1).label);
        assertEquals("看电视", slots.get(2).label);
        assertEquals("看央视", slots.get(3).label);
    }

    @Test public void synologyRegionalPackagesShareOneSlotWithDeterministicPriority() {
        FixedHome.Slot photos = FixedHome.slots(HOME).get(1);
        Map<String, String> apps = new HashMap<>();
        apps.put("com.synology.projectkailash.cn", "China TV");
        assertEquals("China TV", photos.resolve(apps::get));
        apps.put("com.synology.projectkailash", "Worldwide TV");
        assertEquals("Worldwide TV", photos.resolve(apps::get));
        apps.clear();
        apps.put("com.synology.photos", "unlisted phone app");
        assertNull(photos.resolve(apps::get));
    }

    @Test public void inputNeverResolvesAsAnAppOrOpensAnInstaller() {
        FixedHome.Slot input = FixedHome.slots(HOME).get(2);
        assertTrue(input.isInput());
        assertNull(input.resolve(pkg -> { throw new AssertionError("Input isn't an app"); }));
        for (String target : new String[]{"", "market://details?id=app", "intent:install", "pkg/.Activity"}) {
            assertNull(FixedHome.slots(new String[]{"Missing|" + target}).get(0)
                    .resolve(pkg -> { throw new AssertionError("Invalid target: " + pkg); }));
        }
    }

    @Test public void configuredEntriesDefineSlotCountWithinBounds() {
        List<FixedHome.Slot> missing = FixedHome.slots(new String[]{null, "Photos"});
        assertEquals(2, missing.size());
        assertEquals("Photos", missing.get(1).label);
        assertNull(missing.get(1).resolve(pkg -> "unexpected"));
        assertEquals(4, FixedHome.slots(new String[]{"A", "B", "C", "D", "E"}).size());
        assertEquals(1, FixedHome.slots(new String[]{}).size());
        try {
            missing.clear();
            fail("Slots must be immutable");
        } catch (UnsupportedOperationException expected) { }
    }

    @Test public void removingCctvLeavesExactlyThreeEntriesWithoutAnEmptyFourthCard() {
        String[] configured = java.util.Arrays.copyOf(HOME, 3);
        List<FixedHome.Slot> slots = FixedHome.slots(configured);
        assertEquals(3, slots.size());
        assertEquals(slots.size(), FixedHome.slotCount(configured));
        assertEquals("看电视", slots.get(2).label);
        for (FixedHome.Slot slot : slots) assertFalse(slot.target.contains("com.newtv.cboxtv"));
    }

    private static final class Port {
        final String id, label;
        Port(String id, String label) { this.id = id; this.label = label; }
    }

    private Port select(List<Port> ports, String explicitId) {
        return FixedHome.input(ports, explicitId, "HDMI 1", p -> p.id, p -> p.label);
    }

    @Test public void hdmiOneDoesNotMeanFirstEnumeratedInputOrHardwareOne() {
        Port two = new Port("vendor/HW1", "HDMI 2");
        Port one = new Port("vendor/HW5", "HDMI1");
        Port ten = new Port("vendor/HW10", "HDMI 10");
        assertSame(one, select(List.of(two, ten, one), ""));
        assertNull(select(List.of(two, ten), ""));
    }

    @Test public void ambiguousOrStaleHdmiConfigurationFailsClosed() {
        Port first = new Port("vendor/HW5", "HDMI 1");
        Port duplicate = new Port("other/HW8", "hdmi 1");
        assertNull(select(List.of(first, duplicate), ""));
        assertSame(first, select(List.of(first, duplicate), first.id));
        assertNull(select(List.of(first), "stale/HW5"));
        assertNull(select(List.of(), ""));
        assertFalse(FixedHome.matchesPort("HDMI 1", null));
        assertFalse(FixedHome.matchesPort(" ", ""));
    }
}
