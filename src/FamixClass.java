import java.util.*;
class FamixClass {
	private long ID;
	private boolean viable=true;
	private String ClassName;
	private boolean isParametrizedType=false;
	private Set<FamixClass> ParametrizedClasses=new HashSet<FamixClass> ();
	private long containerID;
	private ArrayList<FamixClass> containedTypes=new ArrayList<FamixClass>();
	private String Name;
	private ArrayList<Method> OverrideOrSpecializeMethods=new ArrayList<Method> ();
	private ArrayList<Method> ContainedMethods=new ArrayList<Method> ();
	private HashSet<Attribute> ContainedAttributes=new HashSet<Attribute> ();
	private ArrayList<FamixClass> InheritedClasses=new ArrayList<FamixClass> ();
	private Map<FamixClass,Method> OverrideClassAndMethod=new HashMap<FamixClass,Method>();
	private Map<FamixClass,Integer> InheritanceRelations=new HashMap<FamixClass,Integer> ();
	private Set<Method> CalledMethods=new HashSet<Method>();
	private Set<Attribute> AccessedAttributes=new HashSet<Attribute> ();
	private Set<Method> ProtectedCalledMethods=new HashSet<Method> ();
	private Set<Attribute> ProtectedAccessedAttributes=new HashSet<Attribute>();
	private Map<FamixClass,Integer> Calls=new HashMap<FamixClass,Integer> ();
	private Map<FamixClass,Integer> Accesses=new HashMap<FamixClass,Integer> ();
	private Map<FamixClass, Tuplet> ClassesRelations=new HashMap<FamixClass, Tuplet> ();
	private Map<FamixClass,ArrayList<Method>> OverrideRelations=new HashMap<FamixClass,ArrayList<Method>> ();
	private ContainingFile File;
	private FamixNamespace namespace;
	private boolean isFromJava=true;
	private boolean Interface;

	public FamixClass(boolean Interface, long ID, String Name,long containerID) {
		this.ID=ID;
		this.Name=Name;
		this.Interface=Interface;
		this.containerID=containerID;
		ClassName=Name;
	}

	public void setNotViable(){
        viable=false;
    }

    public FamixNamespace getNamespace(){
	    return namespace;
    }

    public void setNamespace(FamixNamespace namespace){
	    this.namespace=namespace;
    }
	public boolean isInterface(){
	    return Interface;
    }

    public boolean isViable(){
	    return viable;
    }

    public int getWMC_STATIC(){
		return ContainedMethods.stream()
				.filter(method -> method!=null)
				.filter(Method::isStatic)
				.distinct()
				.mapToInt(Method::getCyclomaticComplexity)
				.sum();
	}

	public int getNOM_STATIC(){
		return (int)ContainedMethods.stream()
				.filter(method -> method!=null)
				.filter(Method::isStatic)
				.distinct()
				.count();
	}

	public Set<Method> getExtStaticMethodsFromSet(Set<Method> methods){
		Set<Method> s = new HashSet<Method>();
		Iterator<Method> it = methods.iterator();
		while(it.hasNext()){
			Method m = it.next();
			if(m == null) continue;
			if(m.isStatic() && m.isUserDefined() && m.getParent()!=null && m.getParent()!=this && !isRelated(m.getParent())) s.add(m);
		}
		return s;
	}

	public int getFANOUT_STATIC(){
		Set<Method> s = getExtStaticMethodsFromSet(CalledMethods);
		s.addAll(ProtectedCalledMethods);
		return s.size();
	}

	public int getFANOUT_CLS_STATIC(){
		Set<Method> s = getExtStaticMethodsFromSet(CalledMethods);
		s.addAll(ProtectedCalledMethods);
		return (int)s.stream()
				.map(Method::getParent)
				.distinct()
				.count();
	}

	public Set<Method> getMethodsThatCallStaticMethods(Interpreter i){
		Iterator<Method> it = ContainedMethods.iterator();
		Set<Method> sm = new HashSet<>();
		while(it.hasNext()) {
			Method m = it.next();
			if(m==null || !m.isStatic()) continue;
			Set<Method> s = i.getMethodsThatCallMethod(m);
			Iterator<Method> mit = s.iterator();
			while(mit.hasNext()){
				Method method = mit.next();
				if(method.getParent()!=this && method.getParent()!=null && !isRelated(method.getParent()))
					sm.add(method);
			}
		}
		return sm;
	}

