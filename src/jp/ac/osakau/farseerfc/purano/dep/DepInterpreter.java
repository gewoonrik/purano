package jp.ac.osakau.farseerfc.purano.dep;

import com.google.common.base.Joiner;

import jp.ac.osakau.farseerfc.purano.effect.*;
import jp.ac.osakau.farseerfc.purano.reflect.ClassFinder;
import jp.ac.osakau.farseerfc.purano.reflect.MethodRep;
import jp.ac.osakau.farseerfc.purano.util.Types;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Interpreter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Slf4j
public class DepInterpreter extends Interpreter<DepValue> implements Opcodes{
	
	private final IDepEffect effect= new IDepEffect(){

		@Override
		public void addThisField(FieldEffect tfe) {
			methodEffect.addThisField(tfe);
			currentFrameEffect.addThisField(tfe);
		}

		@Override
		public void addOtherField(OtherFieldEffect ofe) {
			methodEffect.addOtherField(ofe);
			currentFrameEffect.addOtherField(ofe);
		}

		@Override
		public void addOtherEffect(Effect oe) {
			methodEffect.addOtherEffect(oe);
			currentFrameEffect.addOtherEffect(oe);
		}
		
		@Override
		public void addCallEffect(CallEffect ce) {
			methodEffect.addCallEffect(ce);
			currentFrameEffect.addCallEffect(ce);
		}
		
		@Override
		public void addArgumentEffect(ArgumentEffect ae) {
			methodEffect.addArgumentEffect(ae);
			currentFrameEffect.addArgumentEffect(ae);
		}

		@Override
		public void addStaticField(StaticEffect sfe) {
			methodEffect.addStaticField(sfe);
			currentFrameEffect.addStaticField(sfe);
		}
	};
			
	private final DepEffect methodEffect;
	private @Getter @Setter DepEffect currentFrameEffect;
	private final MethodRep method;
	
	private static final boolean findCacheSemantic = true;
	private static final boolean findNative = false;
		
    @Nullable
    private final ClassFinder classFinder;


	public DepInterpreter(DepEffect effect, MethodRep method) {
		super(ASM4);
		this.methodEffect = effect;
		this.method = method;
		this.classFinder = null;
	}
	
	public DepInterpreter(DepEffect effect, MethodRep method, ClassFinder classFinder) {
		super(ASM4);
		this.methodEffect = effect;
		this.method = method;
		this.classFinder = classFinder;
	}
	
	
    
    private String opcode2string(int opcode){
    	List<String> result = new ArrayList<>();
		for(Field f: Opcodes.class.getFields()){
			if(!f.getName().startsWith("ACC_")){
				int v = 0;
				try {
					
					v = f.getInt(f);
				} catch ( @NotNull IllegalArgumentException | IllegalAccessException e) {
					//e.printStackTrace();
				}
				if(opcode == v){
					result.add(f.getName());
				}
			}
		}
		return Joiner.on(" ").join(result);
    }

    
    @Override
    public DepValue newValue( @Nullable final Type type) {
        if (type == null) {
            return new DepValue((Type) null);
        }
        switch (type.getSort()) {
        case Type.VOID:
            return null;
        case Type.BOOLEAN:
        case Type.CHAR:
        case Type.BYTE:
        case Type.SHORT:
        case Type.INT:
            return new DepValue(Type.INT_TYPE);
        case Type.FLOAT:
            return new DepValue(Type.FLOAT_TYPE);
        case Type.LONG:
            return new DepValue(Type.LONG_TYPE);
        case Type.DOUBLE:
            return new DepValue(Type.DOUBLE_TYPE);
        case Type.ARRAY:
        case Type.OBJECT:
            return new DepValue(Type.getObjectType("java/lang/Object"));
        default:
        	System.err.println("Unknown type :"+type);
            throw new Error("Internal error");
        }
    }

