import game.GameManager;
import gamesense.GameSenseApi;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;

class Main {

    public static void main(String[] args) throws Exception {
        Logger.getLogger(GlobalScreen.class.getPackage().getName()).setLevel(Level.WARNING);
        try {
            GlobalScreen.registerNativeHook();
        }
        catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());

            System.exit(1);
        }

        new Main().start();

        // To stop its thread from keeping the program alive
        GlobalScreen.unregisterNativeHook();
    }

    private void start() throws Exception {
        GameSenseApi gameSenseApi = new GameSenseApi();
        gameSenseApi.initialise();
        gameSenseApi.registerGameAndEvents();

        gameSenseApi.buzz(100);;

        try {
            new GameManager(imageData1 -> {
                try {
                    gameSenseApi.showImage(imageData1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).playNewGame();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        gameSenseApi.unregisterGame();
    }
}
