import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.lang.reflect.ParameterizedType;
import java.util.*;
class Interpreter {
	private ArrayList<FamixClass> Classes=new ArrayList<FamixClass> ();
	private ArrayList<Access> Accesses=new ArrayList<Access>();
	private ArrayList<Method> Methods=new ArrayList<Method> ();
	private ArrayList<Attribute> Attributes=new ArrayList<Attribute> ();
	private ArrayList<Invocation> Invocations=new ArrayList<Invocation> ();
	private ArrayList<Inheritance> Inherits=new ArrayList<Inheritance> ();
	private ArrayList<ContainingFile> Files=new ArrayList<ContainingFile> ();
	private ArrayList<Parameter> Parameters=new ArrayList<Parameter> ();
	private ArrayList<FamixNamespace> Namespaces=new ArrayList<FamixNamespace>();
	private ArrayList<LocalVariable> LocalVariables=new ArrayList<LocalVariable>();
	private ArrayList<ParametrizedType> ParamTypes=new ArrayList<ParametrizedType>();
	private Map<Attribute,Attribute> AttributeConnections=new HashMap<Attribute,Attribute>();
	private Reader r;
	private long maxID=0;

	public void setReader(Reader r) {
		this.r=r;
	}
	
	public void checkForType(String st) throws Exception{
		if(st==null) return;
		if(st.contains("(FAMIX.Class")||st.contains("FAMIX.PrimitiveType")||st.contains("FAMIX.Type")||st.contains("FAMIX.ParameterizableClass")) {
			interpretClass(st);
			return;
		}
		if(st.contains("(FAMIX.IndexedFileAnchor")) {
			interpretFile(st);
			return;
		}
		if(st.contains("(FAMIX.Method")) {
			interpretMethod(st);
			return;
		}
		if(st.contains("(FAMIX.Invocation")) {
			interpretInvocation(st);
			return;
		}
		if(st.contains("(FAMIX.Inheritance")) {
			interpretInheritance(st);
			return;
		}
		if(st.contains("(FAMIX.Namespace ")){
		    interpretNamespace(st);
		    return;
        }
		if(st.contains("(FAMIX.Attribute")||st.contains("(FAMIX.LocalVariable ") || st.contains("FAMIX.Parameter ")) {
			interpretAttribute(st);
			return;
		}
		if(st.contains("(FAMIX.Access ")){
			interpretAccess(st);
			return;
		}
		if(st.contains("(FAMIX.ParameterizedType ")){
			interpretParameterizedType(st);
			return;
		}
	}

	private void interpretAccess(String st1) throws Exception{
		maxID=getID(st1.toCharArray());
		String st=r.getNextLine();
		long variableID=0;
		long accessorID=0;
		while(st!=null && !st.contains("(FAMIX.")){
			if(st.contains("variable ")) variableID=getID(st.toCharArray());
			if(st.contains("(accessor ")) accessorID=getID(st.toCharArray());
			st=r.getNextLine();
		}
		Access a=new Access(variableID,accessorID);
		Accesses.add(a);
		checkForType(st);
	}

	private void interpretParameterizedType(String st) throws Exception{
		maxID=getID(st.toCharArray());
		ArrayList<Long> l=new ArrayList<Long>();
		long parameterizableClassID=0;
		st=r.getNextLine();
		while(st!=null && !st.contains("(FAMIX.")){
			if(st.contains("arguments"))  l=getArguments(st.toCharArray());
			if(st.contains("parameterizableClass")) parameterizableClassID=getID(st.toCharArray());
			st=r.getNextLine();
		}
		ParametrizedType p=new ParametrizedType();
		p.arguments=l;
		p.parameterizableClassID=parameterizableClassID;
		p.ID=maxID;
		ParamTypes.add(p);
		checkForType(st);
	}

	private ArrayList<Long> getArguments(char[] chArray){
		ArrayList<Long> l=new ArrayList<Long>();
		for(int i=0;i<chArray.length;i++){
			ArrayList<Character> Digits=new ArrayList<Character>();
			if(chArray[i]=='(' && chArray[i+1]=='r'){
				while(chArray[i]>='0' && chArray[i]<='9'){
					Digits.add(chArray[i]);
					i++;
				}
				long nr=digitsListToLong(Digits);
				l.add(nr);
			}
			if(chArray[i]==')' && chArray[i+1]==')') return l;
		}
		return l;
	}

