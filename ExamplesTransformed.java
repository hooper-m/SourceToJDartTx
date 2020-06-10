

/**
 * This class provides examples of code to transform, and not to transform
 *  for the detection of if statements whose conditions are mutually exclusive.
 * Each method represents a single example after the transformation has applied.
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
 * For specific examples of the code before transformation, @see Examples.java
 *  
 * @author Matthew Hooper - 6/4/2020
 */
public class ExamplesTransformed {
	
	/// Examples of code to transform
	
	/**
	 * A basic example of code to be transformed
	 */	
	public static int basicExampleTx(int x) {
		int y = 0;
		int branches45 = 0;
		if (x < 0) {
			y = 1;
			branches45++;
		}
		if (x > 0) {
			y = -1;
			branches45++;
		}
		if (branches45 == 1) {
			assert false;
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
	public static int basicExampleNoBracesTx(int x) {
		int y = 0;
		int branches67 = 0;
		if (x < 0) {			
			y = 1;
			branches67++;
		}
		if (x > 0) {
			y = -1;
			branches67++;
		}
		if (branches67 == 1) {
			assert false;
		}
		return y;
	}
	
	/**
	 * The if-else statement should not be transformed,
	 * but the pair of if statements should be
	 */
	public static int partialTxExampleTx(int x, int y) {
		int z = 0;
		
		if (x % 2 == 0) {
			z = 2;
		}
		else {
			z = 1;
		}

		z *= 2;

		int branches90 = 0;
		if (y > 0) {
			z++;
			branches90++;
		}
		if (y < 0) {
			z--;
			branches90++;
		}
		if (branches90 == 1) {
			assert false;
		}
		
		return z;
	}
	
	/**
	 * The transformation should apply to any number of consecutive if statements 
	 */
	public static int manyConsecutiveIfStatementsExampleTx(int x) {
		int y = 0;
		int branches105 = 0;
		if (x % 5 == 0) {
			y = 0;
			branches105++;
		}
		int branches108 = 0;
		if (x % 5 == 1) {
			y = 1;
			branches105++;
			branches108++;
		}
		if (branches105 == 1) {
			assert false;
		}
		int branches111 = 0;
		if (x % 5 == 2) {
			y = 2;
			branches108++;
			branches111++;
		}
		if (branches108 == 1) {
			assert false;
		}
		int branches114 = 0;
		if (x % 5 == 3) {
			y = 2;
			branches111++;
			branches114++;
		}
		if (branches111 == 1) {
			assert false;
		}
		if (x % 5 == 4) {
			y = 2;
			branches114++;			
		}				
		if (branches114 == 1) {
			assert false;
		}
		return y;
	}
	
	/**
	 * The transformation should apply to both pairs of if statements,
	 *  each with its own variable to count branches executed
	 */
	public static int multipleSectionsOfConsecutiveIfStatementsTx(int x, int y) {
		int z = 0;
		
		int branches130 = 0;		
		if (x > 0) {
			z = 1;
			branches130++;
		}
		if (x < 0) {
			z = -1;
			branches130++;
		}
		if (branches130 == 1) {
			assert false;
		}
		
		z *= 2;
		
		int branches139 = 0;		
		if (y > 0) {
			z++;
			branches139++;
		}
		if (y < 0) {
			z--;
			branches139++;
		}
		if (branches139 == 1) {
			assert false;
		}
		return z;
	}
	
	/**
	 * Each pair of if statements should be transformed,
	 *  regardless of scope depth.
	 */
	public static int nestedScopesExampleTx(int x) {
		int y = 0;
		int branches154 = 0;
		if (x > 0) {
			for (int i = 0; i < x; i++) {
				int branches156 = 0;
				if (i % 2 == 0) {
					y++;
					branches156++;					
				}
				if (i % 2 == 1) {
					y *= 2;
					{
						int branches162 = 0;
						if (x / i > 5) {
							i++;
							branches162++;
						}
						if (x / i < 5) {
							y--;
							branches162++;
						}
						if (branches162 == 1) {
							assert false;
						}
					}
					branches156++;
				}
				if (branches156 == 1) {
					assert false;
				}
			}
			branches154++;
		}
		if (x < 0) {
			y = -1;
			branches154++;
		}
		if (branches154 == 1) {
			assert false;
		}
		return y;
	}
	
	/**
	 * In the case where the final if statement contains an else branch,
	 *  we can still check if the pair are exclusive
	 */
	public static int endingElseStatementTx(int x) {
		int y = 0;
		int branches184 = 0;
		if (x > 0) {
			y = 1;
			branches184++;
		}
		if (x < 0) {
			y = -1;
			branches184++;
		}
		else {
			y = 2;
		}
		if (branches184 == 1) {
			assert false;
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
	 *  we should check if conditions q and r are exclusive/
	 *  If so, we'd recommend transforming to
	 *  
	 *   if (p)
	 *   	...
	 *   else if (q)
	 *   	...
	 *   else if (r)
	 *   	...
	 */
	public static int elseIfFollowedByIfTx(int x) {
		int y = 0;
		
		int branches222 = 0;
		if (x > 0) {
			y = 1;
		}
		else if (x < 0) {
			y = -1;
			branches222++;
		}
		if (x == 0) {
			y = 0;
			branches222++;
		}
		if (branches222 == 1) {
			assert false;
		}
		return y;
	}
	
	/// Examples of code not to transform
	
	/**
	 * Single if statements should not be transformed 
	 */	
	public static int doNotTxBasicExampleTx(int x) {
		int y = 0;
		if (x > 0) {
			y++;
		}
		return y;
	}	
	
	/**
	 * A simple example of expert style, so this code should not be transformed
	 */
	public static int expertStyleExampleTx(int x) {
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
	public static int firstIfHasElseChildTx(int x) {
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
	public static int ifReturnsExampleTx(int x) {
		/*
		 * In this particular example, the following re-factor would work.
		 * However, it's not immediately clear to me how well this approach can generalize,
		 *  and we are also not necessarily interested pedagogically. 
		 */
		int ret = 0;
		int branches283 = 0;
		if (x < 0) {
			ret = 1;
			branches283++;
		}
		if (x > 0) {
			ret =  -1;
			branches283++;
		}
		if (branches283 == 1) {
			assert false;
		}
		return ret;
	}
	
	/// Examples of code I'm not sure how to transform
	/// (none right now)
}
