package decaf.translate;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.*;  
import decaf.tree.Tree;
import decaf.tree.Tree.Default;
import decaf.tree.Tree.GuardedStmt;
import decaf.tree.Tree.IfSubStmt;
import decaf.backend.OffsetCounter;
import decaf.error.RuntimeError;
import decaf.machdesc.Intrinsic;
import decaf.symbol.Symbol;
import decaf.symbol.Variable;
import decaf.tac.Label;
import decaf.tac.Temp;
import decaf.type.BaseType;
import decaf.type.ClassType;
import decaf.type.ArrayType;

public class TransPass2 extends Tree.Visitor {

	private Translater tr;

	private Temp currentThis;

	private Stack<Label> loopExits;

	public TransPass2(Translater tr) {
		this.tr = tr;
		loopExits = new Stack<Label>();
	}

	@Override
	public void visitClassDef(Tree.ClassDef classDef) {
		for (Tree f : classDef.fields) {
			f.accept(this);
		}
	}

	@Override
	public void visitMethodDef(Tree.MethodDef funcDefn) {
		if (!funcDefn.statik) {
			currentThis = ((Variable) funcDefn.symbol.getAssociatedScope()
					.lookup("this")).getTemp();
		}
		tr.beginFunc(funcDefn.symbol);
		funcDefn.body.accept(this);
		tr.endFunc();
		currentThis = null;
	}

	@Override
	public void visitTopLevel(Tree.TopLevel program) {
		for (Tree.ClassDef cd : program.classes) {
			cd.accept(this);
		}
	}

	@Override
    public void visitScopy(Tree.Scopy scopy)
    {
		scopy.expr.accept(this);
		scopy.ident.accept(this);
		int n;
//		if(!(scopy.expr.isClass))
//			System.out.println("hello!!!!");
//		if(!scopy.expr.isClass) { // if it is not a class
//			n = 4;
//		}
//		else {
			n = ((ClassType)scopy.expr.type).getSymbol().getSize();
//		}
		Temp size = tr.genLoadImm4(n);
        tr.genParm(size);
        Temp result = tr.genIntrinsicCall(Intrinsic.ALLOCATE);
        int time = n / 4 - 1;
        for (int i = 0; i < time; i++)
        {
            Temp tmp = tr.genLoad(scopy.expr.val, (i+1)  * 4);
            tr.genStore(tmp, result, (i+1)  * 4);
        }
        tr.genStore(tr.genLoadVTable(((ClassType)scopy.expr.type).getSymbol().getVtable()), result, 0);
        scopy.ident.symbol.setTemp(result);
    }
	
	@Override
	 public void visitInitArray(Tree.InitArray initarray) {
    	initarray.left.accept(this);
    	initarray.right.accept(this);
    	// the difference between scopy and dcopy
    	if(!(initarray.left.type.isClassType())) {
	    	Temp result =tr.genNewInitArray(initarray.right.val,initarray.left.val);
	         initarray.val=result;
    	}
    	else {
//    		System.out.println("here!!!!!");
    		Temp result =tr.genNewInitClassArray(initarray.right.val,initarray.left);
    		initarray.val=result;
    	}
    }
	
	@Override
    public void visitGuardedStmt(Tree.GuardedStmt guardedstmt) {
		Label start = Label.createLabel();
        Label[] labels = new Label[guardedstmt.slist.size() + 1];
        for (int i = 0; i <= guardedstmt.slist.size(); i++)
        {
            labels[i] = Label.createLabel();
        }
        Label last = labels[guardedstmt.slist.size()];

        tr.genMark(start);
        int k = 0;
        for (Tree stmt : guardedstmt.slist)
        {
            tr.genMark(labels[k]);
            ((Tree.IfSubStmt)stmt).condition.accept(this);
            tr.genBeqz(((Tree.IfSubStmt) stmt).condition.val, labels[k + 1]);
            ((Tree.IfSubStmt)stmt).stmt.accept(this);
            tr.genBranch(labels[k+1]);
            k++;
        }
        tr.genMark(last);
        
    }
	
