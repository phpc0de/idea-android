package p1.p2;

import android.animation.ObjectAnimator;
import android.widget.Button;

@SuppressWarnings("unused")
public class AnimatorTest {

    public void testObjectAnimator(Button button) {
        Object myObject = new MyObject();
        ObjectAnimator animator1 = ObjectAnimator.ofInt(myObject, "prop1", 0, 1, 2, 5);
    }

    private static class MyObject {
        public void <warning>setProp<caret>1(int x)</warning> {
        }
    }
}
