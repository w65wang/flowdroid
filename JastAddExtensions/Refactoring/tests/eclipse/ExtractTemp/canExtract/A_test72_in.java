package p;

class A {
    void f() {
        StringBuffer buf= new StringBuffer();
        String[] strings= new String[] {"A", "B", "C", "D"};
        for (int i= 0; i < strings.length; i++) {
            buf.append(strings[i]);
        }
    }
}
