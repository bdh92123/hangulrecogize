package recog.common;

import javafx.beans.NamedArg;
import javafx.geometry.Rectangle2D;

/**
 * 사각형 확장 클래스
 * Created by bdh92123 on 2016-12-01.
 */
public class Rectangle extends Rectangle2D {

    /**
     * Creates a new instance of {@code Rectangle2D}.
     *
     * @param minX   The x coordinate of the upper-left corner of the {@code Rectangle2D}
     * @param minY   The y coordinate of the upper-left corner of the {@code Rectangle2D}
     * @param width  The width of the {@code Rectangle2D}
     * @param height The height of the {@code Rectangle2D}
     */
    public Rectangle(@NamedArg("minX") double minX, @NamedArg("minY") double minY, @NamedArg("width") double width, @NamedArg("height") double height) {
        super(minX, minY, width, height);
    }

    public double getIntersectWidth(Rectangle rectangle) {
        if(!intersects(rectangle)) {
            return 0;
        }

        return getIntersectWidthProjection(rectangle);
    }

    public double getIntersectHeight(Rectangle rectangle) {
        if(!intersects(rectangle)) {
            return 0;
        }

        return getIntersectHeightProjection(rectangle);
    }

    public double getIntersectWidthProjection(Rectangle rectangle) {
        if(getMinX() > rectangle.getMinX() && getMaxX() < rectangle.getMaxX()) {
            return getWidth();
        } else if(rectangle.getMinX() > getMinX() && rectangle.getMaxX() < getMaxX()) {
            return rectangle.getWidth();
        } else if(getMaxX() > rectangle.getMinX() && getMaxX() < rectangle.getMaxX()) {
            return getMaxX() - rectangle.getMinX();
        } else if(rectangle.getMaxX() > getMinX() && rectangle.getMaxX() < getMaxX()) {
            return rectangle.getMaxX() - getMinX();
        }

        return 0;
    }

    public double getIntersectHeightProjection(Rectangle rectangle) {
        if(getMinY() > rectangle.getMinY() && getMaxY() < rectangle.getMaxY()) {
            return getHeight();
        } else if(rectangle.getMinY() > getMinY() && rectangle.getMaxY() < getMaxY()) {
            return rectangle.getHeight();
        } else if(getMaxY() > rectangle.getMinY() && getMaxY() < rectangle.getMaxY()) {
            return getMaxY() - rectangle.getMinY();
        } else if(rectangle.getMaxY() > getMinY() && rectangle.getMaxY() < getMaxY()) {
            return rectangle.getMaxY() - getMinY();
        }

        return 0;
    }

}
