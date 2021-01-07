package game;

class NewUserInput {
    final int leftClicks;
    final int rightClicks;
    final int scrollUps;
    final int scrollDowns;

    NewUserInput(int leftClicks, int rightClicks, int scrollUps, int scrollDowns) {
        this.leftClicks = leftClicks;
        this.rightClicks = rightClicks;
        this.scrollUps = scrollUps;
        this.scrollDowns = scrollDowns;
    }
}
