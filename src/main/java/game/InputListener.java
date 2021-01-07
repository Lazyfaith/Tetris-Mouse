package game;

import org.jnativehook.GlobalScreen;
import org.jnativehook.mouse.NativeMouseAdapter;
import org.jnativehook.mouse.NativeMouseEvent;
import org.jnativehook.mouse.NativeMouseWheelListener;

class InputListener {

    private final Object leftLock = new Object();
    private final Object rightLock = new Object();
    private final Object wheelScrollLock = new Object();

    private volatile int leftClicks = 0;
    private volatile int rightClicks = 0;
    private volatile int scrollUps = 0;
    private volatile int scrollDowns = 0;

    private boolean listenersAttached = false;

    private final NativeMouseAdapter mouseListener = new NativeMouseAdapter() {
        @Override
        public void nativeMousePressed(NativeMouseEvent nativeMouseEvent) {
            //NB: don't use #getClickClount as that just accumulates endlessly until a different button is clicked
            if (nativeMouseEvent.getButton() == NativeMouseEvent.BUTTON1) {
                synchronized (leftLock) {
                    leftClicks += 1;
                }
            } else if (nativeMouseEvent.getButton() == NativeMouseEvent.BUTTON2) {
                synchronized (rightLock) {
                    rightClicks += 1;
                }
            }
        }
    };
    private final NativeMouseWheelListener wheelListener = nativeMouseWheelEvent -> {
        synchronized (wheelScrollLock) {
            if (nativeMouseWheelEvent.getWheelRotation() < 0) {
                scrollUps += nativeMouseWheelEvent.getWheelRotation() * -1;
            } else {
                scrollDowns += nativeMouseWheelEvent.getWheelRotation();
            }
        }
    };

    void listenToMouseEvents() {
        if (listenersAttached) {
            return;
        }

        // Listener isn't attached so can ignore locks and just rely on volatile status
        leftClicks = 0;
        rightClicks = 0;
        scrollUps = 0;
        scrollDowns = 0;

        GlobalScreen.addNativeMouseListener(mouseListener);
        GlobalScreen.addNativeMouseWheelListener(wheelListener);
        listenersAttached = true;
    }

    void stopListeningToMouseEvents() {
        if (!listenersAttached) {
            return;
        }
        GlobalScreen.removeNativeMouseListener(mouseListener);
        GlobalScreen.removeNativeMouseWheelListener(wheelListener);
        listenersAttached = false;
    }

    NewUserInput getNewInput() {
        return new NewUserInput(
                getNewLeftClicks(),
                getNewRightClicks(),
                getNewScrollUps(),
                getNewScrollDowns()
        );
    }

    private int getNewLeftClicks() {
        synchronized (leftLock) {
            int val = leftClicks;
            leftClicks = 0;
            return val;
        }
    }

    private int getNewRightClicks() {
        synchronized (rightLock) {
            int val = rightClicks;
            rightClicks = 0;
            return val;
        }
    }

    private int getNewScrollUps() {
        synchronized (wheelScrollLock) {
            int val = scrollUps;
            scrollUps = 0;
            return val;
        }
    }

    private int getNewScrollDowns() {
        synchronized (wheelScrollLock) {
            int val = scrollDowns;
            scrollDowns = 0;
            return val;
        }
    }
}
