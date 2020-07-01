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
		
		public void process() {
			for (CtStatement statement : body.getStatements()) {
				if (statement instanceof CtIf) {
					processIfStatement((CtIf) statement);
				}
				else if (currentChain.size() > 0) {
					if (currentChain.size() > 1) {
						addChain();
					}
					currentChain = new LinkedList<>();
				}
			}
			
			if (currentChain.size() > 1) {
				addChain();
			}
			
			if (allChains.size() > 0) {
				insertNewStatements();
				modified = true;
			}
		}
		
		private void addChain() {
			allChains.add(currentChain);
			if (LOG) {
				printParentClassAndLineNumber(currentChain.getFirst());
			}
		}
		
		private void processIfStatement(CtIf ifStatement) {
			if (ifStatement.getThenStatement() == null) {
				CtBlock<?> noOpBlock = new CtBlockImpl<>();
				noOpBlock.insertBegin(factory.createCodeSnippetStatement(""));
				ifStatement.setThenStatement(noOpBlock);
			}
			if (!containsBranchingControlFlowStatements(ifStatement.getThenStatement())) {
				currentChain.add(ifStatement);
			}
			
			CtStatement elseStatement = ifStatement.getElseStatement();
			
			if (elseStatement instanceof CtBlock<?>) {
				processElseStatement((CtBlock<?>) elseStatement);
			}
		}
		
		private void processElseStatement(CtBlock<?> elseStatement) {
			if (currentChain.size() > 1) {
				addChain();				
			}
			
			currentChain = new LinkedList<>();
			
			CtIf deepestElseIfChild = findDeepestElseIfStatement(elseStatement);
			if (deepestElseIfChild != null) {
				currentChain.add(deepestElseIfChild);
			}
		}
		
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
		
		private String insertAssignAndQueueInitStatements(CtIf ifStatement, CtStatementListImpl<?> initQueue) {
			String branchNumber = "branch" + ifStatement.getPosition().getLine();
			String initStatement = "boolean " + branchNumber + " = false";
			String assignStatement = branchNumber + " = true";
			ifStatement.getThenStatement().insertBefore(factory.createCodeSnippetStatement(assignStatement));
			initQueue.addStatement(factory.createCodeSnippetStatement(initStatement));
			return branchNumber;
		}
	}
	
	private boolean isBranchingControlFlowStatement(CtStatement statement) {
		return statement instanceof CtReturn<?>
				|| statement instanceof CtThrow
				|| statement instanceof CtContinue
				|| statement instanceof CtBreak;
	}
	
	private boolean containsBranchingControlFlowStatements(CtStatement statement) {
		if (statement instanceof CtStatementList) {
			return ((CtStatementList) statement).getStatements().stream()
					.anyMatch(childStatement -> isBranchingControlFlowStatement(childStatement));
		}
		
		return isBranchingControlFlowStatement(statement);
	}
	
	private CtIf findDeepestElseIfStatement(CtBlock<?> elseStatement) {
		if (elseStatement.getStatements().size() == 1
			&& elseStatement.getStatement(0) instanceof CtIf) {
			
			CtIf childIfStatement = (CtIf) elseStatement.getStatement(0);
			return childIfStatement.getElseStatement() == null
				   ? childIfStatement
				   : findDeepestElseIfStatement(childIfStatement.getElseStatement());
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
