package NovicePatternAnalysis.SourceToJDartTx;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtBreak;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.code.CtContinue;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtThrow;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.factory.Factory;
import spoon.support.reflect.code.CtBlockImpl;
import spoon.support.reflect.code.CtIfImpl;

public class BranchCounterProcessor extends AbstractProcessor<CtBlock<?>> {
	
	private boolean modified = false;
	
	public boolean isModified() {
		return modified;
	}

	@Override
	public void process(CtBlock<?> body) {
		List<CtStatement> statementList = body.getStatements();
		LinkedList<LinkedList<CtIfImpl>> ifChains = new LinkedList<>();
		LinkedList<CtIfImpl> chain = new LinkedList<>();

		for (int i = 0; i < statementList.size(); i++) {
			CtStatement statement = statementList.get(i);
			if (statement instanceof CtIfImpl) {				
				CtIfImpl ifStatement = (CtIfImpl) statement;
				
				// Add body with no-op statement to prevent future index out of bounds and npe's
				if (ifStatement.getThenStatement() == null) {
					CtBlockImpl<Object> dummyBlock = new CtBlockImpl<>();
					dummyBlock.insertBegin(body.getFactory().Code().createCodeSnippetStatement(""));
					ifStatement.setThenStatement(dummyBlock);
				}
				
				if (!containsBranchingControlFlowStatements(ifStatement)) {
					chain.add(ifStatement);
				}
				
				CtBlock<?> elseStatement = ifStatement.getElseStatement();
				
				if (elseStatement != null) {
					if (chain.size() > 1) {
						ifChains.add(chain);
						printFileAndLine(chain);						
					}
					
					chain = new LinkedList<>();
					
					CtIfImpl deepestElseIfChild = findDeepestElseIfStatement(elseStatement);
					if (deepestElseIfChild != null) {
						chain.add(deepestElseIfChild);
					}
				}
			}
			else if (chain.size() == 1) {
				chain.clear();
			}
			else if (chain.size() > 1) {
				ifChains.add(chain);
				printFileAndLine(chain);
				chain = new LinkedList<>();				
			}
		}

		if (chain.size() > 1) {
			ifChains.add(chain);
			printFileAndLine(chain);
		}

		if (ifChains.size() > 0) {
			processIfChains(body, ifChains);
			modified = true;
		}
	}
	
	private CtIfImpl findDeepestElseIfStatement(CtBlock<?> elseStatement) {
		if (elseStatement != null
				&& elseStatement.getStatements().size() == 1
				&& elseStatement.getStatement(0) instanceof CtIfImpl) {
			CtIfImpl childIfStatement = (CtIfImpl) elseStatement.getStatement(0);
			return childIfStatement.getElseStatement() == null ?
				   childIfStatement :
				   findDeepestElseIfStatement(childIfStatement.getElseStatement());
		}

		return null;
	}
	
	private boolean containsBranchingControlFlowStatements(CtIfImpl ifStatement) {
		CtStatement thenStatement = ifStatement.getThenStatement();
		
		if (thenStatement instanceof CtBlock<?>) {
			CtBlock<?> thenBlock = (CtBlock<?>) thenStatement;
			return thenBlock.getStatements().stream()
					.anyMatch(statement -> isBranchingControlFlowStatement(statement));
		}
		
		return isBranchingControlFlowStatement(thenStatement);
	}
	
	private boolean isBranchingControlFlowStatement(CtStatement statement) {
		return statement instanceof CtReturn<?>
				|| statement instanceof CtThrow
				|| statement instanceof CtContinue
				|| statement instanceof CtBreak;
	}

	private void printFileAndLine(LinkedList<CtIfImpl> chain) {
		CtElement parent = chain.getFirst().getParent();
		while (!(parent instanceof CtClass<?>)) {
			parent = parent.getParent();
		}
		CtClass<?> clas = (CtClass<?>) parent;
		
		boolean ret = false;
		
		for (CtIfImpl ifStatement : chain) {
			if (ifStatement.getThenStatement() instanceof CtBlock<?>) {
				CtBlock<?> thenBlock = (CtBlock<?>) ifStatement.getThenStatement();
				if (thenBlock.getStatements().size() > 0 && thenBlock.getLastStatement() instanceof CtReturn<?>) {
					ret = true;
					break;
				}
			}
			else if (ifStatement.getThenStatement() instanceof CtReturn<?>) {
				ret = true;
				break;
			}
		}
		
		String message = "- chain found at " + clas.getSimpleName() + ":"
				+ chain.getFirst().getPosition().getLine();
		
		if (ret) {
			message += " (r)";
		}
		
		System.out.println(message);

	}
	
	private void processIfChains(CtBlock<?> body, LinkedList<LinkedList<CtIfImpl>> ifChains) {
		Factory factory = body.getFactory();
		for (LinkedList<CtIfImpl> chain : ifChains) {
			Iterator<CtIfImpl> iter = chain.iterator();
			CtIfImpl prev = iter.next();

			String prevBranch = "branch" + prev.getPosition().getLine();
			prev.getThenStatement().insertBefore(factory.Code().createCodeSnippetStatement(prevBranch + " = true"));
			body.insertBegin(factory.Code().createCodeSnippetStatement("boolean " + prevBranch + " = false"));

			while (iter.hasNext()) {
				CtIfImpl curr = iter.next();
				String currBranch = "branch" + curr.getPosition().getLine();
				curr.getThenStatement().insertBefore(factory.Code().createCodeSnippetStatement(currBranch + " = true"));
				body.insertBegin(factory.Code().createCodeSnippetStatement("boolean " + currBranch + " = false"));

				CtCodeSnippetStatement assertXor = factory.Code()
						.createCodeSnippetStatement("assert " + prevBranch + " ^ " + currBranch);
				if (body.getLastStatement() instanceof CtReturn<?>) {
					body.addStatement(body.getStatements().size() - 1, assertXor);
				}
				else {
					body.insertEnd(assertXor);
				}

				prevBranch = currBranch;
			}
		}
	}
}