	@Override
	public void visitVarDef(Tree.VarDef varDef) {
		if (varDef.symbol.isLocalVar()) {
			Temp t = Temp.createTempI4();
			t.sym = varDef.symbol;
			varDef.symbol.setTemp(t);
		}
	}

	@Override
	public void visitDefault(Tree.Default mydefault) {
		mydefault.arrayname.accept(this);
		mydefault.left.accept(this);
		Label errl = Label.createLabel();
		
		mydefault.val = Temp.createTempI4();
		
		Temp length = tr.genLoad(mydefault.arrayname.val, -OffsetCounter.WORD_SIZE);
		Temp cond = tr.genLes(mydefault.left.val, length);
		tr.genBeqz(cond, errl);
		cond = tr.genLes(mydefault.left.val, tr.genLoadImm4(0));
		tr.genBnez(cond, errl);
		
		
		Temp esz = tr.genLoadImm4(OffsetCounter.WORD_SIZE);
		Temp t = tr.genMul(mydefault.left.val, esz);
		Temp base = tr.genAdd(mydefault.arrayname.val, t);
		Temp template = Temp.createTempI4();
		template = tr.genLoad(base, 0);
		tr.genAssign(mydefault.val,template);
//		mydefault.val = template;
		Label exit = Label.createLabel();
		tr.genBranch(exit);
		
		tr.genMark(errl);
		mydefault.right.accept(this);
		tr.genAssign(mydefault.val,mydefault.right.val);
		tr.genBranch(exit);
		
		tr.genMark(exit);
	}
	
	
	public void checkIfZero(Temp src)
    {
        Temp msg = tr.genLoadStrConst(RuntimeError.DIVIDED_BY_ZERO);
        Temp cond = tr.genEqu(src, tr.genLoadImm4(0));
        Label end = Label.createLabel();
        tr.genBeqz(cond, end);
        tr.genParm(msg);
        tr.genIntrinsicCall(Intrinsic.PRINT_STRING);
        tr.genIntrinsicCall(Intrinsic.HALT);
        tr.genMark(end);
    }
	
	@Override
	public void visitForeachStmt(Tree.ForeachStmt foreach) {
		Label exit = Label.createLabel();
		
		int n = 1;
		Temp base = Temp.createTempI4();
		base = tr.genLoadImm4(n);
		Temp template = Temp.createTempI4();// for loop
		template = tr.genLoadImm4(0);
		
//		foreach.lvalue.val = Temp.createTempI4();
		
		Label loopstart = Label.createLabel();
//		Label loopstart1 = Label.createLabel();
//		Label loopstart2= Label.createLabel();
		
		foreach.expr1.accept(this);  // the array
		foreach.lvalue.accept(this); // the normalstmt
		
		loopExits.push(exit);
		
		tr.genMark(loopstart);
//		tr.genMark(loopstart1);
//		tr.genMark(loopstart2);
		
		Temp length = tr.genLoad(foreach.expr1.val, -OffsetCounter.WORD_SIZE);
		Temp cond = tr.genLes(template, length);
		tr.genBeqz(cond, exit);
		
		Temp esz = tr.genLoadImm4(OffsetCounter.WORD_SIZE);
		Temp t = tr.genMul(template, esz);
		Temp base1 = tr.genAdd(foreach.expr1.val, t);
		((Tree.NormalStmt)foreach.lvalue).symbol.setTemp(tr.genLoad(base1, 0));
		
		
		if(foreach.expr2 != null) {
			foreach.expr2.accept(this);  // the bool
			tr.genBeqz(foreach.expr2.val, exit);//if not true, then break
		}
		
		Label mystmt = Label.createLabel();
		tr.genBranch(mystmt);
		 
		tr.genMark(mystmt);
		foreach.stmt.accept(this);
		
		tr.genAssign(template,tr.genAdd(template, base));
		
		tr.genBranch(loopstart);
		
		
		tr.genMark(exit);
		
	}
	
