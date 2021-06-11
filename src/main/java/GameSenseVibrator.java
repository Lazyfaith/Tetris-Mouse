import game.Vibrator;
import gamesense.GameSenseApi;

class GameSenseVibrator implements Vibrator {

    private interface Thrower {
        void call() throws Exception;
    }

    private final GameSenseApi gameSenseApi;

    GameSenseVibrator(GameSenseApi gameSenseApi) {
        this.gameSenseApi = gameSenseApi;
    }

    @Override
    public void doShortBuzz() {
        ignoreExceptions(gameSenseApi::shortVibrate);
    }

    @Override
    public void doGrandBuzz() {
        ignoreExceptions(gameSenseApi::grandVibrate);
    }

    // Just vibration events being lost, so not a huge deal
    // If all events are being lost & the game should stop then a render will happen in a few milliseconds anyway which will throw
    private static void ignoreExceptions(Thrower thrower) {
        try {
            thrower.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