	private void interpretLocalVariable(String st) throws Exception{
		long ID=getID(st.toCharArray());
		maxID=ID;
		long containerID=0,declaredID=0;
		st=r.getNextLine();
		while(st!=null && !st.contains("(FAMIX.")){
			if(st.contains("(parentBehaviouralEntity ")) containerID=getID(st.toCharArray());
			if(st.contains("(declaredType ")) declaredID=getID(st.toCharArray());
			st=r.getNextLine();
		}
		LocalVariable lv=new LocalVariable(ID,containerID,declaredID);
		LocalVariables.add(lv);
		checkForType(st);
	}

	private void interpretNamespace(String st) throws Exception{
        long id=getID(st.toCharArray());
        maxID=id;
        String name="";
        boolean isStub=false;
        long parentID=0;
        st=r.getNextLine();
        while(st!=null && !st.contains("FAMIX")){
            if(st.contains("(name '")) name=getName(st.toCharArray());
            if(st.contains("(isStub ")) isStub=true;
            if(st.contains("(parentScope ")) parentID=getID(st.toCharArray());
            st=r.getNextLine();
        }
        FamixNamespace n=new FamixNamespace();
        n.ID=id;
        n.name=name;
        n.isStub=isStub;
        n.parentID=parentID;
        Namespaces.add(n);
        checkForType(st);
    }

	private void interpretParameter(String st)throws Exception{
		long ID=getID(st.toCharArray());
		maxID=ID;
		st=r.getNextLine();
		long declaredID=0,parentID=0;
		while(st!=null && !st.contains("FAMIX")){
			if(st.contains("(declaredType ")) declaredID=getID(st.toCharArray());
			if(st.contains("(parentBehaviouralEntity")) parentID=getID(st.toCharArray());
			st=r.getNextLine();
		}
		Parameter p=new Parameter(ID,declaredID,parentID);
		Parameters.add(p);
		checkForType(st);
	}

	private void interpretMethod(String st) throws Exception{
		Long MethodID=getID(st.toCharArray());
		maxID=MethodID;
		st=r.getNextLine();
		Long parentType=0L;
		String signature="";
		String modifiers="";
		String kind="";
		Long declaredType=0L;
		long begin = 0;
		long end = 0;
		boolean isStatic = false;
		boolean isStub=false;
		int cyclomaticComplexity=1;
		while(st!=null && !st.contains("FAMIX.")) {
			if(st.contains("kind")) kind=getName(st.toCharArray());
			if(st.contains("modifiers ")) modifiers=getModifiers(st.toCharArray());
			if(st.contains("parentType")) parentType=getID(st.toCharArray());
			if(st.contains("signature"))signature=getName(st.toCharArray());
			if(st.contains("cyclomaticComplexity"))cyclomaticComplexity=(int)getID(st.toCharArray());
			if(st.contains("isStub")) isStub=true;
			if(st.contains("declaredType")) declaredType=getID(st.toCharArray());
			if(st.contains("(hasClassScope")) isStatic = true;
			if(st.contains("(astStartPosition")) begin = getID(st.toCharArray());
			if(st.contains("(astStopPosition")) end = getID(st.toCharArray());
			st=r.getNextLine();
		}
		Method m=new Method(MethodID,parentType,signature,modifiers,kind,cyclomaticComplexity,isStub,declaredType,isStatic,end - begin + 1);
		Methods.add(m);
		checkForType(st);
	}
	
	private String getModifiers(char[] chArray) {
		String st="";
		int i;
		for(i=0;i<chArray.length;i++) {
			if(chArray[i]=='\'') {
				i++;
				do {
					st=st+chArray[i];
					i++;
				}while(chArray[i]!='\'');
				st=st+" ";
			}
		}
		return st;
	}

	public ArrayList<FamixClass> getClasses(){
	    return Classes;
    }

	private void interpretInvocation(String st1) throws Exception{
		maxID=getID(st1.toCharArray());
		String st=r.getNextLine();
		long candidateID=0;
		long receiverID=0;
		long senderID=0;
		while(st!=null && !st.contains("FAMIX.")) {
			if(st.contains("(candidates (ref:")) candidateID=getID(st.toCharArray());
			if(st.contains("(sender (ref:")) senderID=getID(st.toCharArray());
			if(st.contains("(receiver (ref:")) receiverID=getID(st.toCharArray());
			st=r.getNextLine();
		}
		if(candidateID!=0) {
			Invocation i = new Invocation(candidateID, senderID, receiverID);
			Invocations.add(i);
		}
		checkForType(st);
	}
	
