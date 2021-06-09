package gamesense;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
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
    private static final String VIBRATE_EVENT = "VIBRATE";
    private static final String DISPLAY_EVENT = "DISPLAY";

    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonAdapter<Map<String, Object>> mapAdapter;

    private String engineAddress;

    public GameSenseApi() {
        Moshi moshi = new Moshi.Builder().build();
        Type map = Types.newParameterizedType(Map.class, String.class, Object.class);
        mapAdapter = moshi.adapter(map);
    }

    public void initialise() throws Exception {
        engineAddress = getEngineAddress();
        System.out.println(engineAddress);
    }

    private String getEngineAddress() throws Exception {
        // TODO: exception thrown if file doesn't exist
        String fileContents = Files.readString(
                Paths.get(System.getenv("PROGRAMDATA"), "SteelSeries/SteelSeries Engine 3/coreProps.json"));

        Map<String, Object> result = mapAdapter.fromJson(fileContents);
        return (String) result.get("address");
    }

    public void registerGameAndEvents() throws Exception {
        String registerGameString = mapAdapter.toJson(Map.of(
                "game", ENGINE_GAME_ID,
                "game_display_name", "Tetris Mouse"
        ));
        post("/game_metadata", registerGameString);

        String bindVibrateEvent = mapAdapter.toJson(Map.of(
                "game", ENGINE_GAME_ID,
                "event", VIBRATE_EVENT,
                "value_optional", true,
                "handlers", List.of(
                        Map.of(
                                "device-type", "tactile",
                                "zone", "one",
                                "mode", "vibrate",
                                "pattern", List.of(
                                        // Limit is 5 steps in pattern, with customs counting as 2 steps
                                        // (idfk why the docs say the limit is 140. That's just wrong)
                                        customVibrateStep(200, 200),
                                        vibrateStep("ti_predefined_doubleclick_100", 350),
                                        customVibrateStep(600, 0)
                                )
                        )
                )
        ));
        System.out.println(bindVibrateEvent);
        post("/bind_game_event", bindVibrateEvent);

        String bindDisplayEvent = mapAdapter.toJson(Map.of(
                "game", ENGINE_GAME_ID,
                "event", DISPLAY_EVENT,
                "value_optional", true,
                "handlers", List.of(
                        Map.of(
                                "device-type", "screened-128x36",
                                "zone", "one",
                                "mode", "screen",
                                "datas", List.of(
                                        Map.of(
                                                "has-text", false,
                                                "image-data", new int[(128 * 36) / 8] // Empty so it can be set by the actual events
                                        )
                                )
                        )
                )
        ));
        System.out.println(bindDisplayEvent);
        post("/bind_game_event", bindDisplayEvent);
    }

    public void unregisterGame() throws Exception {
        String removeGameString = mapAdapter.toJson(Map.of("game", ENGINE_GAME_ID));
        post("/remove_game", removeGameString);
    }

    //TODO: hardcode value? Handler currently isn't affected by it
    public void buzz(int n) throws Exception {
        String str = mapAdapter.toJson(Map.of(
                "game", ENGINE_GAME_ID,
                "event", VIBRATE_EVENT,
                "data", Map.of(
                        "value", n
                )
        ));
        System.out.println(str);
        post("/game_event", str);
    }

    private void post(String end, String dataString) throws Exception {
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

    public void showImage(int[] imageData) throws Exception {
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
