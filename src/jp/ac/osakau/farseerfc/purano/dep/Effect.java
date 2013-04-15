package jp.ac.osakau.farseerfc.purano.dep;

import lombok.Data;

public abstract @Data class Effect {
	private final DepSet deps;
	
	@Override
	public String toString(){
		return this.getClass().getName();
	}
}
