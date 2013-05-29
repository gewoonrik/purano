package jp.ac.osakau.farseerfc.purano.test;


import jp.ac.osakau.farseerfc.purano.ano.Argument;
import jp.ac.osakau.farseerfc.purano.ano.Field;
import org.jetbrains.annotations.NotNull;

public class TargetA extends TargetC implements TargetInterface{
	private int c;
	private int [] ma;
	
	public TargetA(int mc){
		this.c = mc;
	}
	
	public static int staticAdd(int a, int b){
		return a+b;
	}
	
	public int memberAdd(int a, int b){
		return a+b;
	}
	
	public int thisAdd(int a){
		return a+c;
	}
	
	public void setC(int ac){
		this.c = ac;
	}


	public void allMember(){
		setC(12);
	}

	@Override
	public void func(int a) {
		setC(a);
	}
	

	public void localArrayAccess(){
		int [] a = new int [12];
		a[0]=1;
	}
	
	public void mArrayNew(){
		ma = new int [c];
	}
	
	public void mArrayAccess(){
		ma[1] = c;
	}
	
	public void argArrayAccess(int a []){
		a[1]= c;
	}

    public void thisArrayModifyThroughArg(){
        argArrayAccess(ma);
    }

    public int [] exploitMember(){
        return ma;
    }

    public void thisArrayModifyThroughReturn(){
        argArrayAccess(exploitMember());
    }


    public int selfRecursion(int n){
		if(n>0){
			return selfRecursion(n-1)+1;
		}
		return 1;
	}
	
	public int crossRecursionA(int n){
		if(n>0){
			return crossRecursionB(n-1)+1;
		}
		return 1;
	}
	
	public int crossRecursionB(int n){
		if(n>0){
			return crossRecursionA(n-1)*2;
		}
		return 1;
	}
	
	public boolean equals(Object o){
		c = 1;
		return false;
	}
	
	@NotNull
    public static TargetA factory(int a){
		return new TargetA(a);
	}

    public static void modifyFactory(){
        factory(1).setC(12);
    }
}
