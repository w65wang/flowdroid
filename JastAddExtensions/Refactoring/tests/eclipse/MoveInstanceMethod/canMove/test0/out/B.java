package p2;

///import p1.A;

public class B {
	public void mB1() {}
	
	public void mB2() {}

	/**
	 * @param a TODO
	 */
        /// public void mA1(A a) {
        public void mA1(p1.A a) {
   	        this.mB1(); /// mB1();
		a.mA2(); //test comment
		this.mB2(); /// mB2();
		System.out.println(a);
	}
}