package NovicePatternAnalysis.SourceToJDartTx;

import java.io.FileWriter;
import java.io.IOException;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtType;

/**
 * Hello world!
 *
 */
public class App {
    public static void main( String[] args ) throws IOException {
		Launcher l = new Launcher();
		// In the final version, paths to source files will be passed as command line arguments.
		String source = "Examples.java";
		l.addInputResource(source);
		l.addProcessor(new BranchCounterProcessor());		
		CtModel model = l.buildModel();
		l.process();
		
		try(FileWriter out = new FileWriter("out.java")) {
			for (CtType<?> t : model.getAllTypes()) {
				out.write(t.toString());
			}
		}
		
		System.out.println("done");
    }
}
