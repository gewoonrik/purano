package jp.ac.osakau.farseerfc.purano.dep;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import jp.ac.osakau.farseerfc.purano.reflect.MethodRep;
import jp.ac.osakau.farseerfc.purano.util.Types;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ClassDepVisitor extends ClassVisitor{
	private final Types typeNameTable = new Types();
    private @NotNull final PrintWriter out;
	private final StringWriter sw = new StringWriter();
	private final PrintWriter writer = new PrintWriter(sw);
	
	private String className ;
	
	public ClassDepVisitor() {
		super(Opcodes.ASM4);
		out = new PrintWriter(System.out);
	}

    private void printf(String format, Object ... args){
		writer.print(String.format(format, args));
	}
	
	@Override
	public void visit(int version, int access, @NotNull String name, @Nullable String signature,
			@NotNull String superName, @NotNull String[] interfaces) {
		className = name;
		List<String> ifs = Lists.transform(Arrays.asList(interfaces),
				new Function<String,String>(){
			@Override
			@javax.annotation.Nullable
			public String apply(@javax.annotation.Nullable String inter) {
                return inter == null ? null : typeNameTable.fullClassName(inter);
            }});

		printf("%s class %s extends %s %s%s {\n",
			Types.access2string(access),
			typeNameTable.fullClassName(name),
			typeNameTable.fullClassName(superName),
			(interfaces.length==0) ?
				"" :
				"implements "+Joiner.on(", ").join(ifs),
			signature==null?"":" /*" +signature+ "*/"
			);
	}

	
	@Nullable
    @Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		printf("    %s %s\n",desc,visible?"true":"false");
		return null;
	}

    @Nullable
    @Override
	public FieldVisitor	visitField(int access,
			String name, @NotNull String desc, @Nullable String signature, @Nullable Object value){
		printf("    %s %s %s%s%s;\n",
				Types.access2string(access),
			typeNameTable.desc2full(desc),
			name,
			signature==null?"":" /*" +signature+ "*/",
			value==null?"":" = "+value);
		return null;
	}
	
	@Nullable
    @Override
	public MethodVisitor visitMethod(int access, String name, @NotNull String desc,
			@Nullable String signature, @Nullable String[] exceptions) {
		if((access & Opcodes.ACC_SYNTHETIC )>0 ){
			// Avoid synthetic methods, which are generated by compiler 
			// and not visible from source codes
			return null;
		}
		
		printf("\n    %s %s%s\n{\n",
				Types.access2string(access),
				typeNameTable.dumpMethodDesc(desc, name),
				signature==null?"":" /*"+signature+"*/ ",
				(exceptions == null || exceptions.length == 0) ? "" : 
					"throws "+ Joiner.on(", ").join(exceptions));
		final MethodRep rep = new MethodRep(new MethodInsnNode(0, className, name, desc), access);
		return new TraceMethodVisitor(new MethodNode(Opcodes.ASM4,access,name,desc,signature,exceptions){
			@Override public void visitLocalVariable(
					String name, String desc, String signature, 
					@NotNull org.objectweb.asm.Label start, @NotNull org.objectweb.asm.Label end, int index) {
				super.visitLocalVariable(name, desc, signature, start, end, index);
				printf("LocalVariable %s\n",name);
				
			}


            @Override
			public void visitEnd() {
				super.visitEnd();
				printf("}\n{\n");
				rep.setMethodNode(this);
				DepEffect effect = new DepEffect();
				Analyzer<DepValue> ana = new Analyzer<>(new DepInterpreter(effect, rep));
				try {
					/*Frame<DepValue> [] frames =*/ ana.analyze("dep", this);
				} catch (AnalyzerException e) {
					e.printStackTrace();
				}
				printf("%s\n",effect.dump(rep,typeNameTable,"    "));
				printf("}\n");
			}
		},new Textifier(Opcodes.ASM4){
			@Override public void visitMethodEnd() {
				print(writer); 
			}
		});
	}

	@Override
	public void visitEnd(){
		printf("}\n");
		out.print(typeNameTable.dumpImports());
		out.print(sw.toString());
		out.flush();
	}


	public static void main(String[] args) throws IOException {
		ClassDepVisitor tt = new ClassDepVisitor();
		ClassReader cr = new ClassReader("javafx.util.Duration");//readAllBytes("target/TryTree.class"));
		//TraceClassVisitor tcv = new TraceClassVisitor(tt,new Textifier(), new PrintWriter(System.err));
		cr.accept(tt, 0);
	}
	
	public static byte[] readAllBytes(String filename) throws IOException{
		try(FileInputStream fis = new FileInputStream(filename)){
			int next;
			List<Byte> bytes=new ArrayList<>();
			while((next=fis.read())!=-1){
				bytes.add((byte)next);
			}
			return ArrayUtils.toPrimitive(bytes.toArray(new Byte[bytes.size()]));
		}
	}

	
}
