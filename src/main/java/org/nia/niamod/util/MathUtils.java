package org.nia.niamod.util;

import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class MathUtils {
    public static int mode(List<Integer> numbers) {
        if (numbers.isEmpty()) return 0;

        Map<Integer, Integer> counts = new HashMap<>();
        for (int n : numbers) {
            counts.put(n, 1 + counts.getOrDefault(n, 0));
        }

        int mostCommon = numbers.getFirst();
        int maxCount = 0;

        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > maxCount) {
                mostCommon = entry.getKey();
                maxCount = entry.getValue();
            }
        }

        return mostCommon;
    }
}