	private void interpretInheritance(String st1) throws Exception{
		maxID=getID(st1.toCharArray());
		String st=r.getNextLine();
		while(!st.contains("subclass")) {
			st=r.getNextLine();
		}
		long SubclassID=getID(st.toCharArray());
		st=r.getNextLine();
		while(!st.contains("superclass")) {
			st=r.getNextLine();
		}
		long SuperclassID=getID(st.toCharArray());
		Inheritance i=new Inheritance(SubclassID,SuperclassID);
		Inherits.add(i);
	}
	
	private void interpretAttribute(String st) throws Exception{
		long AttributeID=getID(st.toCharArray());
		maxID=AttributeID;
		Attribute a=new Attribute(AttributeID,st.contains("LocalVariable"),st.contains("FAMIX.Parameter "));
		st=r.getNextLine();
		long type=0,parentType=0;
		String modifiers="";
		while(st!=null && !st.contains("FAMIX.")) {
			if(st.contains("declaredType")) type=getID(st.toCharArray());
			if(st.contains("modifiers")) modifiers=getModifiers(st.toCharArray());
			if(st.contains("parent")) parentType=getID(st.toCharArray());
			st=r.getNextLine();
		}
		a.setContainerID(parentType);
		a.setType(type);
		a.setModifiers(modifiers);
		Attributes.add(a);
		checkForType(st);
	}
	
	private void interpretFile(String st1) throws Exception{
	    maxID=getID(st1.toCharArray());
		String st=r.getNextLine();
		long ID=getID(st.toCharArray());
		while(!st.contains("fileName")) {
			st=r.getNextLine();
		}
		String FileName=getName(st.toCharArray());
		Iterator<ContainingFile> it=Files.iterator();
		boolean entered=false;
		while(it.hasNext()) {
			ContainingFile f=it.next();
			if(f.getFileName().equals(FileName)) {
				f.addID(ID);
				entered=true;
			}
		}
		if(entered==false) {
			ContainingFile f=new ContainingFile(FileName);
			f.addID(ID);
			Files.add(f);
		}
	}
	
	private void interpretClass(String st) throws Exception{
		boolean container=st.contains("FAMIX.ParameterizableClass");
		long ClassID=getID(st.toCharArray());
		maxID=ClassID;
		long containerID=0;
		st=r.getNextLine();
		String ClassName=getName(st.toCharArray());
		st=r.getNextLine();
		boolean Interface=false;
		while(st!=null && !st.contains("FAMIX.")) {
		    if(st.contains("(container ")) containerID=getID(st.toCharArray());
		    if(st.contains("isInterface")) Interface=true;
			st = r.getNextLine();
		}
		if(st!=null && st.contains("isInterface")) Interface=true;
		FamixClass c=new FamixClass(Interface,ClassID,ClassName,containerID);
		Classes.add(c);
		checkForType(st);
	}
	
	public String getName(char[] chArray) throws Exception{
		ArrayList<Character> Name=new ArrayList<Character>();
		int i;
		boolean found=true;
		for(i=0;found ;i++) {
			if(i>=chArray.length){
				chArray=r.getNextLine().toCharArray();
				i=0;
			}
			if(chArray[i]=='\'') {
				do {
					i++;
					found=false;
					if(i==chArray.length){
						chArray=r.getNextLine().toCharArray();
						i=0;
					}
					if(chArray[i]!='\'') Name.add(chArray[i]);
				}while (chArray[i]!='\'');
			}
		}
		return charListToString(Name);
	}

	private long getID(char[] chArray) {
		ArrayList<Character> IdDigits=new ArrayList<Character>();
		int i;
		for(i=0;i<chArray.length;i++) {
			while(chArray[i]>='0' && chArray[i]<='9') {
				IdDigits.add(chArray[i]);
				i++;
			}
		}
		return digitsListToLong(IdDigits);
	}
	
	private String charListToString(ArrayList<Character> ClassName) {
		Iterator<Character> it=ClassName.iterator();
		String st="";
		while(it.hasNext()) {
			st=st+it.next();
		}
		return st;
	}
	
