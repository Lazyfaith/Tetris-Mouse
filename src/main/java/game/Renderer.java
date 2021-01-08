package game;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.Consumer;
import javax.imageio.ImageIO;

class Renderer {
    static {
        try {
            SCORE_CHAR_MAP = ImageIO.read(GameCore.class.getResource("/char_map_6x12.bmp"));
            GAME_OVER_TEXT = ImageIO.read(GameCore.class.getResource("/game_over.bmp"));
            SCORE_TEXT = ImageIO.read(GameCore.class.getResource("/text_score.bmp"));
            LVL_TEXT = ImageIO.read(GameCore.class.getResource("/text_lvl.bmp"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final BufferedImage SCORE_CHAR_MAP;
    private static final int CHAR_WIDTH = 6;
    private static final int CHAR_HEIGHT = 12;
    private static final BufferedImage GAME_OVER_TEXT;
    private static final BufferedImage SCORE_TEXT;
    private static final BufferedImage LVL_TEXT;

    private static final int DISPLAY_SHORT = 36;
    private static final int DISPLAY_LONG = 128;

    private final GameCore game;
    private final Consumer<int[]> renderOut;

    public Renderer(GameCore activeGame, Consumer<int[]> renderOut) {
        this.game = activeGame;
        this.renderOut = renderOut;
    }

    public void renderGame() {
        // Think of as vertical display (thin & tall)
        boolean[][] pixels = new boolean[DISPLAY_SHORT][DISPLAY_LONG];

        int blockSize = 3;

        // Paint 1 pixel border around the board area
        int bw = 1; // border width
        drawRect(pixels, 0, 0, GameCore.BOARD_W * blockSize + bw * 2, GameCore.BOARD_H * blockSize + bw * 2);

        // Draw the active tiles
        boolean[][] activePiece = game.getActivePiece();
        if (activePiece != null) {
            for (int tx = 0; tx < activePiece.length; tx++) {
                for (int ty = 0; ty < activePiece[0].length; ty++) {
                    if (activePiece[tx][ty]) {
                        int activeX = game.getActiveX();
                        int activeY = game.getActiveY();
                        fillRect(pixels, (activeX + tx) * blockSize + bw, (activeY + ty) * blockSize + bw, blockSize, blockSize);
                    }
                }
            }
        }
        // Draw settled tiles on the board
        boolean[][] board = game.getBoard();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[0].length; y++) {
                if (board[x][y]) {
                    drawRect(pixels, x * blockSize + bw, y * blockSize + bw, blockSize, blockSize);
                }
            }
        }

        // Draw score
        int scoreY = (GameCore.BOARD_H + 2) * blockSize;
        drawImage(pixels, SCORE_TEXT, 0, scoreY);
        drawScore(pixels, scoreY + SCORE_TEXT.getHeight() + 2);

//        debug_printPixels(pixels);

        renderOut.accept(convertPixelsToInts(pixels));
    }

    private static void drawRect(boolean[][] pixels, int x, int y, int w, int h) {
        for (int i = x; i < x + w; i++) {
            pixels[i][y] = true;
            pixels[i][y + h - 1] = true;
        }
        for (int i = y; i < y + h; i++) {
            pixels[x][i] = true;
            pixels[x + w - 1][i] = true;
        }
    }

    private static void fillRect(boolean[][] pixels, int x, int y, int w, int h) {
        for (int ix = x; ix < x + w; ix++) {
            for (int iy = y; iy < y + h; iy++) {
                pixels[ix][iy] = true;
            }
        }
    }

    private void drawScore(boolean[][] pixels, int y) {
        int numToDisplay = Math.min(game.getScore(), 999999);
        char[] numDigits = String.format("%d", numToDisplay).toCharArray();
        // Right up to edge since 6 digits fills the available width
        int x = DISPLAY_SHORT - numDigits.length * CHAR_WIDTH;
        drawNumber(pixels, numDigits, x, y);
    }

    private void drawNumber(boolean[][] pixels, char[] numDigits, int x, int y) {
        for (int i = 0; i < numDigits.length; i++) {
            drawNumDigit(pixels, x + CHAR_WIDTH * i , y, Character.digit(numDigits[i], 10));
        }
    }

    private static void drawNumDigit(boolean[][] pixels, int x, int y, int numDigit) {
        assert 0 <= numDigit && numDigit < 10;
        if (SCORE_CHAR_MAP == null) {
            return;
        }
        BufferedImage charTile = SCORE_CHAR_MAP.getSubimage(numDigit * CHAR_WIDTH, 0, CHAR_WIDTH, CHAR_HEIGHT);
        drawImage(pixels, charTile, x, y);
    }

    private static void drawImage(boolean[][] pixels, BufferedImage img, int x, int y) {
        for (int px = 0; px < img.getWidth(); px++) {
            for (int py = 0; py < img.getHeight(); py++) {
                int col = img.getRGB(px, py);
                if (col == Color.BLACK.getRGB()) {
                    pixels[x + px][y + py] = true;
                }
            }
        }
    }

    private int[] convertPixelsToInts(boolean[][] pixels) {
        int[] render = new int[DISPLAY_LONG * DISPLAY_SHORT / 8];
        for (int x = 0; x < DISPLAY_LONG; x++) {
            for (int y = 0; y < DISPLAY_SHORT; y++) {
                // Rotating the pixel grid 90 anticlockwise to show on horizontal screen (short and wide)
                if (pixels[DISPLAY_SHORT - y - 1][x]) {
                    int bit = x + (y * DISPLAY_LONG);
                    int byteInArr = bit / 8;
                    int bitInByte = bit % 8;
                    render[byteInArr] = render[byteInArr] | 1 << (7 - bitInByte);
                }
            }
        }
        return render;
    }

    public void showGameOverScreen() {
        boolean[][] pixels = new boolean[DISPLAY_SHORT][DISPLAY_LONG];
        int y = 0;
        drawImage(pixels, GAME_OVER_TEXT, 0, y);
        y += GAME_OVER_TEXT.getHeight() + 20;
        drawImage(pixels, SCORE_TEXT, 0, y);
        y += SCORE_TEXT.getHeight() + 2;
        drawScore(pixels, y);
        y += CHAR_HEIGHT + 2;
        drawLevel(pixels, y);

//        debug_printPixels(pixels);

        renderOut.accept(convertPixelsToInts(pixels));
    }

    private void drawLevel(boolean[][] pixels, int y) {
        drawImage(pixels, LVL_TEXT, 0, y);
        char[] scoreDigits = String.format("%d", game.getLevel()).toCharArray();
        int lvlX = DISPLAY_SHORT - 2 - scoreDigits.length * CHAR_WIDTH;
        int lvlY = y + LVL_TEXT.getHeight() + 2;
        drawNumber(pixels, scoreDigits, lvlX, lvlY);
    }

    private void debug_printPixels(boolean[][] pixels) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < DISPLAY_LONG; y++) {
            for (int x = 0; x < DISPLAY_SHORT; x++) {
                sb.append(pixels[x][y] ? "X" : ".");
            }
            sb.append("\n");
        }
        System.out.println("\n\n" + sb + "\n\n");
    }
}
