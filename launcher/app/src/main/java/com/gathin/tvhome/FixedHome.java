package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/** Administrator-configured slots. Installed apps and saved Home choices never supply extra slots. */
final class FixedHome {
    private static final int MAX_SLOTS = 4;
    static final String INPUT = "input:";

    static final class Slot {
        final String label;
        final String target;

        Slot(String label, String target) {
            this.label = label;
            this.target = target;
        }

        boolean isInput() { return target.startsWith(INPUT); }

        /** Resolve only explicitly listed packages; variants of one app can share one slot. */
        <T> T resolve(Function<String, T> resolver) {
            if (isInput()) return null;
            for (String pkg : target.split(",")) {
                pkg = pkg.trim();
                if (!pkg.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")) continue;
                T result = resolver.apply(pkg);
                if (result != null) return result;
            }
            return null;
        }
    }

    /** Only build-time entries define the row; keep one safe placeholder for an empty configuration. */
    static int slotCount(String[] configured) {
        return Math.max(1, Math.min(MAX_SLOTS, configured.length));
    }

    static List<Slot> slots(String[] configured) {
        List<Slot> result = new ArrayList<>();
        for (int i = 0; i < slotCount(configured); i++) {
            String value = i < configured.length && configured[i] != null ? configured[i] : "";
            String[] parts = value.split("\\|", 2);
            String label = parts[0].trim();
            result.add(new Slot(label.isEmpty() ? "—" : label, parts.length == 2 ? parts[1].trim() : ""));
        }
        return Collections.unmodifiableList(result);
    }

    /** Match a port's original label, never its editable nickname, list position or hardware ID suffix. */
    static boolean matchesPort(String expected, String label) {
        return expected != null && !expected.trim().isEmpty() && label != null
                && expected.replaceAll("\\s+", "").equalsIgnoreCase(label.replaceAll("\\s+", ""));
    }

    /** A stale explicit ID or duplicate labels must require configuration, not select another port. */
    static <T> T input(List<T> ports, String configuredId, String label,
                       Function<T, String> idOf, Function<T, String> labelOf) {
        T selected = null;
        for (T port : ports) {
            boolean match = configuredId.isEmpty() ? matchesPort(label, labelOf.apply(port))
                    : configuredId.equals(idOf.apply(port));
            if (!match) continue;
            if (selected != null) return null;
            selected = port;
        }
        return selected;
    }

    private FixedHome() {}
}