	public int getFANIN_STATIC(Interpreter i){
		return getMethodsThatCallStaticMethods(i).size();
	}

	public int getFANIN_CLS_STATIC(Interpreter i){
		return (int)getMethodsThatCallStaticMethods(i).stream()
				.map(Method::getParent)
				.distinct()
				.count();
	}

    public Set<Attribute> getForeignAccesses(){
        Set<Attribute> aa=new HashSet<Attribute>();
        Iterator<Attribute> it=AccessedAttributes.iterator();
        while(it.hasNext()){
            Attribute a=it.next();
            if(a.getType()==null) continue;
            if(!isRelated(a.getType()) && !ContainedAttributes.contains(a)){
                aa.add(a);
            }
        }
        Iterator<Method> itm=CalledMethods.iterator();
        while(itm.hasNext()){
            Method m=itm.next();
            if(!m.isConstr() && m.isAccessor()){
                aa.add(m.getAccessorAttribute());
            }
        }
        return aa;
    }

    public int getATFD(){
        return (int)getForeignAccesses().stream()
                        .filter(attribute -> attribute!=null)
                        .count();
    }

    public boolean isRelated(FamixClass c){
        if(this.equals(c)) return true;
        else{
            Iterator<FamixClass> it=InheritedClasses.iterator();
            while(it.hasNext()){
                FamixClass c1=it.next();
                if(c1.isRelated(c)) return true;
            }
        }
        return false;
    }

    public void addContainedType(FamixClass c){
        containedTypes.add(c);
    }

    public ArrayList<FamixClass> getContainedTypes(){
	    return containedTypes;
    }

    public long getContainerID(){
	    return containerID;
    }

    public Set<Method> getCalledMethods(){
		return CalledMethods;
	}

	public Set<Attribute> getProtectedAccessedAttributes(){
		Set<Attribute> s=new HashSet<Attribute>();
		s.addAll(notContainedFromSet(ProtectedAccessedAttributes));
		s.addAll(getAttributesFromAccessorMethods(ProtectedCalledMethods,1));
		return s;
	}

	public Set<Attribute> getAttributesFromAccessorMethods(Set<Method> s,int i){
		Iterator<Method> itm=s.iterator();
		Set<Attribute> sa=new HashSet<Attribute>();
		while(itm.hasNext()){
			Method m=itm.next();
			if(m.isAccessor() && m.getParent()!=this){
				if(i==1){
					if(m.isProtected()) sa.add(m.getAccessorAttribute());
				}else{
					if(m.isPublic()) sa.add(m.getAccessorAttribute());
				}
			}
		}
		return sa;
	}

	public Set<Attribute> notContainedFromSet(Set<Attribute> s){
		Iterator<Attribute> it=s.iterator();
		Set<Attribute> newSet=new HashSet<Attribute> ();
		while(it.hasNext()){
			Attribute a=it.next();
			if(!ContainedAttributes.contains(a)) newSet.add(a);
		}
		return newSet;
	}

	public Set<Method> removeAccessors(Set<Method> methods){
		Set<Method> m=new HashSet<Method>();
		Iterator<Method> it=methods.iterator();
		while(it.hasNext()){
			Method single=it.next();
			if(!single.isAccessor()) m.add(single);
		}
		return m;
	}

	public Set<Method> getCalledProtectedMethods(){
		Set<Method> pcm=ProtectedCalledMethods;
		return pcm;
	}

	public Set<Attribute> getRealAccessedAttributes(){
		return AccessedAttributes;
	}

	public Set<Attribute> getAccessedAttributes(){
		Set<Attribute> s=new HashSet<Attribute>();
		s.addAll(notContainedFromSet(AccessedAttributes));
		s.addAll(getAttributesFromAccessorMethods(CalledMethods,0));
		return s;
	}

	public void addMethod(Method m) {
		ContainedMethods.add(m);
	}

	public void addAttribute(Attribute a) {
		ContainedAttributes.add(a);
	}

	public boolean isBOVRProper(Method m){
	    if(ContainedMethods.contains(m)) {
            Iterator<FamixClass> it=InheritedClasses.iterator();
            while(it.hasNext()){
                FamixClass c=it.next();
                if(c.hasMethod(m.getSignature())) return true;
            }
            return false;
        }
	    return false;
    }

