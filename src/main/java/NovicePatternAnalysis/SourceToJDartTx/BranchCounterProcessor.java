package NovicePatternAnalysis.SourceToJDartTx;

import java.util.Iterator;
import java.util.LinkedList;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtBreak;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.code.CtContinue;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtStatementList;
import spoon.reflect.code.CtThrow;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.factory.CodeFactory;
import spoon.support.reflect.code.CtBlockImpl;
import spoon.support.reflect.code.CtStatementListImpl;

public class BranchCounterProcessor extends AbstractProcessor<CtBlock<?>> {
	
	private boolean modified = false;
	private final boolean LOG = true;

	public boolean isModified() {
		return modified;
	}

	@Override
	public void process(CtBlock<?> body) {
		IfChainProcessor processor = new IfChainProcessor(body);
		processor.process();
	}
	
	/**
	 * Class to build a list of "if chains", and insert statements needed for dynamic analysis.
	 * 
	 * An "if chain" is defined as a 2 or more consecutive if statements that contain no
	 *  branching control flow statements, e.g. 'return', 'throw', 'break', 'continue'.
	 * There must not be any other kinds of statements between each pair of if statements.
	 * 'if else' may only be the start of an 'if chain', and only if followed by an 'if' statement.
	 * 
	 * The following are examples of if chains:
	 * 
	 * if (p) {[some work]}
	 * if (q) {[some work]}
	 * --
	 * else if (p) {[some work]}
	 * if (q) {[some work]}
	 * 
	 * The following are NOT examples of if chains:
	 * 
	 * if (p) {[some work]}
	 * if (q) break;
	 * if (r) {[some work]}
	 * --
	 * if (p) {[some work]}
	 * else if (q) {[some work]}
	 * --
	 * if (p) {[some work]}
	 * [some other work]
	 * if (q) {[some work]}
	 */
	class IfChainProcessor {
		private CtBlock<?> body;
		private CodeFactory factory;
		private LinkedList<CtIf> currentChain;
		private LinkedList<LinkedList<CtIf>> allChains;
		
		public IfChainProcessor(CtBlock<?> body) {
			this.body = body;
			factory = body.getFactory().Code();
			currentChain = new LinkedList<>();
			allChains = new LinkedList<>();
		}
		
		/**
		 * Finds all if chains, then inserts the new statements
		 */
		public void process() {
			for (CtStatement statement : body.getStatements()) {
				if (isPartOfIfChain(statement)) {
					CtIf ifStatement = (CtIf) statement;
					currentChain.add(ifStatement);
					CtStatement elseStatement = ifStatement.getElseStatement();
					if (elseStatement instanceof CtBlock<?>) {
						startNewIfChain((CtBlock<?>) elseStatement);
					}
				}
				else if (currentChain.size() > 0) {
					completeCurrentChain();
				}
			}
			
			completeCurrentChain();
			
			if (allChains.size() > 0) {
				insertNewStatements();
				modified = true;
			}
		}
		
		/**
		 * @param statement
		 * @return True if the statement should be considered part of an if chain,
		 *  i.e. if statement is an 'if statement'
		 *  whose body contains no branching control flow statements
		 */
		private boolean isPartOfIfChain(CtStatement statement) {
			if (!(statement instanceof CtIf)) {
				return false;
			}
			CtIf ifStatement = (CtIf) statement;
			if (ifStatement.getThenStatement() == null) {
				CtBlock<?> noOpBlock = new CtBlockImpl<>();
				noOpBlock.insertBegin(factory.createCodeSnippetStatement(""));
				ifStatement.setThenStatement(noOpBlock);
			}
			
			return !containsBranchingControlFlowStatements(ifStatement.getThenStatement());
		}

		/**
		 * Adds the current chain to the list of all chains,
		 *  then initializes a new chain.
		 */
		private void completeCurrentChain() {
			if (currentChain.size() > 1) {
				allChains.add(currentChain);
				if (LOG) {
					printParentClassAndLineNumber(currentChain.getFirst());
				}
			}
			
			currentChain = new LinkedList<>();
		}
		
