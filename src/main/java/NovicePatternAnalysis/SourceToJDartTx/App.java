package NovicePatternAnalysis.SourceToJDartTx;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
		Path src = Paths.get(args[0]);
//				Paths.get("C:\\Users\\Udnamtam\\Documents\\shool\\Research\\pattern-detection\\stripped_submissions\\"
//				+ "Assignment7\\Possum"
//				+ "\\src\\assignment7");
		
		String asgSubmittor = src.getName(src.getNameCount() - 2).toString() 
							+ "_"
							+ src.getName(src.getNameCount() - 1).toString();

		System.out.println(asgSubmittor);
		l.addInputResource(src.toString());
		
//		l.addInputResource("examples\\Examples.java");
		
		BranchCounterProcessor processor = new BranchCounterProcessor();
		l.addProcessor(processor);
		CtModel model = l.buildModel();
		l.process();
		
		if (processor.isModified()) {
			try(FileWriter out = new FileWriter(/*args[1] + */"tx\\" + asgSubmittor + "Tx.java")) {
				for (CtType<?> t : model.getAllTypes()) {
					out.write(t.toString());
				}
			}
		}
		else {
			System.out.println("- no chains found");
		}
		
//		if (processor.isModified()) {
//			for (CtType<?> t : model.getAllTypes()) {
//				try (FileWriter out = new FileWriter("tx\\" + t.getSimpleName() + "Tx.java")) {
//					out.write(t.toString());
//				}
//			}
//		}
//		System.out.println("done");
    }
}