	public void setOverrideOrSpecialize() {
		Iterator<Method> it=ContainedMethods.iterator();
		while(it.hasNext()) {
			Method m=it.next();
			if(!m.getSignature().contains("<init>")) {
                FamixClass c = this.isOverrideOrSpecialize(m);
                if (c != null) {
                    OverrideOrSpecializeMethods.add(m);
                    addClassToMap(InheritanceRelations, c, 2);
                    if (OverrideRelations.containsKey(c)) {
                        OverrideRelations.get(c).add(m);
                    } else {
                        ArrayList<Method> adder = new ArrayList<Method>();
                        adder.add(m);
                        OverrideRelations.put(c, adder);
                    }
                }
            }
		}
		setInherits();
		
	}

	public Map<FamixClass,ArrayList<Method>> getOverrideRelations(){
		return OverrideRelations;
	}

	private void setInherits() {
		Iterator<FamixClass> inheritIterator=InheritedClasses.iterator();
		while(inheritIterator.hasNext()) {
			FamixClass c=inheritIterator.next();
			if(!InheritanceRelations.containsKey(c)) {
				InheritanceRelations.put(c,1);
			}
		}
	}

	public void addParametrizedClass(FamixClass c){
		ParametrizedClasses.add(c);
	}

	public Set<FamixClass> getParametrizedClasses(){
		return ParametrizedClasses;
	}

	public ArrayList<Method> getOverrideOrSpecialize(){
		return OverrideOrSpecializeMethods;
	}

	public boolean getContainer(){
		return isParametrizedType;
	}

	public ArrayList<Method> getContainedMethods(){
	    return ContainedMethods;
    }

    public int getContainedNumber(FamixClass c){
		Iterator<FamixClass> it=containedTypes.iterator();
		int cnt=0;
		while(it.hasNext()){
			FamixClass c1=it.next();
			cnt++;
			if(c1.equals(c)) return	cnt;
		}
		return cnt;
	}

	public boolean hasMethod(String signature) {
		Iterator<Method> it=ContainedMethods.iterator();
		while(it.hasNext()) {
			Method m=it.next();
			if(m.getSignature().equals(signature)) return true;
		}
		
		return false;
	}

	public FamixClass isOverrideOrSpecialize(Method m) {
		Iterator<FamixClass> it=InheritedClasses.iterator();
		while(it.hasNext()) {
			FamixClass c=it.next();
			if(c.hasMethod(m.getSignature())) return c;
			FamixClass b=c.isOverrideOrSpecialize(m);
			if(b!=null) return b;
		}
		return null;
	}

	public Set<Attribute> getDeclaredAttributes(){
	    Set<Attribute> s=new HashSet<Attribute>();
        Iterator<Attribute> it=ContainedAttributes.iterator();
        while(it.hasNext()){
            Attribute a=it.next();
            if(a.getContainerClass().equals(this)) s.add(a);
        }
        return s;
    }

    public HashSet<Attribute> getContainedAttributes(){
	    return ContainedAttributes;
    }

	public Method getMethod(String signature){
	    Iterator<Method> it=ContainedMethods.iterator();
	    while(it.hasNext()){
	        Method m=it.next();
	        if(m.getSignature().equals(signature)) return m;
        }
	    return null;
    }

	public long getID() {
		return ID;
	}

	public String getName() {
		return Name;
	}

	public void setFile(ContainingFile f) {
		if(f!=null) isFromJava=false;
		File=f;
		Name=f.getFileName()+"/"+Name;
	}

	public void addInheritedClass(FamixClass c) {
		InheritedClasses.add(c);
	}

	public ArrayList<FamixClass> getInheritedClasses(){
		return InheritedClasses;
	}

	public boolean equals(Object o) {
		if(o instanceof FamixClass) {
			FamixClass c=(FamixClass) o;
			if(c.getID()==ID) return true;
		}
		return false;
	}

	public String getClassName(){
		return ClassName;
	}

	public double getAMW(){
	    return ContainedMethods.stream()
				.filter(method-> !method.getSignature().contains("<init>"))
				.filter(Method::isNotDefaultConstructor)
                .mapToInt(Method::getCyclomaticComplexity)
                .average()
                .orElse(0);
    }

