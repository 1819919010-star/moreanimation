package com.github.JumDa5he.moreanimation.compat.ysm;

/** Read-only subset used by our exported clips. No shared Gecko Molang variables are modified. */
final class YsmKeyframeExpression {
    record Context(double animationTime, double health, double maxHealth) {}
    interface Value { double get(Context context); }

    private final String source;
    private int cursor;

    private YsmKeyframeExpression(String source) { this.source = source; }

    static Value compile(String source) {
        YsmKeyframeExpression parser = new YsmKeyframeExpression(source);
        Value result = parser.conditional();
        parser.space();
        if (parser.cursor != source.length()) throw parser.error();
        return result;
    }

    private Value conditional() {
        Value condition = comparison();
        if (!take("?")) return condition;
        Value yes = conditional();
        require(":");
        Value no = conditional();
        return c -> condition.get(c) != 0 ? yes.get(c) : no.get(c);
    }

    private Value comparison() {
        Value result = sum();
        while (true) {
            String op = operator("<=", ">=", "==", "!=", "<", ">");
            if (op == null) return result;
            Value left = result, right = sum();
            result = c -> switch (op) {
                case "<" -> left.get(c) < right.get(c) ? 1 : 0;
                case ">" -> left.get(c) > right.get(c) ? 1 : 0;
                case "<=" -> left.get(c) <= right.get(c) ? 1 : 0;
                case ">=" -> left.get(c) >= right.get(c) ? 1 : 0;
                case "==" -> left.get(c) == right.get(c) ? 1 : 0;
                default -> left.get(c) != right.get(c) ? 1 : 0;
            };
        }
    }

    private Value sum() {
        Value result = product();
        while (true) {
            String op = operator("+", "-");
            if (op == null) return result;
            Value left = result, right = product();
            result = op.equals("+") ? c -> left.get(c) + right.get(c)
                    : c -> left.get(c) - right.get(c);
        }
    }

    private Value product() {
        Value result = primary();
        while (true) {
            String op = operator("*", "/");
            if (op == null) return result;
            Value left = result, right = primary();
            result = op.equals("*") ? c -> left.get(c) * right.get(c)
                    : c -> left.get(c) / right.get(c);
        }
    }

    private Value primary() {
        if (take("-")) { Value value = primary(); return c -> -value.get(c); }
        if (take("+")) return primary();
        if (take("(")) { Value value = conditional(); require(")"); return value; }
        space();
        int start = cursor;
        if (cursor < source.length() && (Character.isDigit(source.charAt(cursor)) || source.charAt(cursor) == '.')) {
            while (cursor < source.length() && (Character.isDigit(source.charAt(cursor)) || source.charAt(cursor) == '.')) cursor++;
            if (cursor < source.length() && (source.charAt(cursor) == 'e' || source.charAt(cursor) == 'E')) {
                cursor++;
                if (cursor < source.length() && (source.charAt(cursor) == '+' || source.charAt(cursor) == '-')) cursor++;
                while (cursor < source.length() && Character.isDigit(source.charAt(cursor))) cursor++;
            }
            double value = Double.parseDouble(source.substring(start, cursor));
            if (!Double.isFinite(value)) throw error();
            return c -> value;
        }
        while (cursor < source.length() && (Character.isLetterOrDigit(source.charAt(cursor))
                || source.charAt(cursor) == '_' || source.charAt(cursor) == '.')) cursor++;
        String identifier = source.substring(start, cursor);
        if (identifier.equals("math.sin") || identifier.equals("math.cos")) {
            require("("); Value argument = conditional(); require(")");
            // Molang trigonometric functions take degrees, unlike java.lang.Math.
            return identifier.equals("math.sin") ? c -> Math.sin(Math.toRadians(argument.get(c)))
                    : c -> Math.cos(Math.toRadians(argument.get(c)));
        }
        return switch (identifier) {
            case "q.anim_time", "query.anim_time" -> Context::animationTime;
            case "q.health", "query.health" -> Context::health;
            case "q.max_health", "query.max_health" -> c -> Math.max(0.0001, c.maxHealth());
            default -> throw error();
        };
    }

    private String operator(String... options) {
        for (String option : options) if (take(option)) return option;
        return null;
    }
    private void space() { while (cursor < source.length() && Character.isWhitespace(source.charAt(cursor))) cursor++; }
    private boolean take(String token) {
        space();
        if (!source.startsWith(token, cursor)) return false;
        cursor += token.length();
        return true;
    }
    private void require(String token) { if (!take(token)) throw error(); }
    private IllegalArgumentException error() {
        return new IllegalArgumentException("Unsupported keyframe expression at " + cursor + ": " + source);
    }
}
