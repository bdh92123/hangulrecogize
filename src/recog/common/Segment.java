package recog.common;

/**
 * 한글 입력 획의 벡터정보
 * Created by Baek on 2016-10-07.
 */
public class Segment implements Cloneable {
    private boolean circle;
    private double x;
    private double y;
    private double dx;
    private double dy;

    public char getDirection() {
        if(circle) {
            return '9';
        }

        double d;
        double tempDx = dx, tempDy = dy;

        // y축과 평행인 입력의 경우는 기울기를 임의의 큰 값으로 정함
        if (tempDx == 0){
            tempDx = 1;
            if (tempDy > 0) d = 10.0f;
            else d = -10.0f;
        } else d = (double)tempDy / tempDx;


        // 영역에 따른 값을 리턴
        if (tempDx > 0)
        {
            if (d > 2.4142) return '5'; // tan(3*pi/8)
            else if (d < -2.4142) return '1'; // -tan(3*pi/8)
            else if (d > 0.4142) return '6'; // tan(pi/8)
            else if (d < -0.4142) return '8'; // -tan(pi/8)
            else return '7';
        }
        else
        {
            if (d > 2.4142) return '1'; // 67.5
            else if (d < -2.4142) return '5'; // -tan(3*pi/8)
            else if (d > 0.4142) return '2'; // tan(pi/8)
            else if (d < -0.4142) return '4'; // -tan(pi/8)
            else return '3';
        }
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getDx() {
        return dx;
    }

    public void setDx(double dx) {
        this.dx = dx;
    }

    public double getDy() {
        return dy;
    }

    public void setDy(double dy) {
        this.dy = dy;
    }

    public double getEndX() {
        return x + dx;
    }

    public double getEndY() {
        return y + dy;
    }

    public double getCenterX() { return x + dx / 2d;}

    public double getCenterY() { return y + dy / 2d;}

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void setCircle(boolean circle) {
        this.circle = circle;
    }

    public boolean isCircle() {
        return circle;
    }

    @Override
    public boolean equals(Object obj) {
        Segment segment = (Segment) obj;
        return (x == segment.getX() && y == segment.getY() && dx == segment.getDx() && dy == segment.getDy());
    }

    public String toString() {
        return String.format("[x=%.2f, y=%.2f, dx=%.2f, dy=%.2f]\n", x, y, dx, dy);
    }
}