    public int getNOAV(){
		return (int) ProtectedAccessedAttributes.stream()
        .filter(Attribute::isUserDefined).filter(Attribute::isViable).count()+(int)AccessedAttributes.stream().filter(Attribute::isUserDefined).filter(Attribute::isViable).count();
	}

	public Double round(Double d){
		return ((double) ((int) (d*100)))/100;
	}

    public int getWMC(){
		return ContainedMethods.stream()
				.filter(method -> !method.getSignature().contains("<init>"))
				.filter(Method::isNotDefaultConstructor)
				.mapToInt(Method::getCyclomaticComplexity)
				.sum();
	}

	public int getNOM(){
		return (int) ContainedMethods.stream()
				.filter(method -> !method.getSignature().contains("<init>"))
				.filter(Method::isNotDefaultConstructor)
				.count();
	}

	public int getNProtMA(){
        return (int)ContainedAttributes.stream()
                .filter(Attribute::isProtected)
                .filter(Attribute::isViable)
                .count();
    }

    public int getNProtMM(){
	    return (int)ContainedMethods.stream()
                .filter(method -> method.isProtected())
                .count();
    }

    public boolean isUserDefined(){
	    if(File==null) return false;
	    return true;
    }

    public int getNProtM(){
	    return getNProtMA()+getNProtMM();
    }

    public String getMetrics(Interpreter i){
		String st="";
		st=st+round(getAMW())+","+getWMC()+","+getNOM()+","+getNOPA()+","+getNOAV()+","+getNProtM()+","+getATFD()+","+getATFD2()+","+getFDP()+
				","+round(getTCC())+","+ round(getLAA())+","+round(getWOC())+","+round(getBOvR())+","+getCC(i)+","+getCM(i)+","+getCINT()+
				","+round(getCDISP())+","+ round(getBUR())+","+getHIT(i)+","+getDIT(this,0)+","+getNOC(i)+","+getRFC()+
				","+getCBO()+","+getCBO2()+","+ getWMC_STATIC()+ ","+getNOM_STATIC()+","+getFANOUT_STATIC()+","+getFANOUT_CLS_STATIC()+
				","+getFANIN_STATIC(i)+","+getFANIN_CLS_STATIC(i);
		return st;
	}

	public int getCBO2(){
        Set<FamixClass> s=new HashSet<FamixClass>();
        s.addAll(getReturnClasses());
        s.addAll(getDeclaredClasses());
        return s.size();
    }

    public Set<FamixClass> getReturnClasses(){
	    Set<FamixClass> sc=getReturnsFromMethods(CalledMethods);
	    sc.addAll(getReturnsFromMethods(ProtectedCalledMethods));
	    sc.addAll(getReturnsFromMethods(ContainedMethods));
        return sc;
    }

    public Set<FamixClass> getReturnsFromMethods(Collection<Method> s){
        Set<FamixClass> sc=new HashSet<>();
        Iterator<Method> it=s.iterator();
        while(it.hasNext()){
            Method m=it.next();
            FamixClass c=m.getDeclaredType();
            if(c!=null){
                if(!c.isViable()) c=c.getExtender();
                if(c!=null && c!=this && c.isUserDefined()) sc.add(c);
            }
        }
        return sc;
    }

    public Set<FamixClass> getDeclaredClasses(){
	    Set<FamixClass> sc=new HashSet<FamixClass>();
	    Iterator<Attribute> it=ContainedAttributes.iterator();
	    while(it.hasNext()){
            Attribute a=it.next();
            FamixClass c=a.getType();
            if(c==null) continue;
            if(!c.isViable()) c=c.getExtender();
            if(c!=null && c.isUserDefined() && c!=this) sc.add(c);
        }
        return sc;
    }

	public int getCBO(){
        Set<FamixClass> s=new HashSet<FamixClass>();
        s.addAll(getExtDataClasses());
        s.addAll(getCalledClasses());
        return s.size();
    }

    public Set<FamixClass> getExtDataClasses(){
	    Set<Attribute> sa=getAccessedAttributes();
	    Set<FamixClass> sc=new HashSet<FamixClass>();
	    Iterator<Attribute> it=sa.iterator();
	    while(it.hasNext()){
	        Attribute a=it.next();
	        if(a==null) continue;
	        FamixClass c=a.getContainerClass();
	        if(c==null) continue;
            if(!c.isViable()) c=c.getExtender();
            if(c!=null && c.isUserDefined() && c!=this) {
                sc.add(c);
            }
        }
	    return sc;
    }

