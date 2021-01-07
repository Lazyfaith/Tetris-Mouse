import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;
import javax.imageio.ImageIO;

class Game {

    static {
        try {
            SCORE_CHAR_MAP = ImageIO.read(Game.class.getResource("/char_map_6x12.bmp"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final BufferedImage SCORE_CHAR_MAP;
    private static final int CHAR_WIDTH = 6;
    private static final int CHAR_HEIGHT = 12;

    // First pair are the rotate space of the shape, subsequent pairs are the initial tiles positions in the space
    private static final int [][][] PIECES = {
            // Line
            { {4, 4}, {0, 1}, {1, 1}, {2, 1}, {3, 1} },
            // Square
            { {2, 2}, {0, 0}, {1, 0}, {0, 1}, {1, 1} },
            // Ls
            { {3, 3}, {0, 0}, {0, 1}, {1, 1}, {2, 1} },
            { {3, 3}, {0, 1}, {1, 1}, {2, 1}, {0, 2} },
            // Zs
            { {3, 3}, {0, 1}, {1, 1}, {1, 0}, {2, 0} },
            { {3, 3}, {0, 0}, {1, 0}, {1, 1}, {2, 1} },
            // T
            { {3, 3}, {1, 0}, {0, 1}, {1, 1}, {2, 1} }
    };

    private static final int DISPLAY_SHORT = 36;
    private static final int DISPLAY_LONG = 128;
    private static final int TICKS_PER_SECOND = 15;
    private static final int BOARD_W = 10;
    private static final int BOARD_H = 20;
    private static final int INITIAL_FALL_DELAY = 11;
    private static final int MAX_LEVEL = INITIAL_FALL_DELAY - 1;

    private final InputListener input = new InputListener();
    private final Random rng = new Random();

    // [0, 0] is top left corner
    private boolean[][] board = new boolean[BOARD_W][BOARD_H];
    private boolean[][] active = null;
    private int activeX;
    private int activeY;

    private int fallDelay = INITIAL_FALL_DELAY;
    private int score = 0;
    private int level = 0;
    private int rowsCleared = 0;
    private int rowsSoftDropped = 0;

    public void run(Consumer<int[]> renderOut) {
        input.listenToMouseEvents();

        try {
            startTickLoop(renderOut);
        } finally {
            input.stopListeningToMouseEvents();
        }
    }

    private void startTickLoop(Consumer<int[]> renderOut) {
        long startTime = System.currentTimeMillis();
        long tick = 0;
        int tickTime = 1000 / TICKS_PER_SECOND; // Roughly
        while (true) {
            long now = System.currentTimeMillis();

            if (now >= startTime + (tick * tickTime)) {
                boolean renderChange = tick();
                if (renderChange || tick == 0) {
                    render(renderOut);
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
    }

    private boolean tick() {
        boolean renderChange = false;
        if (active == null) {
            boolean canPlace = placeNewActivePiece();
            renderChange = true;

            if (!canPlace) {
                //TODO: end game!
            }
        }

        renderChange |= processUserInput();

        fallDelay--;
        if (fallDelay > 0) {
            return renderChange;
        }

        boolean pieceLands = !isLegalMove(active, activeX, activeY + 1);
        if (pieceLands) {
            for (int tx = 0; tx < active.length; tx++) {
                for (int ty = 0; ty < active[0].length; ty++) {
                    if (active[tx][ty]) {
                        board[activeX + tx][activeY + ty] = true;
                    }
                }
            }
            active = null;

            int rowsRemoved = clearFullRowsFromBoard();
            rowsCleared += rowsRemoved;
            score += baseScoreForRowsRemoved(rowsRemoved) * (level + 1) + rowsSoftDropped;
            level = Math.min(rowsCleared / 10, MAX_LEVEL);
            rowsSoftDropped = 0;
        } else {
            activeY++;
        }
        fallDelay = currentFallDelay();
        return true;
    }

    private boolean placeNewActivePiece() {
        assert active == null;

        int[][] pieceData = PIECES[rng.nextInt(PIECES.length)];
        // If top row(s) of active piece space are empty, then initial Y value must be higher to place at top of board
        int initialY = Arrays.stream(pieceData)
                .map(t -> t[1])
                .min(Integer::compareTo)
                .map(i -> -i)
                .get();
        active = new boolean[pieceData[0][0]][pieceData[0][1]];
        for (int i = 1; i < pieceData.length; i++) {
            int[] t = pieceData[i];
            active[t[0]][t[1]] = true;
        }
        activeX = 3;
        activeY = initialY;
        return isLegalMove(active, activeX, activeY);
    }

    private int currentFallDelay() {
        return INITIAL_FALL_DELAY - level;
    }

    private boolean isLegalMove(boolean[][] tileSpace, int sx, int sy) {
        for (int tx = 0; tx < tileSpace.length; tx++) {
            for (int ty = 0; ty < tileSpace[0].length; ty++) {
                if (!tileSpace[tx][ty]) {
                    // No tile
                    continue;
                }
                if (sx + tx < 0 || sx + tx >= BOARD_W) {
                    // Horizontally out of bounds
                    return false;
                } else if (sy + ty < 0 || sy + ty >= BOARD_H) {
                    // Vertically out of bounds
                    return false;
                } else if (board[sx + tx][sy + ty]) {
                    // Colliding with landed tile
                    return false;
                }
            }
        }
        return true;
    }

    private boolean processUserInput() {
        boolean renderChange = false;

        int leftClicks = input.getNewLeftClicks();
        if (leftClicks > 0) {
            for (int i = 0; i < leftClicks; i++) {
                if (isLegalMove(active, activeX - 1, activeY)) {
                    activeX--;
                    renderChange = true;
                } else {
                    break;
                }
            }
        }
        int rightClicks = input.getNewRightClicks();
        if (rightClicks > 0) {
            for (int i = 0; i < rightClicks; i++) {
                if (isLegalMove(active, activeX + 1, activeY)) {
                    activeX++;
                    renderChange = true;
                } else {
                    break;
                }
            }
        }
        int scrollUps = input.getNewScrollUps();
        if (scrollUps > 0) {
            for (int i = 0; i < scrollUps; i++) {
                boolean[][] rotated = rotate(active);
                if (isLegalMove(rotated, activeX, activeY)) {
                    active = rotated;
                    renderChange = true;
                } else {
                    break;
                }
            }
        }
        int scrollDowns = input.getNewScrollDowns();
        if (scrollDowns > 0) {
            for (int i = 0; i < scrollDowns; i++) {
                if (isLegalMove(active, activeX, activeY + 1)) {
                    activeY++;
                    rowsSoftDropped++;
                    renderChange = true;
                } else {
                    break;
                }
            }
        }
        return renderChange;
    }

    /** Rotates tiles 90 degrees clockwise in the given piece's space. */
    private static boolean[][] rotate(boolean[][] given) {
        assert given.length == given[0].length;
        boolean[][] result = new boolean[given.length][given.length];
        for (int x = 0; x < given.length; x++) {
            for (int y = 0; y < given[0].length; y++) {
                result[x][y] = given[y][given.length - x - 1];
            }
        }
        return result;
    }

    private int clearFullRowsFromBoard() {
        boolean[][] newBoard = new boolean[BOARD_W][BOARD_H];

        int readRow = BOARD_H - 1;
        int writeRow = readRow;

        while (readRow >= 0) {
            boolean full = true;
            for (int x = 0; x < BOARD_W; x++) {
                if (!board[x][readRow]) {
                    full = false;
                    break;
                }
            }
            if (full) {
                readRow--;
                continue;
            }
            for (int x = 0; x < BOARD_W; x++) {
                newBoard[x][writeRow] = board[x][readRow];
            }
            readRow--;
            writeRow--;
        }

        board = newBoard;
        return writeRow - readRow;
    }

    private int baseScoreForRowsRemoved(int numRemoved) {
        switch (numRemoved) {
            case 0: return 0;
            case 1: return 40;
            case 2: return 100;
            case 3: return 300;
            case 4: return 1200;
        }
        throw new AssertionError("Unexpected number of rows removed: " + numRemoved);
    }

    private void render(Consumer<int[]> renderOut) {
        // Think of as vertical display (thin & tall)
        boolean[][] pixels = new boolean[DISPLAY_SHORT][DISPLAY_LONG];

        int blockSize = 3;

        // Paint 1 pixel border around the board area
        int bw = 1; // border width
        drawRect(pixels, 0, 0, BOARD_W * blockSize + bw * 2, BOARD_H * blockSize + bw * 2);

        // Draw the active tiles
        if (active != null) {
            for (int tx = 0; tx < active.length; tx++) {
                for (int ty = 0; ty < active[0].length; ty++) {
                    if (active[tx][ty]) {
                        fillRect(pixels, (activeX + tx) * blockSize + bw, (activeY + ty) * blockSize + bw, blockSize, blockSize);
                    }
                }
            }
        }
        // Draw settled tiles on the board
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[0].length; y++) {
                if (board[x][y]) {
                    drawRect(pixels, x * blockSize + bw, y * blockSize + bw, blockSize, blockSize);
                }
            }
        }

        // Draw score
        int scoreToDraw = Math.min(score, 999999);
        int charsY = (BOARD_H + 2) * blockSize;
        char[] scoreDigits = String.format("%06d", scoreToDraw).toCharArray();
        for (int i = 0; i < 6; i++) {
            drawNum(pixels, CHAR_WIDTH * i, charsY, Character.digit(scoreDigits[i], 10));
        }

//        debug_printPixels(pixels);

        renderOut.accept(convertPixelsToInts(pixels));
    }

    private static void drawRect(boolean[][] arr, int x, int y, int w, int h) {
        for (int i = x; i < x + w; i++) {
            arr[i][y] = true;
            arr[i][y + h - 1] = true;
        }
        for (int i = y; i < y + h; i++) {
            arr[x][i] = true;
            arr[x + w - 1][i] = true;
        }
    }

    private static void fillRect(boolean[][] arr, int x, int y, int w, int h) {
        for (int ix = x; ix < x + w; ix++) {
            for (int iy = y; iy < y + h; iy++) {
                arr[ix][iy] = true;
            }
        }
    }

    private static void drawNum(boolean[][] arr, int x, int y, int numDigit) {
        assert 0 <= numDigit && numDigit < 10;
        if (SCORE_CHAR_MAP == null) {
            return;
        }

        for (int cx = 0; cx < CHAR_WIDTH; cx++) {
            for (int cy = 0; cy < CHAR_HEIGHT; cy++) {
                int col = SCORE_CHAR_MAP.getRGB(numDigit * CHAR_WIDTH + cx, cy);
                if (col == Color.BLACK.getRGB()) {
                    arr[x + cx][y + cy] = true;
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