		/**
		 * Completes the current chain, then checks elseStatement
		 * for an 'else if' statement which which to begin a new chain.
		 * @param elseStatement
		 */
		private void startNewIfChain(CtBlock<?> elseStatement) {
			completeCurrentChain();
			
			CtIf deepestElseIfChild = findDeepestElseIfStatement(elseStatement);
			if (deepestElseIfChild != null) {
				currentChain.add(deepestElseIfChild);
			}
		}
		
		/**
		 * Inserts the initialization, assignment, and assertion statements
		 *  needed for dynamic analysis to all if chains found.
		 */
		private void insertNewStatements() {
			CtStatementListImpl<?> initQueue = new CtStatementListImpl<>();
			for (LinkedList<CtIf> chain : allChains) {
				Iterator<CtIf> iter = chain.iterator();
				CtIf start = iter.next();
				String prevBranchNumber = insertAssignAndQueueInitStatements(start, initQueue);
				
				while (iter.hasNext()) {
					CtIf curr = iter.next();
					String currBranchNumber = insertAssignAndQueueInitStatements(curr, initQueue);
					CtCodeSnippetStatement assertXor
						= factory.createCodeSnippetStatement("assert " + prevBranchNumber + " ^ " + currBranchNumber);
					
					int assertIndex = isBranchingControlFlowStatement(this.body.getLastStatement())
							? this.body.getStatements().size() - 1
							: this.body.getStatements().size();
							
					this.body.addStatement(assertIndex, assertXor);

					prevBranchNumber = currBranchNumber;
				}
			}
			this.body.insertBegin(initQueue);
		}
		
		/**
		 * Inserts 'branchXX = true' into the ifStatement body.
		 * Adds the statement 'boolean branchXX = false' to a queue for later insertion.
		 * @param ifStatement - statement associated with the branch variable
		 * @param initQueue - queue of initialization statements
		 * @return "branchXX", where 'XX' is the line number of the if statement in the original source file 
		 */
		private String insertAssignAndQueueInitStatements(CtIf ifStatement, CtStatementListImpl<?> initQueue) {
			String branchNumber = "branch" + ifStatement.getPosition().getLine();
			String initStatement = "boolean " + branchNumber + " = false";
			String assignStatement = branchNumber + " = true";
			ifStatement.getThenStatement().insertBefore(factory.createCodeSnippetStatement(assignStatement));
			initQueue.addStatement(factory.createCodeSnippetStatement(initStatement));
			return branchNumber;
		}
	}
	
	/**
	 * @return true if statement is 'return', 'throw', 'break', or 'continue'
	 */
	private boolean isBranchingControlFlowStatement(CtStatement statement) {
		return statement instanceof CtReturn<?>
				|| statement instanceof CtThrow
				|| statement instanceof CtBreak
				|| statement instanceof CtContinue;
	}
	
	/**
	 * @return true if statement is 'return', 'throw', 'break', or 'continue',
	 *  or if statement is statement list containing any such statements
	 */
	private boolean containsBranchingControlFlowStatements(CtStatement statement) {
		if (statement instanceof CtStatementList) {
			return ((CtStatementList) statement).getStatements().stream()
					.anyMatch(childStatement -> isBranchingControlFlowStatement(childStatement));
		}
		
		return isBranchingControlFlowStatement(statement);
	}
	
	private CtIf findDeepestElseIfStatement(CtBlock<?> elseStatement) {
		while (elseStatement.getStatements().size() == 1
				&& elseStatement.getStatement(0) instanceof CtIf) {
			CtIf childIfStatement = (CtIf) elseStatement.getStatement(0);
			if (childIfStatement.getElseStatement() == null) {
				return childIfStatement;
			}
			else {
				elseStatement = childIfStatement.getElseStatement();
			}
		}
		
		return null;
	}
	
	private void printParentClassAndLineNumber(CtStatement statement) {
		CtElement parent = statement;
		while (!(parent instanceof CtClass<?>)) {
			parent = parent.getParent();
		}
		String className = ((CtClass<?>) parent).getSimpleName();
		String message = "- chain found at " + className + ":"
						+ statement.getPosition().getLine();
		System.out.println(message);
	}
}