    public Set<FamixClass> getCalledClasses(){
	    Set<FamixClass> sc=new HashSet<FamixClass>();
	    Iterator<Method> it=CalledMethods.iterator();
	    while(it.hasNext()){
	        Method m=it.next();
	        if(m.isNotDefaultConstructor() && !m.isAccessor()) {
                FamixClass c = m.getParent();
                if(c==null) continue;
                if(!c.isViable()) c=c.getExtender();
                if(c!=null && c.isUserDefined() && c!=this) {
                    sc.add(c);
                }
            }
        }
	    return sc;
    }

	public int getNOC(Interpreter i){
	    Iterator<FamixClass> it=i.getClasses().iterator();
	    int NOC=0;
	    while(it.hasNext()){
	        FamixClass c=it.next();
	        if(c.getInheritedClasses().contains(this) && !c.isFromJava && c.isViable()) NOC++;
        }
	    return NOC;
    }

    public int getRFC(){
	    int noMethods=(int)ContainedMethods.stream()
                .filter(method -> !method.isAccessor())
                .filter(Method::isNotDefaultConstructor)
                .filter(method -> !method.getSignature().contains("<init>"))
                .count();
	    int noCalledMethods=(int) CalledMethods.stream()
                .filter(method -> !method.isAccessor())
                .filter(Method::isNotDefaultConstructor)
                .filter(method -> !ContainedMethods.contains(method))
                .filter(Method::isUserDefined)
                .count();
	    int noProtectedCalledMethods=(int) ProtectedCalledMethods.stream()
                .filter(method -> !method.isAccessor())
                .filter(Method::isNotDefaultConstructor)
                .filter(method -> !ContainedMethods.contains(method))
                .filter(Method::isUserDefined)
                .count();
	    return noMethods+noCalledMethods+noProtectedCalledMethods;
    }

	public int getDIT(FamixClass c,int height){
		if(isInterface()) return 0;
        Iterator<FamixClass> it=InheritedClasses.iterator();
        int maxHeight=height;
        while(it.hasNext()){
            FamixClass c1=it.next();
            if(!c1.isFromJava && !c1.isInterface()) {
                int newHeight = c1.getDIT(c, height + 1);
                if (newHeight > maxHeight) maxHeight = newHeight;
            }
        }
        return maxHeight;
    }

	public int getHIT(Interpreter i){
		if(isInterface()) return 0;
        ArrayList<FamixClass> classes=i.getClasses();
        Iterator<FamixClass> it=classes.iterator();
        int maxHeight=0;
        while(it.hasNext()){
            FamixClass c=it.next();
            if(!c.isViable() || c.isInterface()) continue;
            int height=c.getInheritHeight(this,1);
            if(height>maxHeight) maxHeight=height;
        }
        return maxHeight;
    }

    public int getInheritHeight(FamixClass c,int height){
	    if(InheritedClasses.contains(c)) {
	    	return height;
		}
	    Iterator<FamixClass> it=InheritedClasses.iterator();
	    int maxHeight=0;
	    while(it.hasNext()){
	        FamixClass c1=it.next();
	        if(!c1.isViable()) continue;
            int newHeight=c1.getInheritHeight(c,height+1);
            if(newHeight>maxHeight) maxHeight=newHeight;
        }
	    return maxHeight;
    }

	public double getBUR(){
	    Iterator<Attribute> ita=ProtectedAccessedAttributes.iterator();
	    int protectedAccesses=0;
	    int totalProtectedAttributes=0;
	    while(ita.hasNext()){
	        Attribute a=ita.next();
	        if(!a.isViable()) continue;
	        FamixClass type=a.getType();
	        if(InheritedClasses.contains(type)) protectedAccesses++;
        }
	    Iterator<Method> itm=ProtectedCalledMethods.iterator();
	    while(itm.hasNext()){
	        Method m=itm.next();
	        FamixClass type=m.getParent();
	        if(InheritedClasses.contains(type)) protectedAccesses++;
        }
	    Iterator<FamixClass> itc=InheritedClasses.iterator();
	    while(itc.hasNext()){
	        FamixClass c=itc.next();
            totalProtectedAttributes=c.getNoProtectedAccesses()+totalProtectedAttributes;
        }
	    if(totalProtectedAttributes==0) return 0;
	    return ((double)protectedAccesses)/((double)totalProtectedAttributes);
    }

