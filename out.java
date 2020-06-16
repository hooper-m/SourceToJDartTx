/**
 * This class provides examples of code to transform, and not to transform
 * for the detection of if statements whose conditions are mutually exclusive.
 * Each method represents a single example where the transformation should,
 * or should not apply.
 * In general, the transformation is as follows:
 *
 * if (condition1)
 * ...
 * if (condition2)
 * ...
 *
 * ==>
 *
 * int branchesXX = 0;
 * if (condition1) {
 * ...
 * branchesXX++;
 * }
 * if (condition2) {
 * ...
 * branchesXX++;
 * }
 * if (branchesXX == 1)
 * assert false;
 *
 * where branchesXX counts the number of branches reached in a single execution,
 * and XX is the line number of the first if statement in the original source file.
 *
 * For specific examples of how the transformation is applied, @see ExamplesTransformed.java
 *
 * @author Matthew Hooper - 6/4/2020
 */
// / Examples of code I'm not sure how to transform
// / (none right now)
public class Examples {
    // / Examples of code to transform
    /**
     * A basic example of code to be transformed
     */
    public static int basicExample(int x) {
        boolean branch48 = false;
        boolean branch45 = false;
        int y = 0;
        if (x < 0) {
            branch45 = true;
            y = 1;
        }
        if (x > 0) {
            branch48 = true;
            y = -1;
        }
        assert branch45 ^ branch48;
        return y;
    }

    /**
     * A basic example of code to be transformed, without braces.
     * It's important to ensure the transformed code includes braces
     * as the following
     *
     * if (x < 0)
     * y = 1;
     * branches++;
     *
     * would be incorrect
     */
    public static int basicExampleNoBraces(int x) {
        boolean branch69 = false;
        boolean branch67 = false;
        int y = 0;
        if (x < 0) {
            branch67 = true;
            y = 1;
        }
        if (x > 0) {
            branch69 = true;
            y = -1;
        }
        assert branch67 ^ branch69;
        return y;
    }

    /**
     * The if-else statement should not be transformed,
     * but the pair of if statements should be
     */
    public static int partialTxExample(int x, int y) {
        boolean branch93 = false;
        boolean branch90 = false;
        int z = 0;
        if ((x % 2) == 0) {
            z = 2;
        } else {
            z = 1;
        }
        z *= 2;
        if (y > 0) {
            branch90 = true;
            z++;
        }
        if (y < 0) {
            branch93 = true;
            z--;
        }
        assert branch90 ^ branch93;
        return z;
    }

    /**
     * The transformation should apply to any number of consecutive if statements
     */
    public static int manyConsecutiveIfStatementsExample(int x) {
        boolean branch117 = false;
        boolean branch114 = false;
        boolean branch111 = false;
        boolean branch108 = false;
        boolean branch105 = false;
        int y = 0;
        if ((x % 5) == 0) {
            branch105 = true;
            y = 0;
        }
        if ((x % 5) == 1) {
            branch108 = true;
            y = 1;
        }
        if ((x % 5) == 2) {
            branch111 = true;
            y = 2;
        }
        if ((x % 5) == 3) {
            branch114 = true;
            y = 2;
        }
        if ((x % 5) == 4) {
            branch117 = true;
            y = 2;
        }
        assert branch105 ^ branch108;
        assert branch108 ^ branch111;
        assert branch111 ^ branch114;
        assert branch114 ^ branch117;
        return y;
    }

    /**
     * The transformation should apply to both pairs of if statements,
     * each with its own variable to count branches executed
     */
    public static int multipleSectionsOfConsecutiveIfStatements(int x, int y) {
        boolean branch142 = false;
        boolean branch139 = false;
        boolean branch133 = false;
        boolean branch130 = false;
        int z = 0;
        if (x > 0) {
            branch130 = true;
            z = 1;
        }
        if (x < 0) {
            branch133 = true;
            z = -1;
        }
        z *= 2;
        if (y > 0) {
            branch139 = true;
            z++;
        }
        if (y < 0) {
            branch142 = true;
            z--;
        }
        assert branch130 ^ branch133;
        assert branch139 ^ branch142;
        return z;
    }