	@Override
	public void visitBinary(Tree.Binary expr) {
		expr.left.accept(this);
		expr.right.accept(this);
		switch (expr.tag) {
		case Tree.PLUS:
			expr.val = tr.genAdd(expr.left.val, expr.right.val);
			break;
		case Tree.MINUS:
			expr.val = tr.genSub(expr.left.val, expr.right.val);
			break;
		case Tree.MUL:
			expr.val = tr.genMul(expr.left.val, expr.right.val);
			break;
		case Tree.DIV:
			this.checkIfZero(expr.right.val);
			expr.val = tr.genDiv(expr.left.val, expr.right.val);
			break;
		case Tree.MOD:
			this.checkIfZero(expr.right.val);
			expr.val = tr.genMod(expr.left.val, expr.right.val);
			break;
		case Tree.AND:
			expr.val = tr.genLAnd(expr.left.val, expr.right.val);
			break;
		case Tree.OR:
			expr.val = tr.genLOr(expr.left.val, expr.right.val);
			break;
		case Tree.LT:
			expr.val = tr.genLes(expr.left.val, expr.right.val);
			break;
		case Tree.LE:
			expr.val = tr.genLeq(expr.left.val, expr.right.val);
			break;
		case Tree.GT:
			expr.val = tr.genGtr(expr.left.val, expr.right.val);
			break;
		case Tree.GE:
			expr.val = tr.genGeq(expr.left.val, expr.right.val);
			break;
		case Tree.EQ:
		case Tree.NE:
			genEquNeq(expr);
			break;
		}
	}

	private void genEquNeq(Tree.Binary expr) {
		if (expr.left.type.equal(BaseType.STRING)
				|| expr.right.type.equal(BaseType.STRING)) {
			tr.genParm(expr.left.val);
			tr.genParm(expr.right.val);
			expr.val = tr.genDirectCall(Intrinsic.STRING_EQUAL.label,
					BaseType.BOOL);
			if(expr.tag == Tree.NE){
				expr.val = tr.genLNot(expr.val);
			}
		} else {
			if(expr.tag == Tree.EQ)
				expr.val = tr.genEqu(expr.left.val, expr.right.val);
			else
				expr.val = tr.genNeq(expr.left.val, expr.right.val);
		}
	}

	@Override
	public void visitVarStmt(Tree.VarStmt varstmt) {
//		if (varstmt.symbol.isLocalVar()) {
//		System.out.println("var!!!");
			Temp t = Temp.createTempI4();
//			System.out.println(t.id);
			t.sym = varstmt.symbol;
//			varstmt.val = t;
			varstmt.symbol.setTemp(t);
//		}
	}
	
	@Override
	public void visitNormalStmt(Tree.NormalStmt normal) {
		Temp t = Temp.createTempI4();
//		System.out.println(t.id);
		t.sym = normal.symbol;
//		varstmt.val = t;
		normal.symbol.setTemp(t);
	}
	
	@Override
	public void visitAssign(Tree.Assign assign) {
		assign.left.accept(this);
		assign.expr.accept(this);
		switch (assign.left.lvKind) {
		case ARRAY_ELEMENT:
			Tree.Indexed arrayRef = (Tree.Indexed) assign.left;
			Temp esz = tr.genLoadImm4(OffsetCounter.WORD_SIZE);
			Temp t = tr.genMul(arrayRef.index.val, esz);
			Temp base = tr.genAdd(arrayRef.array.val, t);
			tr.genStore(assign.expr.val, base, 0);
			break;
		case MEMBER_VAR:
			Tree.Ident varRef = (Tree.Ident) assign.left;
			tr.genStore(assign.expr.val, varRef.owner.val, varRef.symbol
						.getOffset());
			break;
		case PARAM_VAR:
		case LOCAL_VAR:
			if(assign.left instanceof Tree.Ident) {
				tr.genAssign(((Tree.Ident) assign.left).symbol.getTemp(),
						assign.expr.val);
			}
			else {
				tr.genAssign(((Tree.VarStmt) assign.left).symbol.getTemp(),
						assign.expr.val);
			}
			break;
		}
	}