    public int getNoProtectedAccesses(){
	    return (int)ContainedAttributes.stream()
                .filter(Attribute::isViable)
				.filter(Attribute::isProtected)
                .count()+(int)ContainedMethods.stream()
									.filter(Method::isProtected)
									.count();
    }

	public double getCDISP(){
        Set<FamixClass> s=new HashSet<FamixClass>();
        Iterator<Method> it=CalledMethods.iterator();
        while(it.hasNext()){
            Method m=it.next();
            if(m.isUserDefined() && m.getParent()!=null) s.add(m.getParent());
        }
        it=ProtectedCalledMethods.iterator();
        while(it.hasNext()){
            Method m=it.next();
            if(m.isUserDefined() && m.getParent()!=null) s.add(m.getParent());
        }
        int CINT=getCINT();
        if(CINT==0) return 0;
        return ((double)s.size())/((double)CINT);
    }

	public int getCINT(){
        return (int)CalledMethods.stream()
                        .filter(Method::isUserDefined).count()+
                (int)ProtectedCalledMethods.stream()
                        .filter(Method::isUserDefined).count();
    }

	public int getCM(Interpreter i){
	    Iterator<Method> it=ContainedMethods.iterator();
	    Set<Method> s=new HashSet<>();
	    while(it.hasNext()){
	        Method m1=it.next();
	        Set<Method> callers = m1.getMethodsThatCallThisMethod();
	        Iterator<Method> itm = callers.iterator();
	        while(itm.hasNext()) {
	        	Method m = itm.next();
				if (!this.equals(m.getParent()) && (m.accessesAttributeOfType(this) || m.callsMethodFromParentOfType(this)))
					s.add(m);
			}
        }
	    return s.size();
    }

	public double getBOvR(){
	    double nrOfOvMethods=ContainedMethods.stream()
                .filter(method -> !method.getSignature().contains("<init>"))
                .filter(method -> isBOVRProper(method))
                .filter(method -> !method.isConstr())
                .count();
	    double nrOfMethods=ContainedMethods.stream()
                .filter(method->!method.getSignature().contains("<init>"))
                .filter(method -> !method.isConstr())
                .count();
	    if(nrOfMethods==0) return 0;
	    return nrOfOvMethods/nrOfMethods;
    }

    public int getCC(Interpreter i){
        Set<FamixClass> s=new HashSet<FamixClass>();
        Iterator<Method> it = ContainedMethods.iterator();
        while(it.hasNext()){
        	Method m1 = it.next();
        	Set<Method> callers = m1.getMethodsThatCallThisMethod();
        	Iterator<Method> itm = callers.iterator();
        	while(itm.hasNext()) {
        		Method m = itm.next();
				FamixClass c = m.getParent();
				if(c==null) continue;
				if (!this.equals(c) && (c.callsMethodsFromType(this) || c.containsAccessedAttributeOfType(this)))
					s.add(c);
			}
        }
        return s.size();
    }

    public boolean callsMethodsFromType(FamixClass c){
        Iterator<Method> it=CalledMethods.iterator();
        while(it.hasNext()){
            Method m=it.next();
            if(c.equals(m.getParent())) return true;
        }
        it=ProtectedCalledMethods.iterator();
        while(it.hasNext()){
            Method m=it.next();
            if(c.equals(m.getParent())) return true;
        }
        return false;
    }

    public boolean containsAccessedAttributeOfType(FamixClass c){
	    Iterator<Attribute> it=AccessedAttributes.iterator();
	    while(it.hasNext()){
	        Attribute a=it.next();
	        if(c.equals(a.getType())) return true;
        }
	    it=ProtectedAccessedAttributes.iterator();
	    while(it.hasNext()){
	        Attribute a=it.next();
	        if(c.equals(a.getType())) return true;
        }
	    return false;
    }