	private long digitsListToLong(ArrayList<Character> IdDigits) {
		Iterator<Character> it=IdDigits.iterator();
		if(IdDigits.isEmpty()) return 0L;
		String st="";
		while(it.hasNext()) {
		    char c=it.next();
			st=st+c;
		}
		return Long.parseLong(st);
	}

	private FamixClass getClassByID(long ID) {
		Iterator<FamixClass> it=Classes.iterator();
		while(it.hasNext()) {
			FamixClass c=it.next();
			if(c.getID()==ID) return c;
		}
		return null;
	}
	
	public void setClassesMethods() {
		Iterator<Method> mit=Methods.iterator();
		while(mit.hasNext()) {
			Method m=mit.next();
			long ID=m.getParentType();
			FamixClass c=getClassByID(ID);
			if(c!=null) {
				c.addMethod(m);
				m.setClass(c);
			}
		}
	}

	public void setContainingFiles() {
		Iterator<ContainingFile> it=Files.iterator();
		while(it.hasNext()) {
			ContainingFile f=it.next();
			ArrayList<Long> IDs=f.getContainedIDs();
			Iterator<Long> IDiterator=IDs.iterator();
			while(IDiterator.hasNext()) {
				Long ID=IDiterator.next();
				FamixClass c=getClassByID(ID);
				if(c!=null) {
					c.setFile(f);
					f.addClass(c);
				}
			}
		}
	}
	
	public void setInheritanceRelations() {
		Iterator<Inheritance> it=Inherits.iterator();
		while(it.hasNext()) {
			Inheritance i=it.next();
			FamixClass subclass=getClassByID(i.getSubclassID());
			FamixClass superclass=getClassByID(i.getSuperclassID());
			if(subclass!=null && superclass!=null) {
				subclass.addInheritedClass(superclass);
			}
		}
	}
	
	public Method getMethodByID(long ID) {
		Iterator<Method> it=Methods.iterator();
		while(it.hasNext()) {
			Method m=it.next();
			if(m.getID()==ID) return m;
		}
		return null;
	}
	
	public Attribute getAttributeByID(long ID) {
		Iterator<Attribute> it=Attributes.iterator();
		while(it.hasNext()) {
			Attribute a=it.next();
			if(a.getID()==ID) return a;
		}
		return null;
	}
	
	private void setClassToAttribute() {
		Iterator<Attribute> it=Attributes.iterator();
		while(it.hasNext()) {
			Attribute a=it.next();
			long ID=a.getTypeID();
			FamixClass c=getClassByID(ID);
			a.setClass(c);
			Method m=getMethodByID(a.getContainerID());
			if(m!=null) {
                a.setContainerMethod(m);
                a.setContainerClass(m.getParent());
            }else{
			    FamixClass containerClass=getClassByID(a.getContainerID());
			    if(containerClass==null){
			        ParametrizedType pt=getParametrizedTypeByID(a.getContainerID());
			        if(pt==null) continue;
			        containerClass=getClassByID(pt.parameterizableClassID);
			        if(containerClass==null) continue;
                }
			    a.setContainerClass(containerClass);
            }
		}
	}

	public void setInvocations() {
		Iterator<Invocation> it=Invocations.iterator();
		while(it.hasNext()) {
			Invocation i=it.next();
			Method candidate=getMethodByID(i.getCandidateID());
			Method sender=getMethodByID(i.getSenderID());
			if(!sender.isProtected()) candidate.addCalledMethod(sender);
			else candidate.addProtectedMethod(sender);
			if(i.hasReceiver()) {
				Attribute receiver=getAttributeByID(i.getReceiverID());
				if(receiver!=null) {
					if(candidate.getParent()==null) continue;
					if(!candidate.getParent().getContainer()) {
						if (receiver.isProtected()) candidate.addProtectedAttribute(receiver);
						else candidate.addAccessedAttribute(receiver);
					}else{
						if(candidate.getParent().getContainedTypes().contains(receiver.getType())){
							receiver.getType().setNotViable();
							addAttribute(receiver.getType().getExtender(),receiver,candidate);
						}else{
							if(receiver.isProtected()) candidate.addProtectedAttribute(receiver);
							else candidate.addAccessedAttribute(receiver);
						}
					}
				}
			}
		}
        Iterator<Attribute> it1=Attributes.iterator();
		while(it1.hasNext()) {
            Attribute a = it1.next();
            if (a.isLocalVariable()) {
                Method m = a.getContainerMethod();
                m.addAccessedAttribute(a);
            }
        }
	}

