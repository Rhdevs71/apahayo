
import java.lang.reflect.Method;
public class TestReflection {
    public static class A {
        public void foo(B b) {}
    }
    public static class B {}
    public static class C {}
    public static void main(String[] args) throws Exception {
        Method m = A.class.getMethod("foo", B.class);
        try {
            m.invoke(new C(), new B()); // Wrong receiver
        } catch (IllegalArgumentException e) {
            System.out.println("Wrong receiver: " + e.getMessage());
        }
        try {
            m.invoke(new A(), new C()); // Wrong param
        } catch (IllegalArgumentException e) {
            System.out.println("Wrong param: " + e.getMessage());
        }
    }
}
