package jp.ac.osakau.farseerfc.purano.effect;

import jp.ac.osakau.farseerfc.purano.dep.DepSet;
import jp.ac.osakau.farseerfc.purano.reflect.MethodRep;
import jp.ac.osakau.farseerfc.purano.util.Escape;
import jp.ac.osakau.farseerfc.purano.util.Types;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(exclude="from")
public abstract class Effect<T extends Effect> implements Cloneable{
	private @Getter @Setter DepSet deps;
	private @Getter @Setter  MethodRep from;
	
	public Effect(DepSet deps, MethodRep from){
		this.deps = deps;
		this.from = from;
	}
	
	@Override
	public String toString(){
		return this.getClass().getName();
	}
	
	@NotNull
    @Override
	public abstract T clone();
	
	@NotNull
    public T duplicate(MethodRep from){
		T cl = clone();
		cl.setFrom(from);
		return cl;
	}
	
	public String dump(MethodRep rep, @NotNull Types table, String prefix){
		String className = getClass().getSimpleName();
		className = className.substring(0,className.length() - 6 );
		String fromStr="";
		if(from != null){
			fromStr = Escape.from(", inheritedFrom = \""+
                table.dumpMethodDesc(from.getInsnNode().desc,
                    String.format("%s#%s",
                        table.fullClassName(from.getInsnNode().owner),
                        from.getInsnNode().name))+"\"");
		}
		return String.format("%s@%s(%s%s)",
				prefix,
				Escape.annotation(className),
				Escape.effect(dumpEffect(rep, table)),
				fromStr);
	}
	

	protected abstract String dumpEffect(MethodRep rep, Types table);
}