	private void setReturnToMethods(){
		Iterator<Method> it=Methods.iterator();
		while(it.hasNext()){
			Method m=it.next();
			FamixClass c=getClassByID(m.getDeclaredTypeID());
			if(c!=null) {
				m.setDeclaredType(c);
			}
		}
	}

	private void setAttributesToParametrizableClasses(){
		Iterator<Access> it=Accesses.iterator();
		while(it.hasNext()){
			Access ac=it.next();
			Attribute a=getAttributeByID(ac.getVariableID());
			Method mac=getMethodByID(ac.getAccessorID());
			if(a==null) continue;
			FamixClass type=a.getType();
			if(mac!=null) {
				FamixClass containerClass = mac.getParent();
				if(containerClass == null) continue;
				if (containerClass.getContainer()) {
					if (containerClass.getContainedTypes().contains(type)) {
						type.setNotViable();
						addAttribute(type.getExtender(), a, mac);
					}else {
						if (a.isProtected()) mac.addProtectedAttribute(a);
						else mac.addAccessedAttribute(a);
					}
				} else {
					if (a.isProtected()) mac.addProtectedAttribute(a);
					else mac.addAccessedAttribute(a);
				}
			}else{
				FamixClass containerClass=getClassByID(a.getContainerID());
				if(containerClass.getContainer()){
					if(containerClass.getContainedTypes().contains(type)){
						type.setNotViable();
						addAttribute(type.getExtender(),a,mac);
						addAttribute(type.getExtender(),a,containerClass);
					}else{
						if(a.isProtected()){
							containerClass.addProtectedAttribute(a);
							mac.addProtectedAttribute(a);
						}
						else{
							containerClass.addAccessedAttribute(a);
							mac.addAccessedAttribute(a);
						}
					}
				}else{
					if(a.isProtected()){
						containerClass.addProtectedAttribute(a);
						mac.addProtectedAttribute(a);
					}
					else{
						containerClass.addAccessedAttribute(a);
						mac.addAccessedAttribute(a);
					}
				}
			}
		}
	}

	private void addAttribute(FamixClass classSwitcher,Attribute cloned,Method container){
		cloned.setParamType();
		cloned.setNotViable();
		if(!AttributeConnections.containsKey(cloned)) {
			maxID++;
			Attribute a = new Attribute(maxID, cloned.isLocalVariable(), cloned.isParameter());
			if (classSwitcher == null) return;
			a.setType(classSwitcher.getID());
			a.setClass(classSwitcher);
			a.setContainerID(cloned.getContainerID());
			a.setContainerMethod(cloned.getContainerMethod());
			a.setModifiers(cloned.getModifiers());
			if (a.isProtected()) container.addProtectedAttribute(a);
			else container.addAccessedAttribute(a);
			container.getParent().addAttribute(a);
			setContainer(a);
			Attributes.add(a);
			AttributeConnections.put(cloned,a);
		}else{
			Attribute a=AttributeConnections.get(cloned);
			if(a.isProtected()) container.addProtectedAttribute(a);
			else container.addAccessedAttribute(a);
			container.getParent().addAttribute(a);
		}
	}

	private void addAttribute(FamixClass classSwitcher,Attribute cloned,FamixClass container){
		cloned.setParamType();
		cloned.setNotViable();
		if(!AttributeConnections.containsKey(cloned)) {
			maxID++;
			if (container == null) return;
			Attribute a = new Attribute(maxID, cloned.isLocalVariable(), cloned.isParameter());
			if (classSwitcher == null) return;
			a.setType(classSwitcher.getID());
			a.setContainerID(cloned.getContainerID());
			a.setContainer(container);
			a.setClass(classSwitcher);
			a.setModifiers(cloned.getModifiers());
			if (a.isProtected()) container.addProtectedAttribute(a);
			else container.addAccessedAttribute(a);
			container.addAttribute(a);
			setContainer(a);
			Attributes.add(a);
			AttributeConnections.put(cloned,a);
		}else{
			Attribute a=AttributeConnections.get(cloned);
			if(a.isProtected()) container.addProtectedAttribute(a);
			else container.addAccessedAttribute(a);
			container.addAttribute(a);
		}
	}