	public double getLAA(){
        double nrOfContained=ContainedAttributes.stream()
                                .filter(Attribute::isViable)
                                .count();
        Set<Attribute> foreignAccesses=getForeignAccesses();
        foreignAccesses.addAll(AccessedAttributes);
        foreignAccesses.addAll(ProtectedAccessedAttributes);
        double nrOfAccesses=foreignAccesses.stream()
								.filter(attribute -> attribute!=null)
                                .filter(Attribute::isViable)
                                .filter(Attribute::isPrivate)
                                .count();
        if(nrOfAccesses==0) return nrOfContained;
        return nrOfContained/nrOfAccesses;
    }

	public int getATFD2(){
	    return (int)getForeignAccesses().stream()
                .filter(attribute -> attribute!=null)
                .map(Attribute::getContainerClass)
				.filter(famixClass -> famixClass!=null)
				.filter(famixClass -> famixClass.isUserDefined())
                .count();
    }

    public double getWOC(){
        double functionalMethods=ContainedMethods.stream()
                .filter(method -> !method.isAccessor())
                .filter(Method::isPublic)
                .filter(method -> !method.isConstr())
                .count();
        double allPublicMembers=ContainedMethods.stream()
                .filter(Method::isPublic)
                .filter(method -> !method.isAccessor())
                .count()+
                ContainedAttributes.stream()
                .filter(Attribute::isPublic)
                .filter(Attribute::isViable).distinct()
                .count();
        if(allPublicMembers==0) return 0;
        return functionalMethods/allPublicMembers;
    }

    public double getTCC(){
        int NP=(int)ContainedMethods.stream()
                .filter(method -> !method.isConstr())
                .filter(method -> !method.getSignature().contains("<init>"))
                .count();
        if(NP==1) return (double) 1;
        NP=(NP * (NP-1))/ 2;
        int NDC=0;
        for(int i=0;i<ContainedMethods.size();i++){
            Method current=ContainedMethods.get(i);
            if(!current.getSignature().contains("<init>") && !current.isConstr()) {
                for (int j = i + 1; j < ContainedMethods.size(); j++) {
                    Method next = ContainedMethods.get(j);
                    if (!next.getSignature().contains("<init>") && !next.isConstr()) {
                        int a1=next.howManyAttributesAccessesOutOfSet(current.getAccessedAttributes());
                        if(a1==1) {
                        	NDC++;
						}
                        else {
							int a2 = next.howManyAttributesAccessesOutOfSet(current.getProtectedAttributesAccessed());
							if(a2==1) {
								NDC++;
							}
						}
                    }
                }
            }
        }
        if(NP==0) return 0;
        return ((double)NDC)/((double)NP);
    }

	public int getFDP(){
	    return (int)getForeignAccesses().stream()
                .filter(attribute -> attribute!=null)
                .map(Attribute::getContainerClass)
                .distinct()
                .count();
    }

    public boolean containsAttribute(Attribute a){
		return ContainedAttributes.contains(a);
	}

	public FamixClass getExtender(){
	    if(InheritedClasses.size()>=1) return InheritedClasses.get(0);
	    else return null;
    }

    public void setContainer(){
		isParametrizedType=true;
	}

	public int getNOPA(){
		return (int)ContainedAttributes.stream()
                .filter(Attribute::isViable)
                .filter(Attribute::isPublic)
                .count();
	}

	public boolean isMethodInherited(Method m) {
		if(OverrideOrSpecializeMethods.contains(m)) return true;
		return false;
	}

	public void set() {
		Iterator<Method> it=ContainedMethods.iterator();
		while(it.hasNext()) {
			Method m=it.next();
			Set<Method> SCM=m.getCalledMethods();
			if(SCM!=null) CalledMethods.addAll(SCM);
			Set<Method> PCM=m.getProtectedMethodsCalled();
			if(PCM!=null) ProtectedCalledMethods.addAll(PCM);
			Set<Attribute> SCA=m.getAccessedAttributes();
			if(SCA!=null) AccessedAttributes.addAll(SCA);
			Set<Attribute> PCA=m.getProtectedAttributesAccessed();
			if(PCA!=null) ProtectedAccessedAttributes.addAll(PCA);
		}
	}

	public void addProtectedAttribute(Attribute a){
	    ProtectedAccessedAttributes.add(a);
    }

    public void addAccessedAttribute(Attribute a){
	    AccessedAttributes.add(a);
    }

