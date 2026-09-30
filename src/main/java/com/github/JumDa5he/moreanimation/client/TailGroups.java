package com.github.JumDa5he.moreanimation.client;

import java.util.*;


public final class TailGroups {
    public record Node(String name, String parent, boolean geometry) {}
    public record Group(String id, List<String> chain) {}
    public record Binding(String tailId, int segment) {}
    public record Layout(List<Group> groups, Map<String, Binding> bindings) {}

    public static boolean tail(String name, Map<String, Node> nodes) {
        String n = name.toLowerCase(Locale.ROOT);


        if (n.matches("(?:tail|body_tail)[0-9]*")) return true;

        if (n.matches("wb[0-9]*")) {
            Node b = nodes.get(name);
            Set<String> seen = new HashSet<>();
            while (b != null && seen.add(b.name())) {
                if (b.name().equalsIgnoreCase("FOX")) return true;
                b = nodes.get(b.parent());
            }
        }
        return false;
    }

    public static Layout build(Collection<Node> source) {
        Map<String, Node> nodes = new LinkedHashMap<>();
        source.forEach(n -> nodes.put(n.name(), n));
        Map<String, List<String>> children = new HashMap<>();
        for (Node n : source) if (tail(n.name(), nodes))
            children.computeIfAbsent(n.parent(), k -> new ArrayList<>()).add(n.name());
        List<Group> groups = new ArrayList<>();
        Map<String, Binding> bindings = new HashMap<>();
        for (Node n : source) {
            if (!tail(n.name(), nodes)) continue;
            if (tail(n.parent(), nodes) && children.getOrDefault(n.parent(), List.of()).size() == 1) continue;

            if (children.getOrDefault(n.name(), List.of()).size() > 1) continue;
            List<String> chain = new ArrayList<>();
            String next = n.name();
            while (next != null && !chain.contains(next)) {
                chain.add(next);
                List<String> c = children.getOrDefault(next, List.of());
                next = c.size() == 1 ? c.get(0) : null;
            }
            if (chain.stream().noneMatch(b -> nodes.get(b).geometry())) continue;
            String id = n.name();
            groups.add(new Group(id, List.copyOf(chain)));
            for (int i = 0; i < chain.size(); i++) bindings.put(chain.get(i), new Binding(id, Math.min(i, 6)));
        }

        for (Node n : source) {
            String lower = n.name().toLowerCase(Locale.ROOT);
            if (!lower.matches("ysmglowtail[0-9]+")) continue;
            int number = Integer.parseInt(lower.substring(11)) - 63;
            Binding original = bindings.get(number == 1 ? "Tail" : "Tail" + number);
            if (original != null) bindings.put(n.name(), original);
        }
        groups.sort(Comparator.comparing(Group::id));
        return new Layout(List.copyOf(groups), Map.copyOf(bindings));
    }
}