	public ArrayList<Method> getMethods(){
	    return Methods;
    }

    private void setLocalParametrized(){
		for(int i=0;i<Attributes.size();i++){
			Attribute a=Attributes.get(i);
			if(a.isLocalVariable()){
				FamixClass type=a.getType();
				Method m=getMethodByID(a.getContainerID());
				if(m==null) continue;
				FamixClass container=m.getParent();
				if(container==null) continue;
				if(container.getContainer()){
					if(container.getContainedTypes().contains(type)){
						addAttribute(type.getExtender(),a,m);
					}
				}
			}
		}
	}

	private void setContainerToAttribute(){
		Iterator<Attribute> it=Attributes.iterator();
		while(it.hasNext()){
			Attribute a=it.next();
			FamixClass Container=getClassByID(a.getContainerID());
			if(Container!=null) {
			    Container.addAttribute(a);
            }else{
			    Method m=getMethodByID(a.getContainerID());
			    if(m!=null) {
                    FamixClass c = m.getParent();
                    if(c!=null) {
                        c.addAttribute(a);
                        Container = c;
                    }
                }
            }
			a.setContainer(Container);
		}
	}

	private void setParameterToContainers(){
		Iterator<Parameter> it=Parameters.iterator();
		while(it.hasNext()){
			Parameter p=it.next();
			FamixClass c=getClassByID(p.getDeclaredID());
			p.setDeclaredType(c);
			Method m=getMethodByID(p.getContainerMethodID());
			p.setContainerMethod(m);
			m.addParameter(p);
		}
	}

	private void setParameterizedAttributes(){
		Iterator<Attribute> it=Attributes.iterator();
		for(int i=0;i<Attributes.size();i++){
			Attribute a=Attributes.get(i);
			ParametrizedType p=getParametrizedTypeByID(a.getTypeID());
			if(p!=null){
				Method m=getMethodByID(a.getContainerID());
				FamixClass container;
				if(m!=null) {
					a.setClass(getClassByID(p.parameterizableClassID));
					a.setType(p.parameterizableClassID);
					a.setContainerMethod(m);
					if(a.isProtected()) m.addProtectedAttribute(a);
					else m.addAccessedAttribute(a);
				}
				else {
					container = getClassByID(a.getContainerID());
					a.setType(p.parameterizableClassID);
					a.setClass(getClassByID(p.parameterizableClassID));
					if(container!=null) {
						if (a.isProtected()) container.addProtectedAttribute(a);
						else container.addAccessedAttribute(a);
					}
				}
			}
		}
	}

	public ParametrizedType getParametrizedTypeByID(long ID){
		Iterator<ParametrizedType> it=ParamTypes.iterator();
		while(it.hasNext()){
			ParametrizedType p=it.next();
			if(p.ID==ID) return p;
		}
		return null;
	}

	private void setContainerToClass(){
        Iterator<FamixClass> it=Classes.iterator();
        while(it.hasNext()){
            FamixClass c=it.next();
            long containerID=c.getContainerID();
            FamixClass container=getClassByID(containerID);
            if(container!=null) {
            	container.addContainedType(c);
            	container.setContainer();
			}
            FamixNamespace namespace=getNamespaceByID(containerID);
            if(namespace!=null){
                c.setNamespace(namespace);
            }
        }
    }

    private FamixNamespace getNamespaceByID(long ID){
	    Iterator<FamixNamespace> it=Namespaces.iterator();
	    while(it.hasNext()){
	        FamixNamespace n=it.next();
	        if(n.ID==ID) return n;
        }
	    return null;
    }

    private void setContainer(Attribute a){
        long ID=a.getTypeID();
        FamixClass c=getClassByID(ID);
        a.setClass(c);
        Method m=getMethodByID(a.getContainerID());
        if(m!=null) {
            a.setContainerMethod(m);
            a.setContainerClass(m.getParent());
        }else{
            FamixClass containerClass=getClassByID(a.getContainerID());
            if(containerClass==null){
                ParametrizedType pt=getParametrizedTypeByID(a.getContainerID());
                if(pt==null) return;
                containerClass=getClassByID(pt.parameterizableClassID);
                if(containerClass==null) return;
            }
            a.setContainerClass(containerClass);
        }
    }

