package game;

import java.util.Arrays;
import java.util.Random;

class GameCore {

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

    public static final int BOARD_W = 10;
    public static final int BOARD_H = 20;

    private static final int INITIAL_FALL_DELAY = 11;
    private static final int MAX_LEVEL = INITIAL_FALL_DELAY - 1;

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

    public boolean tick(NewUserInput input) {
        boolean renderChange = false;
        if (active == null) {
            boolean canPlace = placeNewActivePiece();
            renderChange = true;

            if (!canPlace) {
                //TODO: end game!
            }
        }

        renderChange |= processUserInput(input);

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

    private boolean processUserInput(NewUserInput input) {
        boolean renderChange = false;

        if (input.leftClicks > 0) {
            for (int i = 0; i < input.leftClicks; i++) {
                if (isLegalMove(active, activeX - 1, activeY)) {
                    activeX--;
                    renderChange = true;
                } else {
                    break;
                }
            }
        }
        if (input.rightClicks > 0) {
            for (int i = 0; i < input.rightClicks; i++) {
                if (isLegalMove(active, activeX + 1, activeY)) {
                    activeX++;
                    renderChange = true;
                } else {
                    break;
                }
            }
        }
        if (input.scrollUps > 0) {
            for (int i = 0; i < input.scrollUps; i++) {
                boolean[][] rotated = rotate(active);
                if (isLegalMove(rotated, activeX, activeY)) {
                    active = rotated;
                    renderChange = true;
                } else {
                    break;
                }
            }
        }
        if (input.scrollDowns > 0) {
            for (int i = 0; i < input.scrollDowns; i++) {
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

    public boolean[][] getBoard() {
        return board;
    }

    public boolean[][] getActivePiece() {
        return active;
    }

    public int getScore() {
        return score;
    }

    public int getActiveX() {
        return activeX;
    }

    public int getActiveY() {
        return activeY;
    }
}
