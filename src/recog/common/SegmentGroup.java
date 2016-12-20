package recog.common;

import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Baek on 2016-10-07.
 */
public class SegmentGroup implements Cloneable {
    private List<Segment> segments = new ArrayList<>();

    public SegmentGroup() {
    }

    public SegmentGroup(List<Segment> segments) {
        this.segments.addAll(segments);
    }

    public List<Segment> getSegments() {
        return segments;
    }

    public boolean equals(Object obj) {
        SegmentGroup segmentGroup = (SegmentGroup) obj;

        if(segments.size() != segmentGroup.getSegments().size()) {
            return false;
        }

        for(int i=0; i<segments.size(); i++) {
            if(!segments.get(i).equals(segmentGroup.getSegments().get(i))) {
                return false;
            }
        }

        return true;
    }

    public SegmentGroup subGroup(int from, int to) {
        SegmentGroup segmentGroup = new SegmentGroup();
        for(int i = from; i < to; i++) {
            segmentGroup.getSegments().add(segments.get(i));
        }

        return segmentGroup;
    }

    public Point2D getCenter() {
        int centerX = 0, centerY = 0;
        int nullCount = 0;

        for(Segment segment : segments) {
            if(segment == null) {
                nullCount++;
                continue;
            }
            centerX += segment.getCenterX();
            centerY += segment.getCenterY();
        }

        centerX /= (segments.size() - nullCount);
        centerY /= (segments.size() - nullCount);

        return new Point2D(centerX, centerY);
    }

    public Rectangle getRect() {
        double left = 0x7fffffff, top = 0x7fffffff, right = 0, bottom = 0;

        for(Segment segment : segments) {
            if(segment == null) {
                continue;
            }
            double segmentLeft = Math.min(segment.getX(), segment.getEndX());
            double segmentTop = Math.min(segment.getY(), segment.getEndY());
            double segmentRight = Math.max(segment.getX(), segment.getEndX());
            double segmentBottom = Math.max(segment.getY(), segment.getEndY());

            if(segmentLeft < left) {
                left = segmentLeft;
            }
            if(segmentTop < top) {
                top = segmentTop;
            }
            if(segmentRight > right) {
                right = segmentRight;
            }
            if(segmentBottom > bottom) {
                bottom = segmentBottom;
            }
        }

        Rectangle rect = new Rectangle(left, top, right - left, bottom - top);
        return rect;
    }

    public SegmentGroup expand(SegmentGroup segmentGroup) {
        SegmentGroup newSegmentGroup = new SegmentGroup(this.getSegments());
        newSegmentGroup.getSegments().addAll(segmentGroup.getSegments());
        return newSegmentGroup;
    }

    public String toChainCode() {
        StringBuffer buffer = new StringBuffer();
        for(Segment segment : segments) {
            if(segment == null) {
                buffer.append('&');
            } else {
                buffer.append(segment.getDirection());
            }
        }
        return buffer.toString();
    }

    public int size() {
        return segments.size();
    }

    public String toCoordString() {
        StringBuffer coordStringBuffer = new StringBuffer();
        Segment prevSegment = null;
        for(int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            if(segment == null) {
                if(prevSegment != null) {
                    coordStringBuffer.append(String.format(" (%d,%d)", (int) prevSegment.getEndX(), (int) prevSegment.getEndY()));
                }
                coordStringBuffer.append(" (-1,-1)");
                continue;
            }

            coordStringBuffer.append(String.format(" (%d,%d)", (int) segment.getX(), (int) segment.getY()));
            if(i == segments.size() - 1 && segment != null) {
                coordStringBuffer.append(String.format(" (%d,%d)", (int) segment.getEndX(), (int) segment.getEndY()));
            }

            prevSegment = segment;
        }

        coordStringBuffer.append(" (-1,-1)");
        return coordStringBuffer.toString();
    }
}
