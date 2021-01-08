package game;

import java.util.function.Consumer;

public class GameManager {
    private static final int TICKS_PER_SECOND = 15;

    private final Consumer<int[]> renderOut;
    private final InputListener inputListener = new InputListener();

    public GameManager(Consumer<int[]> renderOut) {
        this.renderOut = renderOut;
    }

    public void playNewGame() {
        GameCore activeGame = new GameCore();
        inputListener.listenToMouseEvents();
        try {
            runGame(activeGame);
        } finally {
            inputListener.stopListeningToMouseEvents();
        }
    }

    private void runGame(GameCore activeGame) {
        long startTime = System.currentTimeMillis();
        long tick = 0;
        int tickTime = 1000 / TICKS_PER_SECOND; // Roughly

        Renderer renderer = new Renderer(activeGame, renderOut);

        while (true) {
            long now = System.currentTimeMillis();

            if (now >= startTime + (tick * tickTime)) {
                TickResult result = activeGame.tick(inputListener.getNewInput());
                if (result == TickResult.GAME_OVER) {
                    break;
                }
                if (result == TickResult.VISUAL_CHANGE || tick == 0) {
                    renderer.renderGame();
                }
                tick++;
            } else {
                try {
                    Thread.sleep(tickTime / 5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        renderer.showGameOverScreen();
    }
}