    private void setContainerToNamespaces(){
	    Iterator<FamixNamespace> it=Namespaces.iterator();
	    while(it.hasNext()){
	        FamixNamespace n=it.next();
	        FamixNamespace container=getNamespaceByID(n.parentID);
	        n.namespace=container;
        }
    }

    public void initialize() {
		System.out.println("Binding the relations to classes, methods and attributes...");
		setContainerToClass();
		setContainingFiles();
		setClassesMethods();
		setClassToAttribute();
		setParameterizedAttributes();
		setInheritanceRelations();
		setInvocations();
		setReturnToMethods();
		setContainerToAttribute();
		setAttributesToParametrizableClasses();
        setLocalParametrized();
        setContainerToNamespaces();
		/*set parameterized attributes */
		Iterator<FamixClass> it=Classes.iterator();
		while(it.hasNext()) {
			FamixClass c=it.next();
			c.setOverrideOrSpecialize();
		}
		it=Classes.iterator();
		while(it.hasNext()) {
			FamixClass c=it.next();
			c.set();
		}

		System.out.println("Binding the relations to the files...");
		Iterator<ContainingFile> itf=Files.iterator();
		while(itf.hasNext()){
			ContainingFile f=itf.next();
			f.setEverything();
		}
		it = Classes.iterator();

		System.out.println("Binding inheritance relations to classes...");
		while(it.hasNext()){
			FamixClass c= it.next();
			c.setInheritanceRelations();
		}
	}

	public ArrayList<ContainingFile> getFiles(){
		return Files;
	}

	public String getFileExternals(){
	    String st="[";
	    Iterator<ContainingFile> it=Files.iterator();
	    while(it.hasNext()){
	        ContainingFile f=it.next();
	        st=st+f.getExternals();
        }
	    st=st+"\n]";
        return st;
    }

	public String getClassMetrics(){
        String st="file,class,AMW,WMC,NOM,NOPA,NOAV,NProtM,ATFD,ATFD2,FDP,TCC,LAA,WOC,BOvR,CC,CM,CINT,CDISP,BUR,HIT,DIT,NOC,RFC,CBO,CBO2" +
				",WMC_STATIC,NOM_STATIC,FANOUT_STATIC,FANOUT_CLS_STATIC,FANIN_STATIC,FANIN_CLS_STATIC\n";
        Iterator<FamixClass> it=Classes.iterator();
        while(it.hasNext()){
        	FamixClass c=it.next();
        	if(c.getContainingFile()!=null){
        		st=st+c.getContainingFile().getFileName()+","+c.getClassName()+","+c.getMetrics(this)+"\n";
			}
		}
        return st;
    }

    public String getMethodAntiPatterns(){
		String st = "file,class,signature,anti-pattern,metrics\n";
		Iterator<Method> it = Methods.iterator();
		while(it.hasNext()){
			Method m = it.next();
			String result = m.isBottleneck();
			if(!result.equals("")) st = st+result+"\n";
			result = m.isFeatureEnvy();
			if(!result.equals("")) st = st+result+"\n";
			result = m.isBlob();
			if(!result.equals("")) st = st+result+"\n";
		}
		return st;
	}

	public String getClassHierarchy(){
		String st = "derived_class,base_class,number_hierarchy_relations\n";
		Iterator<FamixClass> it = Classes.iterator();
		while(it.hasNext()){
			FamixClass c = it.next();
			st = st+c.getInheritanceRelations();
		}
		return st;
	}

    public Set<Method> getMethodsThatCallMethod(Method m){
        Iterator<Method> it = Methods.iterator();
        Set<Method> s = new HashSet<>();
        while(it.hasNext()){
            Method mit = it.next();
            if(mit.isNotDefaultConstructor() && !mit.isAccessor() && mit.getCalledMethods().contains(m)
					&& mit.getParent()!=null && mit.getParent() != m.getParent()) s.add(mit);
        }
        return s;
    }

    public void checkParameters(){

	}

	public String toString() {
		String st="source,target,extCalls,extData,hierarchy,returns,declarations,extDataStrict,all\n";
		Iterator<ContainingFile> it=Files.iterator();
		while(it.hasNext()) {
			ContainingFile f=it.next();
			st=st+f;
		}
		return st;
	}
}
