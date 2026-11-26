package com.wagba.restaurants.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class Changes {

    private Changes() {
    }

    /** Records the field only when the value really changed. */
    public static void track(Map<String, Object> changes, String field, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            //Object: لأن القيمة ممكن تكون String أو Number أو Boolean أو حتى Map تانية.
            Map<String, Object> pair = new LinkedHashMap<>();
            pair.put("before", before == null ? null : before.toString());
            pair.put("after", after == null ? null : after.toString());
            changes.put(field, pair);
        }
    }
}