package aditya.rupal.translate.orcandtext.data;

public class Coords {
    float x, y;

    public Coords(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public boolean inLine(Coords newXY) {
        int diff = (int) Math.abs(newXY.y - this.y);

        if (diff >= 0 && diff <= 13)
            return true;
        else
            return false;
    }

    public boolean inFront(Coords newXY) {
        if (newXY.x < this.x)
            return true;
        else
            return false;
    }

    public Coords mean(Coords newXY) {
        int newx = (int) ((this.x + newXY.x) / 2);
        int newy = (int) ((this.y + newXY.y) / 2);
        return new Coords(newx,newy);
    }
}
