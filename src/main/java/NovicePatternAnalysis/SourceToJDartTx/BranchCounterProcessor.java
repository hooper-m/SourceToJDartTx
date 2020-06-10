package NovicePatternAnalysis.SourceToJDartTx;

import java.util.List;

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
		Factory factory = body.getFactory();

		String branch1 = "";
		String branch2 = "";

		List<CtStatement> statementList = body.getStatements();

		for (int i = 0; i < statementList.size() - 1; i++) {
			CtStatement statement = statementList.get(i);
			if (statement instanceof CtIfImpl) {
				CtIfImpl ifStatement = (CtIfImpl) statement;
				CtStatement nextStatement = statementList.get(i + 1);
				if (nextStatement instanceof CtIfImpl) {
					CtIfImpl nextIfStatement = (CtIfImpl) nextStatement;
					processed = true;

					branch1 = "branch" + ifStatement.getPosition().getLine();
					branch2 = "branch" + nextIfStatement.getPosition().getLine();
					ifStatement.getThenStatement()
							.insertBefore(factory.Code().createCodeSnippetStatement(branch1 + " = true"));
					nextIfStatement.getThenStatement()
							.insertBefore(factory.Code().createCodeSnippetStatement(branch2 + " = true"));
				}
			}
		}

		if (processed) {
			body.insertBegin(factory.Code().createCodeSnippetStatement("boolean " + branch2 + " = false"));
			body.insertBegin(factory.Code().createCodeSnippetStatement("boolean " + branch1 + " = false"));
			CtCodeSnippetStatement assertXor = factory.Code()
					.createCodeSnippetStatement("assert " + branch1 + " ^ " + branch2);
			if (body.getLastStatement() instanceof CtReturn<?>) {
				body.addStatement(statementList.size() - 1, assertXor);
			}
			else {
				body.insertEnd(assertXor);
			}
		}
	}
}