	@Override
	public void visitLiteral(Tree.Literal literal) {
		switch (literal.typeTag) {
		case Tree.INT:
			literal.val = tr.genLoadImm4(((Integer)literal.value).intValue());
			break;
		case Tree.BOOL:
			literal.val = tr.genLoadImm4((Boolean)(literal.value) ? 1 : 0);
			break;
		default:
			literal.val = tr.genLoadStrConst((String)literal.value);
		}
	}

	@Override
	public void visitExec(Tree.Exec exec) {
		exec.expr.accept(this);
	}

	@Override
	public void visitUnary(Tree.Unary expr) {
		expr.expr.accept(this);
		switch (expr.tag){
		case Tree.NEG:
			expr.val = tr.genNeg(expr.expr.val);
			break;
		default:
			expr.val = tr.genLNot(expr.expr.val);
		}
	}

	@Override
	public void visitNull(Tree.Null nullExpr) {
		nullExpr.val = tr.genLoadImm4(0);
	}

	@Override
	public void visitBlock(Tree.Block block) {
		for (Tree s : block.block) {
			s.accept(this);
		}
	}

	@Override
	public void visitThisExpr(Tree.ThisExpr thisExpr) {
		thisExpr.val = currentThis;
	}

	@Override
	public void visitReadIntExpr(Tree.ReadIntExpr readIntExpr) {
		readIntExpr.val = tr.genIntrinsicCall(Intrinsic.READ_INT);
	}

	@Override
	public void visitReadLineExpr(Tree.ReadLineExpr readStringExpr) {
		readStringExpr.val = tr.genIntrinsicCall(Intrinsic.READ_LINE);
	}

	@Override
	public void visitReturn(Tree.Return returnStmt) {
		if (returnStmt.expr != null) {
			returnStmt.expr.accept(this);
			tr.genReturn(returnStmt.expr.val);
		} else {
			tr.genReturn(null);
		}

	}

	@Override
	public void visitPrint(Tree.Print printStmt) {
		for (Tree.Expr r : printStmt.exprs) {
			r.accept(this);
//			System.out.println("heh1");
			tr.genParm(r.val);
//			System.out.println("heh2");
			if (r.type.equal(BaseType.BOOL)) {
				tr.genIntrinsicCall(Intrinsic.PRINT_BOOL);
			} else if (r.type.equal(BaseType.INT)) {
				tr.genIntrinsicCall(Intrinsic.PRINT_INT);
			} else if (r.type.equal(BaseType.STRING)) {
				tr.genIntrinsicCall(Intrinsic.PRINT_STRING);
			}
//			System.out.println("heh3");
		}
//		System.out.println("heh4");
	}

	@Override
	public void visitIndexed(Tree.Indexed indexed) {
		indexed.array.accept(this);
		indexed.index.accept(this);
		tr.genCheckArrayIndex(indexed.array.val, indexed.index.val);
		
		Temp esz = tr.genLoadImm4(OffsetCounter.WORD_SIZE);
		Temp t = tr.genMul(indexed.index.val, esz);
		Temp base = tr.genAdd(indexed.array.val, t);
		indexed.val = tr.genLoad(base, 0);
	}

	@Override
	public void visitIdent(Tree.Ident ident) {
		if(ident.lvKind == Tree.LValue.Kind.MEMBER_VAR){
			ident.owner.accept(this);
		}
//		ident.symbol.getTemp().name
		switch (ident.lvKind) {
		case MEMBER_VAR:
			ident.val = tr.genLoad(ident.owner.val, ident.symbol.getOffset());
			break;
		default:
			ident.val = ident.symbol.getTemp();
//			System.out.println(ident.symbol.getTemp().name);
			break;
		}
	}
	
	@Override
	public void visitBreak(Tree.Break breakStmt) {
		tr.genBranch(loopExits.peek());
	}