    /**
     * Each pair of if statements should be transformed,
     * regardless of scope depth.
     */
    public static int nestedScopesExample(int x) {
        boolean branch172 = false;
        boolean branch154 = false;
        int y = 0;
        if (x > 0) {
            branch154 = true;
            for (int i = 0; i < x; i++) {
                boolean branch159 = false;
                boolean branch156 = false;
                if ((i % 2) == 0) {
                    branch156 = true;
                    y++;
                }
                if ((i % 2) == 1) {
                    branch159 = true;
                    y *= 2;
                    {
                        boolean branch165 = false;
                        boolean branch162 = false;
                        if ((x / i) > 5) {
                            branch162 = true;
                            i++;
                        }
                        if ((x / i) < 5) {
                            branch165 = true;
                            y--;
                        }
                        assert branch162 ^ branch165;
                    }
                }
                assert branch156 ^ branch159;
            }
        }
        if (x < 0) {
            branch172 = true;
            y = -1;
        }
        assert branch154 ^ branch172;
        return y;
    }

    /**
     * In the case where the final if statement contains an else branch,
     * we can still check if the pair are exclusive
     */
    public static int endingElseStatement(int x) {
        boolean branch187 = false;
        boolean branch184 = false;
        int y = 0;
        if (x > 0) {
            branch184 = true;
            y = 1;
        }
        if (x < 0) {
            branch187 = true;
            y = -1;
        } else {
            y = 2;
        }
        assert branch184 ^ branch187;
        return y;
    }

    /**
     * In the case where we have
     *
     * if (p)
     * ...
     * else if (q)
     * ...
     * if (r)
     * ...
     *
     * we should check if conditions q and r are exclusive.
     * If so, we'd recommend transforming to
     *
     * if (p)
     * ...
     * else if (q)
     * ...
     * else if (r)
     * ...
     */
    public static int elseIfFollowedByIf(int x) {
        int y = 0;
        if (x > 0) {
            y = 1;
        } else if (x < 0) {
            y = -1;
        }
        if (x == 0) {
            y = 0;
        }
        return y;
    }

    public static void lastStatementIsIfStatement(int x) {
        boolean branch240 = false;
        boolean branch237 = false;
        boolean branch234 = false;
        int y = 0;
        if (x > 0) {
            branch234 = true;
            y = 1;
        }
        if (x < 0) {
            branch237 = true;
            y = -1;
        }
        if (x == 0) {
            branch240 = true;
            y = 0;
        }
        assert branch234 ^ branch237;
        assert branch237 ^ branch240;
    }

    // / Examples of code not to transform
    /**
     * Single if statements should not be transformed
     */
    public static int doNotTxBasicExample(int x) {
        int y = 0;
        if (x > 0) {
            y++;
        }
        return y;
    }

    /**
     * A simple example of expert style, so this code should not be transformed
     */
    public static int expertStyleExample(int x) {
        int y = 0;
        if (x > 0) {
            y = 1;
        } else if (x < 0) {
            y = -1;
        }
        return y;
    }

    /**
     * Even though these if statements are exclusive, they should not be transformed,
     * since the first has an else statement as a child node.
     * Changing the second if to an else-if would be a compile error,
     * and re-ordering the if statements would impose a semantic change in the code.
     */
    public static int firstIfHasElseChild(int x) {
        int y = 0;
        if (x > 0) {
            y = 1;
        } else {
            y = 2;
        }
        if (x == 0) {
            y--;
        }
        return y;
    }

    /**
     * In the case where any of the if statements exit the method,
     * we should ignore and leave the code as-is
     */
    public static int ifReturnsExample(int x) {
        boolean branch299 = false;
        boolean branch296 = false;
        if (x < 0) {
            branch296 = true;
            return 1;
        }
        if (x > 0) {
            branch299 = true;
            return -1;
        }
        assert branch296 ^ branch299;
        return 0;
    }
}