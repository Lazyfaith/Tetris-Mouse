package gamesense;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class GameSenseApi {
    private static final String ENGINE_GAME_ID = "TETRIS_MOUSE";
    private static final String SHORT_VIBRATE_EVENT = "SHORT_VIBRATE";
    private static final String LONG_VIBRATE_EVENT = "LONG_VIBRATE";
    private static final String GRAND_VIBRATE_EVENT = "GRAND_VIBRATE";
    private static final String DISPLAY_EVENT = "DISPLAY";

    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonAdapter<Map<String, Object>> mapAdapter;

    private String engineAddress;

    public GameSenseApi() {
        Moshi moshi = new Moshi.Builder().build();
        Type map = Types.newParameterizedType(Map.class, String.class, Object.class);
        mapAdapter = moshi.adapter(map);
    }

    public void initialise() throws IOException {
        try {
            engineAddress = getEngineAddress();
        } catch (IOException e) {
            throw new IOException("Could not get Game Sense engine address", e);
        }
        System.out.println(engineAddress);
    }

    private String getEngineAddress() throws IOException {
        // TODO: exception thrown if file doesn't exist
        String fileContents = Files.readString(
                Paths.get(System.getenv("PROGRAMDATA"), "SteelSeries/SteelSeries Engine 3/coreProps.json"));

        Map<String, Object> result = mapAdapter.fromJson(fileContents);
        return (String) result.get("address");
    }

    public void registerGameAndEvents() throws IOException, InterruptedException {
        String registerGameString = mapAdapter.toJson(Map.of(
                "game", ENGINE_GAME_ID,
                "game_display_name", "Tetris Mouse"
        ));
        post("/game_metadata", registerGameString);

        bindVibrationGameEvent(SHORT_VIBRATE_EVENT, List.of(
                customVibrateStep(200, 0)
        ));
        bindVibrationGameEvent(LONG_VIBRATE_EVENT, List.of(
                customVibrateStep(600, 0)
        ));
        bindVibrationGameEvent(GRAND_VIBRATE_EVENT, List.of(
                customVibrateStep(200, 200),
                vibrateStep("ti_predefined_doubleclick_100", 350),
                customVibrateStep(600, 0)
        ));

        bindGameEvent(DISPLAY_EVENT, Map.of(
                "device-type", "screened-128x36",
                "zone", "one",
                "mode", "screen",
                "datas", List.of(
                        Map.of(
                                "has-text", false,
                                "image-data", new int[(128 * 36) / 8] // Empty so it can be set by the actual events
                        )
                )
        ));
    }

    // Limit is 5 steps in pattern, with customs counting as 2 steps
    // (idfk why the docs say the limit is 140. That's just wrong)
    private void bindVibrationGameEvent(String eventName, List<Map<String, Object>> vibrationPattern) throws IOException, InterruptedException {
        bindGameEvent(eventName, Map.of(
                "device-type", "tactile",
                "zone", "one",
                "mode", "vibrate",
                "pattern", vibrationPattern
        ));
    }

    private void bindGameEvent(String eventName, Map<String, Object> handler) throws IOException, InterruptedException {
        String bindEvent = mapAdapter.toJson(Map.of(
                "game", ENGINE_GAME_ID,
                "event", eventName,
                "value_optional", true,
                "handlers", List.of(handler)
        ));
        System.out.println(bindEvent);
        post("/bind_game_event", bindEvent);
    }

    public void unregisterGame() throws IOException, InterruptedException {
        String removeGameString = mapAdapter.toJson(Map.of("game", ENGINE_GAME_ID));
        post("/remove_game", removeGameString);
    }

    public void shortVibrate() throws IOException, InterruptedException {
        postVibrationEvent(SHORT_VIBRATE_EVENT);
    }

    public void longVibrate() throws IOException, InterruptedException {
        postVibrationEvent(LONG_VIBRATE_EVENT);
    }

    public void grandVibrate() throws IOException, InterruptedException {
        postVibrationEvent(GRAND_VIBRATE_EVENT);
    }

    private void postVibrationEvent(String eventName) throws IOException, InterruptedException {
        String str = mapAdapter.toJson(Map.of(
                "game", ENGINE_GAME_ID,
                "event", eventName,
                "data", Map.of(
                        "value", 100
                )
        ));
        System.out.println(str);
        post("/game_event", str);
    }

    private void post(String end, String dataString) throws IOException, InterruptedException {
        HttpRequest registerGame = buildPostRequest(end, dataString);
        var response = client.send(registerGame, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.statusCode() + "\t" + response.uri());
        if (response.statusCode() != 200) {
            System.out.println("\t" + response.body() + "\n\n");
        }
    }

    private HttpRequest buildPostRequest(String end, String dataString) {
        return HttpRequest.newBuilder()
                .uri(URI.create("http://" + engineAddress + end))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(dataString))
                .build();
    }

    private Map<String, Object> vibrateStep(String type, int delayAfter) {
        return Map.of(
                "type", type,
                "delay-ms", delayAfter
        );
    }

    private Map<String, Object> customVibrateStep(int length, int delayAfter) {
        return Map.of(
                "type", "custom",
                "length-ms", length,
                "delay-ms", delayAfter
        );
    }

    public void showImage(int[] imageData) throws IOException, InterruptedException {
        post("/game_event", mapAdapter.toJson(Map.of(
                "game", ENGINE_GAME_ID,
                "event", DISPLAY_EVENT,
                "data", Map.of(
                        "value", 100,
                        "frame", Map.of(
                                // My mouse (Rival 700) is 128x36
                                "image-data-128x36", imageData
                        )
                )
        )));
    }
}
