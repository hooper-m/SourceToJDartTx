package NovicePatternAnalysis.SourceToJDartTx;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.factory.Factory;
import spoon.support.reflect.code.CtIfImpl;

public class BranchCounterProcessor extends AbstractProcessor<CtBlock<?>> {

	@Override
	public void process(CtBlock<?> body) {
		boolean processed = false;

		String branch1 = "";
		String branch2 = "";

		List<CtStatement> statementList = body.getStatements();
		LinkedList<LinkedList<CtIfImpl>> ifChains = new LinkedList<>();
		LinkedList<CtIfImpl> chain = new LinkedList<>();

		for (int i = 0; i < statementList.size(); i++) {
			CtStatement statement = statementList.get(i);
			if (statement instanceof CtIfImpl) {
				chain.add((CtIfImpl) statement);
			}
			else if (chain.size() == 1) {
				chain.clear();
			}
			else if (chain.size() > 0) {
				ifChains.add(chain);
				chain = new LinkedList<>();
			}
		}

		if (chain.size() > 0) {
			ifChains.add(chain);
		}

		processIfChains(body, ifChains);
		int x = 0;
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