    public DepValue newValue( @Nullable final Type type, boolean constant) {
        if (type == null) {
            return new DepValue((Type) null, constant);
        }
        switch (type.getSort()) {
        case Type.VOID:
            return null;
        case Type.BOOLEAN:
        case Type.CHAR:
        case Type.BYTE:
        case Type.SHORT:
        case Type.INT:
            return new DepValue(Type.INT_TYPE, constant);
        case Type.FLOAT:
            return new DepValue(Type.FLOAT_TYPE, constant);
        case Type.LONG:
            return new DepValue(Type.LONG_TYPE, constant);
        case Type.DOUBLE:
            return new DepValue(Type.DOUBLE_TYPE, constant);
        case Type.ARRAY:
        case Type.OBJECT:
            return new DepValue(Type.getObjectType("java/lang/Object"), constant);
        default:
        	System.err.println("Unknown type :"+type);
            throw new Error("Internal error");
        }
    }

        
	
    @Override
	public DepValue newOperation( @NotNull final AbstractInsnNode insn)
			throws AnalyzerException {
		switch (insn.getOpcode()) {
		case ACONST_NULL:
			return newValue(Type.getObjectType("null"), true);
		case ICONST_M1:
		case ICONST_0:
		case ICONST_1:
		case ICONST_2:
		case ICONST_3:
		case ICONST_4:
		case ICONST_5:
			return new DepValue(Type.INT_TYPE, true);
		case LCONST_0:
		case LCONST_1:
			return new DepValue(Type.LONG_TYPE, true);
		case FCONST_0:
		case FCONST_1:
		case FCONST_2:
			return new DepValue(Type.FLOAT_TYPE, true);
		case DCONST_0:
		case DCONST_1:
			return new DepValue(Type.DOUBLE_TYPE, true);
		case BIPUSH:
		case SIPUSH:
			return new DepValue(Type.INT_TYPE, true);
		case LDC:
			Object cst = ((LdcInsnNode) insn).cst;
			if (cst instanceof Integer) {
				return new DepValue(Type.INT_TYPE, true);
			} else if (cst instanceof Float) {
				return new DepValue(Type.FLOAT_TYPE, true);
			} else if (cst instanceof Long) {
				return new DepValue(Type.LONG_TYPE, true);
			} else if (cst instanceof Double) {
				return new DepValue(Type.DOUBLE_TYPE, true);
			} else if (cst instanceof String) {
				return newValue(Type.getObjectType("java/lang/String"), true);
			} else if (cst instanceof Type) {
				int sort = ((Type) cst).getSort();
				if (sort == Type.OBJECT || sort == Type.ARRAY) {
					return newValue(Type.getObjectType("java/lang/Class"), true);
				} else if (sort == Type.METHOD) {
					return newValue(Type
							.getObjectType("java/lang/invoke/MethodType"), true);
				} else {
					throw new IllegalArgumentException("Illegal LDC constant "
							+ cst);
				}
			} else if (cst instanceof Handle) {
				return newValue(Type
						.getObjectType("java/lang/invoke/MethodHandle"), true);
			} else {
				throw new IllegalArgumentException("Illegal LDC constant "
						+ cst);
			}
		case JSR:
            return null;
//			return new DepValue(Type.VOID_TYPE);
		case GETSTATIC:
			{ 
				FieldInsnNode fin = (FieldInsnNode) insn;
				DepValue v = newValue(Type.getType(fin.desc));
                if (v == null) {
                    throw new AssertionError("Cannot get static member from type void");
                }
                v.getDeps().getStatics().add(new FieldDep(fin.desc,fin.owner,fin.name));
				v.getLvalue().getStatics().add(new FieldDep(fin.desc, fin.owner, fin.name));
				return v;
			}
		case NEW:
			return newValue(Type.getObjectType(((TypeInsnNode) insn).desc));
			
		default:
        	System.err.println("Unknow copyOperation "+opcode2string(insn.getOpcode()));
			throw new Error("Internal error.");
        	//return null;
		}
	}

    
    @NotNull
    @Override
    public DepValue copyOperation( @NotNull final AbstractInsnNode insn,
             @NotNull final DepValue value) throws AnalyzerException {
        DepSet deps = new DepSet(value.getDeps());
    	
        switch (insn.getOpcode()){
        case ILOAD:
        	deps.getLocals().add(((VarInsnNode) insn).var);
        	return new DepValue(Type.INT_TYPE,deps);
        case LLOAD:
        	deps.getLocals().add(((VarInsnNode) insn).var);
        	return new DepValue(Type.LONG_TYPE,deps);
        case FLOAD:
        	deps.getLocals().add(((VarInsnNode) insn).var);
        	return new DepValue(Type.FLOAT_TYPE,deps);
        case DLOAD:
        	deps.getLocals().add(((VarInsnNode) insn).var);
        	return new DepValue(Type.DOUBLE_TYPE,deps);
        case ALOAD:{
            int local = ((VarInsnNode) insn).var;
        	deps.getLocals().add(local);
            DepValue dv = new DepValue(Type.getObjectType("java/lang/Object"), deps, value.getLvalue());
//            if(method.isArg(local)){
                dv.getLvalue().getLocals().add(local);
//            }
//            if(!method.isStatic() && local == 0){
//                dv.getLvalue().getLocals().add(0); // this pointer
//            }
        	return dv;
        }
        case DUP:
        case DUP_X1:
        case DUP_X2:
        case DUP2:
        case DUP2_X1:
        case DUP2_X2:
        case SWAP:
        	return new DepValue(value);
        case ASTORE:
        case ISTORE:
        case LSTORE:
        case FSTORE:
        case DSTORE:
        	int local = ((VarInsnNode) insn).var;
            currentFrameEffect.addLocalVariableEffect(new LocalVariableEffect(local, deps, null));
        	return new DepValue(value);
        default:
        	System.err.println("Unknown copyOperation "+opcode2string(insn.getOpcode()));
			throw new Error("Internal error.");
        	//return null;
        }
    }

    
    @Override
    public DepValue unaryOperation( @NotNull final AbstractInsnNode insn,
             @NotNull final DepValue value) throws AnalyzerException {
        switch (insn.getOpcode()) {
        case INEG:
        case IINC:
        case L2I:
        case F2I:
        case D2I:
        case I2B:
        case I2C:
        case I2S:
            return new DepValue(Type.INT_TYPE,value.getDeps(), value.isConstant());
        case FNEG:
        case I2F:
        case L2F:
        case D2F:
            return new DepValue(Type.FLOAT_TYPE,value.getDeps(), value.isConstant());
        case LNEG:
        case I2L:
        case F2L:
        case D2L:
            return new DepValue(Type.LONG_TYPE,value.getDeps(), value.isConstant());
        case DNEG:
        case I2D:
        case L2D:
        case F2D:
            return new DepValue(Type.DOUBLE_TYPE,value.getDeps(), value.isConstant());
        case IFEQ:
        case IFNE:
        	if(!method.getDesc().getReturnType().equals("void")){
	        	for(FieldDep fd: value.getDeps().getFields()){
	        		if(findCacheSemantic && method.checkCacheSematic(fd)) 
	        			{}
//	        			continue;
	        		methodEffect.getReturnDep().getDeps().getFields().add(fd);
	        	}
	        	methodEffect.getReturnDep().getDeps().getLocals().addAll(value.getDeps().getLocals());
	        	methodEffect.getReturnDep().getDeps().getStatics().addAll(value.getDeps().getStatics());
        	}
        	return null;
        case IFLT:
        case IFGE:
        case IFGT:
        case IFLE:
        case TABLESWITCH:
        case LOOKUPSWITCH:
            if(!method.getDesc().getReturnType().equals("void")){
            	methodEffect.getReturnDep().getDeps().merge(value.getDeps());
            }
        	return null;
        case IRETURN:
        case LRETURN:
        case FRETURN:
        case DRETURN:
        	methodEffect.getReturnDep().getDeps().merge(value.getDeps());
        	return null;
        case ARETURN:{
        	methodEffect.getReturnDep().getDeps().merge(value.getDeps());
        	methodEffect.getReturnDep().getLvalue().merge(value.getLvalue());
        	return null;
        }
		case PUTSTATIC: {
            // owner.name = value
			FieldInsnNode fin = (FieldInsnNode) insn;
//			effect.addStaticField(
//					new StaticEffect(fin.desc, fin.owner, fin.name,
//							value.getDeps(),null));
            DepValue dv = new DepValue(value);
            dv.getLvalue().getStatics().add(new FieldDep(fin.desc,fin.owner,fin.name));
            dv.modify(effect, currentFrameEffect, method, null);
			return null;
		}
        case GETFIELD:{
        	FieldInsnNode fin = (FieldInsnNode) insn;
        	DepValue v =new DepValue(Type.getType(fin.desc), value.getDeps());
        	v.getDeps().getFields().add(new FieldDep(fin.desc,fin.owner,fin.name));

            if(value.getLvalue().isThis()){
                if(!method.isStatic() &&value.getLvalue().isThis()){
                    // when we try to get this.f
                    v.getLvalue().getFields().add(new FieldDep(fin.desc,fin.owner,fin.name));
                }else{
                    // when we try to get this.f.g
                    v.getLvalue().getLocals().add(0); // this
                    v.getLvalue().getFields().addAll(value.getLvalue().getFields()); // this.f
                }
            } // else case is local.f , which we do not need to trace

            // when we try to get static.f
            v.getLvalue().getStatics().addAll(value.getLvalue().getStatics()); // add static as root state
            // when we try to get arg.f
            v.getLvalue().getLocals().addAll(value.getLvalue().getLocals());   // add arg as root state
        	return v;
        }
        case NEWARRAY:
            switch (((IntInsnNode) insn).operand) {
            case T_BOOLEAN:
                return new DepValue(Type.getType("[Z"), value.getDeps());
            case T_CHAR:
                return new DepValue(Type.getType("[C"), value.getDeps());
            case T_BYTE:
                return new DepValue(Type.getType("[B"), value.getDeps());
            case T_SHORT:
                return new DepValue(Type.getType("[S"), value.getDeps());
            case T_INT:
                return new DepValue(Type.getType("[I"), value.getDeps());
            case T_FLOAT:
                return new DepValue(Type.getType("[F"), value.getDeps());
            case T_DOUBLE:
                return new DepValue(Type.getType("[D"), value.getDeps());
            case T_LONG:
                return new DepValue(Type.getType("[J"), value.getDeps());
            default:
                throw new AnalyzerException(insn, "Invalid array type");
            }
        case ANEWARRAY:{
            String desc = ((TypeInsnNode) insn).desc;
            return new DepValue(Type.getType("[" + Type.getObjectType(desc)),new DepSet());//value.getDeps());
        }
        case ARRAYLENGTH:
            return new DepValue(Type.INT_TYPE, value.getDeps());
        
        case ATHROW:{
//        	effect.getOtherEffects().add(new ThrowEffect(value.getDeps(), null));
            return null;
        }
        case CHECKCAST:{
            String desc = ((TypeInsnNode) insn).desc;
            DepValue dv = newValue(Type.getObjectType(desc));
            dv.getDeps().merge(value.getDeps());
            dv.getLvalue().merge(value.getLvalue());
            return dv;
        }
        case INSTANCEOF:
            return new DepValue(Type.INT_TYPE);
        case MONITORENTER:
        case MONITOREXIT:
        	return null;
        case IFNULL:
        case IFNONNULL:
        	if(findCacheSemantic){
        		Set<FieldDep> fds = new HashSet<>(value.getDeps().getFields());
        		for(FieldDep fd: fds){
        			if(method.checkCacheSematic(fd)){
//        				value.getDeps().getFields().remove(fd);
        			}
        		}
        	}
        	return null;
        default:
        	System.err.println("Unknown copyOperation "+opcode2string(insn.getOpcode()));
			throw new Error("Internal error.");
        	//return null;
        }
    }

    
    @Override
    public DepValue binaryOperation( @NotNull final AbstractInsnNode insn,
             @NotNull final DepValue value1,  @NotNull final DepValue value2)
            throws AnalyzerException {
    	DepSet deps = new DepSet(value1.getDeps());
    	deps.merge(value2.getDeps());
    	boolean constant = value1.isConstant() && value2.isConstant();
        switch (insn.getOpcode()) {
        case LCMP:
        case FCMPL:
        case FCMPG:
        case DCMPL:
        case DCMPG:
        case IALOAD:
        case BALOAD:
        case CALOAD:
        case SALOAD:
        case IADD:
        case ISUB:
        case IMUL:
        case IDIV:
        case IREM:
        case ISHL:
        case ISHR:
        case IUSHR:
        case IAND:
        case IOR:
        case IXOR:
        	return new DepValue(Type.INT_TYPE, deps, constant);
        case FALOAD:
        case FADD:
        case FSUB:
        case FMUL:
        case FDIV:
        case FREM:
        	return new DepValue(Type.FLOAT_TYPE, deps, constant);
        case LALOAD:
        case LADD:
        case LSUB:
        case LMUL:
        case LDIV:
        case LREM:
        case LSHL:
        case LSHR:
        case LUSHR:
        case LAND:
        case LOR:
        case LXOR:
        	return new DepValue(Type.LONG_TYPE, deps, constant);
        case DALOAD:
        case DADD:
        case DSUB:
        case DMUL:
        case DDIV:
        case DREM:
        	return new DepValue(Type.DOUBLE_TYPE, deps, constant);
        case AALOAD:
//        	if(value1.getType().getInternalName().startsWith("[")){
//        		return new DepValue(Type.getObjectType(value1.getType().getInternalName().substring(1)), deps);
//        	}else{
//        		throw new RuntimeException("AALOAD encounter non-array value! "+value1.getType().getInternalName());
//        	}
        	return new DepValue(Type.getObjectType("java/lang/Object;"),deps,value1.getLvalue());
        case IF_ICMPEQ:
        case IF_ACMPEQ:
        case IF_ICMPNE:
        case IF_ACMPNE:
        	if(!method.getDesc().getReturnType().equals("void")){
	        	if(value2.isConstant() || value1.isConstant()){
	        		if(value1.isConstant()){
	        			for(FieldDep fd: value2.getDeps().getFields()){
	    	        		if(findCacheSemantic && method.checkCacheSematic(fd)) {
//	    	        			deps.getFields().remove(fd);
	    	        		}
	    	        	}
	        		}
	        		if(value2.isConstant()){
	        			for(FieldDep fd: value1.getDeps().getFields()){
	    	        		if(findCacheSemantic && method.checkCacheSematic(fd)) {
//	    	        			deps.getFields().remove(fd);
	    	        		}
	    	        	}
	        		}
	        	}
	        	methodEffect.getReturnDep().getDeps().merge(deps);
        	}
        	return null;
        case IF_ICMPLT:
        case IF_ICMPGE:
        case IF_ICMPGT:
        case IF_ICMPLE:
            if(!method.getDesc().getReturnType().equals("void")){
            	methodEffect.getReturnDep().getDeps().merge(value1.getDeps());
            	methodEffect.getReturnDep().getDeps().merge(value2.getDeps());
            }
        	return null;
        case PUTFIELD:{
            // v1.name = v2
            FieldInsnNode fin = (FieldInsnNode) insn;
            if (!method.isStatic() && value1.getLvalue().isThis()) {
                // this.name == v2
            	if(findCacheSemantic){
            		if(value2.isConstant()){
            			return null;
            		}
	            	FieldDep fd = new FieldDep(fin.desc, fin.owner, fin.name);
	            	if(method.addCacheSemantic(fd)){
	            		return null;
	            	}
            	}
                DepValue dv = new DepValue(value2.getType(), value2.getDeps());
                dv.getLvalue().merge(value1.getLvalue());
                dv.getLvalue().getFields().add(new FieldDep(fin.desc, fin.owner, fin.name));
                dv.modify(effect, currentFrameEffect, method, null);
            } else {
                // sth.name == v2
                // sth maybe arg or this.otherField or staticField
                value1.setDeps(new DepSet(value2.getDeps()));
                value1.modify(effect, currentFrameEffect, method, null);
            }
        	return null;
        }
        default:
        	System.err.println("Unknown binaryOperation "+opcode2string(insn.getOpcode()));
			throw new Error("Internal error.");
        	//return null;
        }
    }

	
    @Override
	public DepValue ternaryOperation( @NotNull final AbstractInsnNode insn,
			 @NotNull final DepValue arrayref,  @NotNull final DepValue index,  @NotNull final DepValue value)
			throws AnalyzerException {
		// DepSet deps = new DepSet(value1.getDeps());
		// deps.merge(value2.getDeps());
		// deps.merge(value3.getDeps());
		switch (insn.getOpcode()) {
		case BASTORE:
		case CASTORE:
		case SASTORE:
		case IASTORE:
		case LASTORE:
		case FASTORE:
		case DASTORE:
		case AASTORE: {
            DepSet depset = new DepSet();
            depset.merge(index.getDeps());
            depset.merge(value.getDeps());
            arrayref.setDeps(depset);
            arrayref.modify(effect, currentFrameEffect,method,null);
			return null;
		}
		default:
			System.err.println("Unknown ternaryOperation "
					+ opcode2string(insn.getOpcode()));
			throw new Error("Internal error.");
			// return null;
		}
	}


	
    @NotNull
    private DepValue addCallEffect(DepSet deps, String callType,
			 @NotNull MethodInsnNode min) {
		CallEffect ce=new CallEffect(callType,min.desc,min.owner,min.name, deps, null);
		effect.addCallEffect(ce);
		return new DepValue(Type.getReturnType(min.desc), deps);
	}
	
    
    @NotNull
    @Override
    public DepValue naryOperation( @NotNull final AbstractInsnNode insn,
             @NotNull final List<? extends DepValue> values) throws AnalyzerException {
    	DepSet deps = new DepSet();
    	for(DepValue value :values){
    		deps.merge(value.getDeps());
    	}
    	String callType;
    	switch(insn.getOpcode()){
    	case MULTIANEWARRAY:
    		return new DepValue(Type.getType(((MultiANewArrayInsnNode) insn).desc), deps);
    	case INVOKEDYNAMIC:
    		effect.addOtherEffect(new InvokeDynamicEffect(null));
    		return new DepValue(Type.getReturnType(((InvokeDynamicInsnNode) insn).desc), deps);
    	case INVOKEVIRTUAL:
    		callType="VIRTUAL";
    		break;
    	case INVOKEINTERFACE:
    		callType="INTERFACE";
    		break;
    	case INVOKESPECIAL:
    		callType="SPECIAL";
    		break;
    	case INVOKESTATIC:{
    		callType="STATIC";
    		break;
    	}
		default:
			System.err.println("Unknow copyOperation "+opcode2string(insn.getOpcode()));
			throw new Error("Internal error.");
        	//return null;
    	}
    	MethodInsnNode min = (MethodInsnNode) insn;
    	if(classFinder == null ){
			return addCallEffect(deps, callType, min);
    	}else{
    		MethodRep rep =  classFinder.loadClass(Types.binaryName2NormalName(min.owner))
    				.getMethodVirtual(MethodRep.getId(min));
    		
//    		log.info("Analyzing Calling {} in {}",new MethodRep(min, 0),method);
    		if(rep == null){
    			return addCallEffect(deps, callType, min);
    		}


            if(!rep.getCalled().contains(method)){
                rep.getCalled().add(method);
            }

    		
    		DepEffect callEffect = null;

            if(insn.getOpcode() == INVOKESPECIAL ||
                    insn.getOpcode() == INVOKESTATIC){
                callEffect = rep.getStaticEffects();
            }else{
                callEffect = rep.getDynamicEffects();
            }


    		if(callEffect == null){
    			return addCallEffect(deps, callType, min);
    		}

    		return transitive(values, rep, callEffect, deps);
    	}
    }

