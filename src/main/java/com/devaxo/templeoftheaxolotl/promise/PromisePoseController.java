package com.devaxo.templeoftheaxolotl.promise;

import java.util.HashMap;
import java.util.Map;

/**
 * Pure pose compositor. Angles are Blockbench degrees; the renderer converts to radians.
 * Feed the exact authored zero-time animation rotations as basePose.
 * This class intentionally does not control movement, claim world anchors or render by itself.
 */
public final class PromisePoseController {
    private PromisePoseController() {}

    public record Rotation(double x, double y, double z) {}

    public static Map<String, Rotation> compose(Map<String, Rotation> basePose,
                                                 boolean dormant,
                                                 double leftArmBackward,
                                                 double rightArmBackward,
                                                 boolean leftHandUsing,
                                                 boolean rightHandUsing,
                                                 boolean leftTridentCharge,
                                                 boolean rightTridentCharge,
                                                 double itemUseBlend) {
        Map<String, Rotation> result = new HashMap<>(basePose);
        double blend = Math.max(0, Math.min(1, itemUseBlend));
        for (PromiseRig.Chain chain : PromiseRig.CHAINS) {
            double backward = chain.left() ? leftArmBackward : rightArmBackward;
            double walk = PromiseRig.walkingRootOffset(chain, backward);
            boolean using = chain.left() ? leftHandUsing : rightHandUsing;
            boolean trident = chain.left() ? leftTridentCharge : rightTridentCharge;
            for (int i = 0; i < chain.size(); i++) {
                String name = chain.bones().get(i);
                Rotation base = basePose.getOrDefault(name, new Rotation(0, 0, 0));
                double y = base.y() + (i == 0 ? walk : 0);
                if (dormant && using && PromiseRig.opensForItemUse(chain, i, trident))
                    y *= 1 - blend;
                // Authored idle angles are preserved, except for the specified procedural adjustments.
                result.put(name, new Rotation(base.x(), y, base.z()));
            }
        }
        return result;
    }
}
