package dev.epicduels.manager;

/** Physical slot contracts shared by menu decoration and its regression checks. */
public final class MenuLayout {
    private MenuLayout() { }
    public static boolean valid(boolean editor, boolean paginated, int size, java.util.Map<Integer, Integer> destinations) {
        java.util.Set<Integer> used = new java.util.HashSet<>();
        for (var entry : destinations.entrySet()) {
            if (!allowed(editor, paginated, size, entry.getKey(), entry.getValue()) || !used.add(entry.getValue())) return false;
        }
        return true;
    }
    public static boolean allowed(boolean editor, boolean paginated, int size, int source, int target) {
        if (target < (editor ? 41 : 0) || target >= size) return false;
        boolean content = source >= 9 && source < 45;
        return !paginated || (content ? target == source : target < 9 || target >= 45);
    }
}
