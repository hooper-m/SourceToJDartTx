

/**
 * This class provides examples of code to transform, and not to transform
 *  for the detection of if statements whose conditions are mutually exclusive.
 * Each method represents a single example where the transformation should,
 *  or should not apply.
 * In general, the transformation is as follows:
 * 
 *  if (condition1)
 *   	...
 *  if (condition2)
 *  	...
 *  
 *  ==>
 *  
 *  int branchesXX = 0;
 *  if (condition1) {
 *  	...
 *  	branchesXX++;
 *  }
 *  if (condition2) {
 *  	...
 *  	branchesXX++;
 *  }
 *  if (branchesXX == 1)
 *  	assert false;
 *  
 *  where branchesXX counts the number of branches reached in a single execution,
 *  and XX is the line number of the first if statement in the original source file.
 *  
 * For specific examples of how the transformation is applied, @see ExamplesTransformed.java
 *  
 * @author Matthew Hooper - 6/4/2020
 */
public class Examples {
	
	/// Examples of code to transform
	
	/**
	 * A basic example of code to be transformed
	 */
	public static int basicExample(int x) {
		int y = 0;
		if (x < 0) {
			y = 1;
		}
		if (x > 0) {
			y = -1;
		}
		return y;
	}
	
	/**
	 * A basic example of code to be transformed, without braces.
	 * It's important to ensure the transformed code includes braces
	 *  as the following
	 * 
	 *  if (x < 0)
	 *    y = 1;
	 *    branches++;
	 * 
	 * would be incorrect
	 */
	public static int basicExampleNoBraces(int x) {
		int y = 0;
		if (x < 0)
			y = 1;
		if (x > 0)
			y = -1;
		return y;
	}
	
	/**
	 * The if-else statement should not be transformed,
	 * but the pair of if statements should be
	 */
	public static int partialTxExample(int x, int y) {
		int z = 0;
		
		if (x % 2 == 0) {
			z = 2;
		}
		else {
			z = 1;
		}

		z *= 2;

		if (y > 0) {
			z++;
		}
		if (y < 0) {
			z--;
		}
		
		return z;
	}
	
	/**
	 * The transformation should apply to any number of consecutive if statements 
	 */
	public static int manyConsecutiveIfStatementsExample(int x) {
		int y = 0;
		if (x % 5 == 0) {
			y = 0;
		}
		if (x % 5 == 1) {
			y = 1;
		}
		if (x % 5 == 2) {
			y = 2;
		}
		if (x % 5 == 3) {
			y = 2;
		}
		if (x % 5 == 4) {
			y = 2;
		}				
		return y;
	}
	
	/**
	 * The transformation should apply to both pairs of if statements,
	 *  each with its own variable to count branches executed
	 */
	public static int multipleSectionsOfConsecutiveIfStatements(int x, int y) {
		int z = 0;
		
		if (x > 0) {
			z = 1;
		}
		if (x < 0) {
			z = -1;
		}
		
		z *= 2;
		
		if (y > 0) {
			z++;
		}
		if (y < 0) {
			z--;
		}
		return z;
	}
	
	/**
	 * Each pair of if statements should be transformed,
	 *  regardless of scope depth.
	 */
	public static int nestedScopesExample(int x) {
		int y = 0;
		if (x > 0) {
			for (int i = 0; i < x; i++) {
				if (i % 2 == 0) {
					y++;
				}
				if (i % 2 == 1) {
					y *= 2;
					{
						if (x / i > 5) {
							i++;
						}
						if (x / i < 5) {
							y--;
						}
					}
				}
			}
		}
		if (x < 0) {
			y = -1;
		}
		return y;
	}
	
	/**
	 * In the case where the final if statement contains an else branch,
	 *  we can still check if the pair are exclusive
	 */
	public static int endingElseStatement(int x) {
		int y = 0;
		if (x > 0) {
			y = 1;
		}
		if (x < 0) {
			y = -1;
		}
		else {
			y = 2;
		}
		return y;
	}
	
	/**
	 * In the case where we have
	 * 
	 *  if (p)
	 *  	...
	 *  else if (q)
	 *  	...
	 *  if (r)
	 *  	...
	 *  
	 *  we should check if conditions q and r are exclusive.
	 *  If so, we'd recommend transforming to
	 *  
	 *   if (p)
	 *   	...
	 *   else if (q)
	 *   	...
	 *   else if (r)
	 *   	...
	 */
	public static int elseIfFollowedByIf(int x) {
		int y = 0;
		
		if (x > 0) {
			y = 1;
		}
		else if (x < 0) {
			y = -1;
		}
		if (x == 0) {
			y = 0;
		}
		
		return y;
	}
	
	public static void lastStatementIsIfStatement(int x) {
		int y = 0;
		if (x > 0) {
			y = 1;
		}
		if (x < 0) {
			y = -1;
		}
		if (x == 0) {
			y = 0;
		}
	}
	
	/// Examples of code not to transform
	
	/**
	 * Single if statements should not be transformed 
	 */
	public static int doNotTxBasicExample(int x) {
		int y = 0;
		if (x > 0)
			y++;
		return y;
	}	
	
	/**
	 * A simple example of expert style, so this code should not be transformed
	 */
	public static int expertStyleExample(int x) {
		int y = 0;
		if (x > 0) {
			y = 1;
		}
		else if (x < 0) {
			y = -1;
		}
		return y;
	}
	
	/**
	 * Even though these if statements are exclusive, they should not be transformed,
	 *  since the first has an else statement as a child node.
	 * Changing the second if to an else-if would be a compile error,
	 *  and re-ordering the if statements would impose a semantic change in the code.
	 */
	public static int firstIfHasElseChild(int x) {
		int y = 0;
		if (x > 0) {
			y = 1;
		}
		else {
			y = 2;
		}
		if (x == 0) {
			y--;
		}
		return y;
	}
	
	/**
	 * In the case where any of the if statements exit the method,
	 *  we should ignore and leave the code as-is
	 */
	public static int ifReturnsExample(int x) {
		if (x < 0) {
			return 1;
		}
		if (x > 0) {
			return -1;
		}
		return 0;
	}
	
	/// Examples of code I'm not sure how to transform	
	/// (none right now)
}