    @NotNull
    private DepValue transitive(@NotNull List<? extends DepValue> values, @NotNull MethodRep rep, @NotNull DepEffect callEffect, DepSet deps) {
        DepValue result = new DepValue(Type.getType(rep.getInsnNode().desc).getReturnType());
        result.getDeps().merge(deps);

        if (rep.isNative() && findNative) {
            effect.addOtherEffect(new NativeEffect(rep));
            return result;
        }

        // transitive from origin

//        for(StaticEffect sfe:callEffect.getStaticField().values()){
//            effect.addStaticField(sfe.duplicate(rep));
//        }
        if(callEffect.getStaticField().size()>0){
            effect.addStaticField(new StaticEffect("","","",new DepSet(), rep));
        }

        for(Effect e :callEffect.getOtherEffects()){
            effect.addOtherEffect(e.duplicate(rep));
        }

        if(callEffect.getOtherField().size()>0){
            throw new RuntimeException("Found otherFieldEffect in :" + rep.toString(new Types(false)));
        }

        if (rep.isStatic() || values.size() == 0) {
            if ( callEffect.getThisField().size() > 0) {
                throw new RuntimeException("Static method invocation generates this effect :" + rep.toString(new Types(false)));
            }
            for (OtherFieldEffect ofe : callEffect.getOtherField().values()) {
                effect.addOtherField(ofe.duplicate(rep));
            }
            return result;
        }

        DepValue obj = values.get(0);
        for (ArgumentEffect ae : callEffect.getArgumentEffects()) {
            // ae.getArgPos  is method call is changing value of argument in position
//            log.info("ArgumentEffect {} values [{}] rep {} method "+method.toString(),
//                    ae.getArgPos(),Joiner.on(",").join(values),rep);
//            log.info("Rep dump {}",rep);

            DepValue dv = values.get(rep.localToArgumentPos(ae.getArgPos()));
            dv.modify(effect, currentFrameEffect,method,rep);

        }

        if(callEffect.getThisField().size()>0){
            if(!method.isStatic() && obj.getLvalue().isThis()){
                DepValue dv = new DepValue(obj);
                for(FieldEffect tfe: callEffect.getThisField().values()){
                    dv.getLvalue().getFields().add(new FieldDep(tfe.getDesc(),tfe.getOwner(),tfe.getName()));
                }
                dv.modify(effect, currentFrameEffect,method,rep);
            }else if(!method.isStatic()){
                obj.modify(effect, currentFrameEffect,method,rep);
            }
        }

        DepValue ret = callEffect.getReturnDep();

        for(int arg:ret.getLvalue().getLocals()){
            if(!rep.isStatic() && arg == 0) continue;
            if(rep.isArg(arg)){
////                boolean re = rep.isArg(arg);
//                log.info("Getting local beyond argument {} return lvalue \nmethod {} \nrep {} ", arg, method,rep);
//                log.info("Rep dump {}",rep);
//                log.info("ReturnLvalue dump {}", ret.getLvalue());
//                throw new RuntimeException("Getting local beyond argument return lvalue ");
                result.getLvalue().merge(values.get(rep.localToArgumentPos(arg)).getLvalue());
                result.getDeps().merge(values.get(rep.localToArgumentPos(arg)).getDeps());
            }
        }

        if(!method.isStatic() ){
            if(obj.getLvalue().isThis()){
                for(FieldDep fd:ret.getLvalue().getFields()){
                    result.getLvalue().getFields().add(fd);
                }
                for(FieldDep fd:ret.getDeps().getFields()){
                    result.getDeps().getFields().add(fd);
                }
            }
            result.getDeps().merge(obj.getDeps());
        }

        for(FieldDep fd:ret.getLvalue().getStatics()){
            result.getLvalue().getStatics().add(fd);
        }


        return result;
    }

    @Override
    public void returnOperation(final AbstractInsnNode insn,
            final DepValue value, final DepValue expected)
            throws AnalyzerException {
    }

    
    @Nullable
    @Override
    public DepValue merge( @NotNull final DepValue v,  @NotNull final DepValue w) {
        DepSet deps = new DepSet();
        deps.merge(v.getDeps());
        deps.merge(w.getDeps());

        DepSet lv = new DepSet();
        lv.merge(v.getLvalue());
        lv.merge(w.getLvalue());

        if (!v.equals(w)) {
            if (v.getType() == null) {
                return new DepValue(w.getType(), deps, lv);
            } else if (w.getType() == null) {
                return new DepValue(v.getType(), deps, lv);
            } else {
                return new DepValue(Types.covariant(v.getType(), w.getType()), deps, lv);
            }
        }
        return new DepValue(v.getType(), deps, lv);
    }
}
