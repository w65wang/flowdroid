package p;

public class TestRecursiveSimpleReordered {
	public static class FooParameter {
		public int x;
		public int y;
		public FooParameter(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	public void foo(FooParameter parameterObject) {
		int x = parameterObject.x;
		int y = parameterObject.y;
		if (/*///parameterObject.*/x < 0)
			foo(new FooParameter(x, y)); ///parameterObject);
		foo(new FooParameter(/*///parameterObject.*/y, /*///parameterObject.*/x));
	}
}
