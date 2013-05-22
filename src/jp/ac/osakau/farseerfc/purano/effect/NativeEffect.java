package jp.ac.osakau.farseerfc.purano.effect;

import jp.ac.osakau.farseerfc.purano.dep.DepSet;
import jp.ac.osakau.farseerfc.purano.reflect.MethodRep;
import jp.ac.osakau.farseerfc.purano.util.Types;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

public class NativeEffect extends Effect implements Cloneable{

	public NativeEffect(MethodRep from) {
		super(new DepSet(), from);
	}
	
	@NotNull
    @Override
	public Effect clone() {
		return new NativeEffect(getFrom());
	}

	@NotNull
    @Override
	protected String dumpEffect(MethodRep rep, Types table) {
		return "";
	}
}