	@Override
	public void visitCallExpr(Tree.CallExpr callExpr) {
		if (callExpr.isArrayLength) {
			callExpr.receiver.accept(this);
			callExpr.val = tr.genLoad(callExpr.receiver.val,
					-OffsetCounter.WORD_SIZE);
		} else {
			if (callExpr.receiver != null) {
				callExpr.receiver.accept(this);
			}
			for (Tree.Expr expr : callExpr.actuals) {
				expr.accept(this);
			}
			if (callExpr.receiver != null) {
				tr.genParm(callExpr.receiver.val);
			}
			for (Tree.Expr expr : callExpr.actuals) {
				tr.genParm(expr.val);
			}
			if (callExpr.receiver == null) {
				callExpr.val = tr.genDirectCall(
						callExpr.symbol.getFuncty().label, callExpr.symbol
								.getReturnType());
			} else {
				Temp vt = tr.genLoad(callExpr.receiver.val, 0);
				Temp func = tr.genLoad(vt, callExpr.symbol.getOffset());
				callExpr.val = tr.genIndirectCall(func, callExpr.symbol
						.getReturnType());
			}
		}

	}

	@Override
	public void visitForLoop(Tree.ForLoop forLoop) {
		if (forLoop.init != null) {
			forLoop.init.accept(this);
		}
		Label cond = Label.createLabel();
		Label loop = Label.createLabel();
		tr.genBranch(cond);
		tr.genMark(loop);
		if (forLoop.update != null) {
			forLoop.update.accept(this);
		}
		tr.genMark(cond);
		forLoop.condition.accept(this);
		Label exit = Label.createLabel();
		tr.genBeqz(forLoop.condition.val, exit);
		loopExits.push(exit);
		if (forLoop.loopBody != null) {
			forLoop.loopBody.accept(this);
		}
		tr.genBranch(loop);
		loopExits.pop();
		tr.genMark(exit);
	}

	@Override
	public void visitIf(Tree.If ifStmt) {
		ifStmt.condition.accept(this);
		if (ifStmt.falseBranch != null) {
			Label falseLabel = Label.createLabel();
			tr.genBeqz(ifStmt.condition.val, falseLabel);
			ifStmt.trueBranch.accept(this);
			Label exit = Label.createLabel();
			tr.genBranch(exit);
			tr.genMark(falseLabel);
			ifStmt.falseBranch.accept(this);
			tr.genMark(exit);
		} else if (ifStmt.trueBranch != null) {
			Label exit = Label.createLabel();
			tr.genBeqz(ifStmt.condition.val, exit);
			if (ifStmt.trueBranch != null) {
				ifStmt.trueBranch.accept(this);
			}
			tr.genMark(exit);
		}
	}

	@Override
	public void visitNewArray(Tree.NewArray newArray) {
		newArray.length.accept(this);
//		System.out.println(newArray.length.val.value);
		newArray.val = tr.genNewArray(newArray.length.val);
	}

	@Override
	public void visitNewClass(Tree.NewClass newClass) {
		newClass.val = tr.genDirectCall(newClass.symbol.getNewFuncLabel(),
				BaseType.INT);
	}

	@Override
	public void visitWhileLoop(Tree.WhileLoop whileLoop) {
		Label loop = Label.createLabel();
		tr.genMark(loop);
		whileLoop.condition.accept(this);
		Label exit = Label.createLabel();
		tr.genBeqz(whileLoop.condition.val, exit);
		loopExits.push(exit);
		if (whileLoop.loopBody != null) {
			whileLoop.loopBody.accept(this);
		}
		tr.genBranch(loop);
		loopExits.pop();
		tr.genMark(exit);
	}

	@Override
	public void visitTypeTest(Tree.TypeTest typeTest) {
		typeTest.instance.accept(this);
		typeTest.val = tr.genInstanceof(typeTest.instance.val,
				typeTest.symbol);
	}

	@Override
	public void visitTypeCast(Tree.TypeCast typeCast) {
		typeCast.expr.accept(this);
		if (!typeCast.expr.type.compatible(typeCast.symbol.getType())) {
			tr.genClassCast(typeCast.expr.val, typeCast.symbol);
		}
		typeCast.val = typeCast.expr.val;
	}
}