    private void setMapForAccessedProtectedAttributes() {
		Iterator<Attribute> it=ProtectedAccessedAttributes.iterator();
		while(it.hasNext()) {
			Attribute a=it.next();
			FamixClass c=a.getType();
			addClassToMap(InheritanceRelations,c,1);
		}
	}

	private void setMapForCalledProtectedMethods() {
		Iterator<Method> it=ProtectedCalledMethods.iterator();
		while(it.hasNext()) {
			Method m=it.next();
			FamixClass c=m.getParent();
			addClassToMap(InheritanceRelations,c,1);
		}
	}

	private void setMapForCall() {
		Iterator<Method> CalledIterator=CalledMethods.iterator();
		while(CalledIterator.hasNext()) {
			Method m=CalledIterator.next();
			FamixClass c=m.getParent();
			addClassToMap(Calls,c,1);
		}
	}

	private void setMapForAccess() {
		Iterator<Attribute> AccessIterator=AccessedAttributes.iterator();
		while(AccessIterator.hasNext()) {
			Attribute a=AccessIterator.next();
			FamixClass c=a.getType();
			addClassToMap(Accesses,c,1);
		}
	}

	private void setTriplets() {
		for(Map.Entry<FamixClass,Integer> entry:Calls.entrySet()) {
			FamixClass c=entry.getKey();
			int i=entry.getValue();
			Tuplet t=new Tuplet();
			t.setNoCalls(i);
			ClassesRelations.put(c,t);
		}
		for(Map.Entry<FamixClass,Integer> entry:Accesses.entrySet()) {
			FamixClass c=entry.getKey();
			int i=entry.getValue();
			if(ClassesRelations.containsKey(c)) {
				Tuplet t=ClassesRelations.get(c);
				t.setNoAccesses(i);
			}
			else {
				Tuplet t=new Tuplet();
				t.setNoCalls(0);
				t.setNoAccesses(i);
				ClassesRelations.put(c,t);
			}
		}
		for(Map.Entry<FamixClass,Integer> entry:InheritanceRelations.entrySet()) {
			FamixClass c=entry.getKey();
			int i=entry.getValue();
			if(ClassesRelations.containsKey(c)) {
				Tuplet t=ClassesRelations.get(c);
				t.setNoInheritanceRelations(i);
			}
			else {
				Tuplet t=new Tuplet();
				t.setNoCalls(0);
				t.setNoAccesses(0);
				t.setNoInheritanceRelations(i);
				ClassesRelations.put(c,t);
			}
		}
	}

	public ContainingFile getContainingFile(){
		return File;
	}

	public boolean checkIfIsFromJava(){
		return isFromJava;
	}

	private void addClassToMap(Map<FamixClass,Integer> m, FamixClass c, int startValue) {
		if(m.containsKey(c)) {
			m.put(c,m.get(c)+1);
		} else m.put(c, startValue);
	}
	
	public int hashCode() {
		return (int) (ID ^ (ID >>>32));
	}

	public Method getInit(){
	    return ContainedMethods.stream()
                .filter(method -> method.getSignature().equals("<init>"))
                .findFirst()
                .orElse(null);
    }

    public void setInheritanceRelations(){
		setMapForAccessedProtectedAttributes();
		setMapForCalledProtectedMethods();
	}

	public String getInheritanceRelations(){
		String st = "";
		if(ClassName.contains("$")) return "";
		for(Map.Entry<FamixClass,Integer> entry:InheritanceRelations.entrySet()){
			FamixClass c = entry.getKey();
			if(c==null) continue;
			if(!InheritedClasses.contains(c) || c.getClassName().contains("$") || !c.isUserDefined() || c.isInterface()
					|| isInterface() || !isViable()
					|| !c.isViable() || !isUserDefined()) continue;
			int n = entry.getValue();
			st = ClassName + "," + c.getClassName() + "," + n +"\n";
		}
		return st;
	}

	public String toString() {
		String st="";
		for(Map.Entry<FamixClass, Tuplet> entry:ClassesRelations.entrySet()) {
			FamixClass c=entry.getKey();
			Tuplet t=entry.getValue();
			if(!this.equals(c)) {
				if (c != null) {
					 if(!c.checkIfIsFromJava() && !this.checkIfIsFromJava())st = st + Name + "," + c.getName() + "," + t + "\n";
				}
			}
		}
		return st;
	}
}
