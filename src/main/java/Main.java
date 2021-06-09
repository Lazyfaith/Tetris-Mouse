import game.GameManager;
import gamesense.GameSenseApi;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;

class Main {

    public static void main(String[] args) throws NativeHookException, IOException, InterruptedException {
        Logger.getLogger(GlobalScreen.class.getPackage().getName()).setLevel(Level.WARNING);
        try {
            GlobalScreen.registerNativeHook();
        }
        catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());

            System.exit(1);
        }

        try {
            new Main().start();
        } finally {
            // To stop its thread from keeping the program alive
            GlobalScreen.unregisterNativeHook();
        }
    }

    private void start() throws IOException, InterruptedException {
        GameSenseApi gameSenseApi = new GameSenseApi();
        gameSenseApi.initialise();
        gameSenseApi.registerGameAndEvents();

        try {
            gameSenseApi.buzz();

            new GameManager(imageData1 -> {
                try {
                    gameSenseApi.showImage(imageData1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).playNewGame();

            gameSenseApi.buzz();
        } catch (Exception ex) {
            throw new RuntimeException("Uncaught error during running game", ex);
        } finally {
            gameSenseApi.unregisterGame();
        }
    }
}
