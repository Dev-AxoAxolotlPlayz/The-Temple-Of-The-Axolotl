package com.devaxo.templeoftheaxolotl.promise;
import java.util.*;
import net.minecraft.world.phys.Vec3;
/** Immutable, client-readable snapshot; only the server assigns anchors. */
public record PromiseSnapshot(boolean equipped, boolean mobility, boolean mining, boolean combat, int upgrade, List<Contact> contacts) {
    public boolean enabled(int arm) { return switch (arm % 4) { case 0 -> combat; case 1 -> mining; default -> mobility; }; }
    public record Contact(int arm, Vec3 point, int face) {
        public Contact(int arm, Vec3 point) { this(arm, point, -1); }
    }
    public int face(int arm) { return contacts.stream().filter(c->c.arm()==arm).mapToInt(Contact::face).findFirst().orElse(-1); }
    public Vec3 target(int arm) { return contacts.stream().filter(c->c.arm()==arm).map(Contact::point).findFirst().orElse(null); }
    public PromiseSnapshot { contacts=List.copyOf(contacts); }
    public static final PromiseSnapshot EMPTY = new PromiseSnapshot(false,false,false,false,0,List.of());
}
