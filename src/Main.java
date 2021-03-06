import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class Main{
	static void replaceSlashes(ArrayList<ContainingFile> Files){
		Iterator<ContainingFile> it= Files.iterator();
		while(it.hasNext()){
			ContainingFile f=it.next();
			String st=f.getFileName();
			while(st.contains("\\")){
				st=st.replace("\\","/");
			}
			f.setFileName(st);
		}
	}

	static String getCommonFromAll(ArrayList<ContainingFile> files){
		String common="";
		Iterator<ContainingFile> it=files.iterator();
		ContainingFile firstFile=null;
		if(it.hasNext()) {
			firstFile=it.next();
			common=firstFile.getFileName();
		}
		while(it.hasNext()){
			ContainingFile f=it.next();
			if(f.getFileName()!=null) common=getCommonFromTwoStrings(common,f.getFileName());
		}
		return common;
	}

	public static String getCommonBasedOnFileSeparators(String common){
		String newCommon="";
		String separator="";
		int size=common.length();
		for(int i=0;i<size;i++){
			char c=common.charAt(i);
			if(c=='/'){
				newCommon=newCommon+separator;
				separator="";
			}
			else separator=separator+c;
		}
		return newCommon;
	}

	static String getCommonFromTwoStrings(String a,String b){
		String st="";
		for(int i=0;i<a.length();i++){
			if(i>b.length()) return st;
			if(a.charAt(i)==b.charAt(i)) st=st+a.charAt(i);
			else return st;
		}
		return st;
	}

	static void replaceSame(ArrayList<ContainingFile> files){
		String st=getCommonBasedOnFileSeparators(getCommonFromAll(files));
		Iterator<ContainingFile> it=files.iterator();
		while(it.hasNext()){
			ContainingFile f=it.next();
			String name=f.getFileName();
			name=name.replace(st,"");
			f.setFileName(name);
		}
	}

	static String outputRelationsName(String args) {
		String st="";
		st=args.replace(".mse", "-relations.csv");
		st=st.replace(".MSE", "-relations.csv");
		return st;
	}

    static String outputMetricsName(String fileName){
	    String st="";
	    st=fileName.replace(".mse","-metrics.csv");
	    st=st.replace(".mse","-metrics.csv");
	    return st;
    }

    static void beginMetricsComputation(Interpreter i,String fileName) throws Exception{
        FileWriter ClassMetricsWriter=new FileWriter(outputMetricsName(fileName));
        PrintWriter ClassMetricsPrinter=new PrintWriter(ClassMetricsWriter);
        ClassMetricsPrinter.print(i.getClassMetrics());
        ClassMetricsPrinter.close();
    }

    static String outputExternalsName(String args){
	    String st="";
	    st=args.replace(".mse","-externals.JSON");
	    st=st.replace(".MSE","-externals.JSON");
	    return st;
    }

    static void beginExternalsComputation(Interpreter i,String fileName) throws  Exception{
	    FileWriter ClassExternalsWriter=new FileWriter(outputExternalsName(fileName));
	    PrintWriter ClassExternalsPrinter=new PrintWriter(ClassExternalsWriter);
	    ClassExternalsPrinter.print(i.getFileExternals());
	    ClassExternalsPrinter.close();
    }

    static String outputInheritanceName(String fileName){
		String st="";
		st=fileName.replace(".mse","-inhRelations.csv");
		st=st.replace(".MSE","-inhRelations.csv");
		return st;
	}

    static void beginClassInheritanceComputation(Interpreter i, String fileName) throws Exception{
		FileWriter ClassInhWriter = new FileWriter(outputInheritanceName(fileName));
		PrintWriter ClassInhPrinter = new PrintWriter(ClassInhWriter);
		ClassInhPrinter.print(i.getClassHierarchy());
		ClassInhPrinter.close();
	}

	static String outputIrregName(String fileName){
		String st="";
		st=fileName.replace(".mse","-methodIrregularities.csv");
		st=st.replace(".MSE","-methodIrregularities.csv");
		return st;
	}

	static void beginMethodIrregularitiesComputation(Interpreter i,String fileName) throws Exception{
		FileWriter MethodIrrWriter = new FileWriter(outputIrregName(fileName));
		PrintWriter MethodIrrPrinter = new PrintWriter(MethodIrrWriter);
		MethodIrrPrinter.print(i.getMethodAntiPatterns());
		MethodIrrPrinter.close();
	}

	public static void main(String[] args) throws Exception{
		File FileName=new File(args[0]);
	
		BufferedReader BR=new BufferedReader(new FileReader(FileName));
		
		Interpreter i=new Interpreter();
		
		Reader r=new Reader(BR,i);

		i.setReader(r);

		r.getToNextLine();

		replaceSlashes(i.getFiles());

		replaceSame(i.getFiles());

		i.initialize();

		String st=i.toString();

		System.out.println("Printing file and class relations...");

		FileWriter classRelationsWriter=new FileWriter(outputRelationsName(args[0]));
		
		PrintWriter classRelationsPrinter=new PrintWriter(classRelationsWriter);
		
		classRelationsPrinter.print(st);
		
		classRelationsPrinter.close();

		System.out.println("Calculating metrics...");

		beginMetricsComputation(i,args[0]);

		System.out.println("Calculating external relations...");

		beginExternalsComputation(i,args[0]);

		System.out.println("Calculating inheritance relations...");

		beginClassInheritanceComputation(i,args[0]);

		System.out.println("Calculating method metrics...");

		beginMethodIrregularitiesComputation(i,args[0]);

		System.out.println("Done!");

		i.checkParameters();
	}

}
