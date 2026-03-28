package technology.tabula.geom;

import java.util.Locale;
import java.util.Objects;

public abstract class Rectangle2D implements Cloneable {

    public abstract double getX();

    public abstract double getY();

    public abstract double getWidth();

    public abstract double getHeight();

    public abstract void setRect(double x, double y, double width, double height);

    public void setRect(Rectangle2D rectangle) {
        setRect(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
    }

    public double getMinX() {
        return getX();
    }

    public double getMinY() {
        return getY();
    }

    public double getMaxX() {
        return getX() + getWidth();
    }

    public double getMaxY() {
        return getY() + getHeight();
    }

    public boolean contains(Point2D point) {
        return contains(point.getX(), point.getY());
    }

    public boolean contains(Rectangle2D rectangle) {
        return contains(rectangle.getMinX(), rectangle.getMinY()) &&
                contains(rectangle.getMaxX(), rectangle.getMaxY());
    }

    public boolean contains(double x, double y) {
        return x >= getMinX() && x <= getMaxX() && y >= getMinY() && y <= getMaxY();
    }

    public boolean intersects(Rectangle2D rectangle) {
        return rectangle.getMaxX() >= getMinX() &&
                rectangle.getMinX() <= getMaxX() &&
                rectangle.getMaxY() >= getMinY() &&
                rectangle.getMinY() <= getMaxY();
    }

    public boolean intersectsLine(Line2D line) {
        if (contains(line.getP1()) || contains(line.getP2())) {
            return true;
        }

        Line2D.Float top = new Line2D.Float((float) getMinX(), (float) getMinY(), (float) getMaxX(), (float) getMinY());
        Line2D.Float bottom = new Line2D.Float((float) getMinX(), (float) getMaxY(), (float) getMaxX(), (float) getMaxY());
        Line2D.Float left = new Line2D.Float((float) getMinX(), (float) getMinY(), (float) getMinX(), (float) getMaxY());
        Line2D.Float right = new Line2D.Float((float) getMaxX(), (float) getMinY(), (float) getMaxX(), (float) getMaxY());
        return line.intersectsLine(top) || line.intersectsLine(bottom) || line.intersectsLine(left) || line.intersectsLine(right);
    }

    public Rectangle2D createUnion(Rectangle2D rectangle) {
        return new Float(
                (float) Math.min(getMinX(), rectangle.getMinX()),
                (float) Math.min(getMinY(), rectangle.getMinY()),
                (float) (Math.max(getMaxX(), rectangle.getMaxX()) - Math.min(getMinX(), rectangle.getMinX())),
                (float) (Math.max(getMaxY(), rectangle.getMaxY()) - Math.min(getMinY(), rectangle.getMinY()))
        );
    }

    public Rectangle2D getBounds2D() {
        return new Float((float) getX(), (float) getY(), (float) getWidth(), (float) getHeight());
    }

    public static void union(Rectangle2D src1, Rectangle2D src2, Rectangle2D dest) {
        dest.setRect(
                Math.min(src1.getMinX(), src2.getMinX()),
                Math.min(src1.getMinY(), src2.getMinY()),
                Math.max(src1.getMaxX(), src2.getMaxX()) - Math.min(src1.getMinX(), src2.getMinX()),
                Math.max(src1.getMaxY(), src2.getMaxY()) - Math.min(src1.getMinY(), src2.getMinY())
        );
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Rectangle2D)) {
            return false;
        }
        Rectangle2D rectangle = (Rectangle2D) other;
        return Double.compare(getX(), rectangle.getX()) == 0 &&
                Double.compare(getY(), rectangle.getY()) == 0 &&
                Double.compare(getWidth(), rectangle.getWidth()) == 0 &&
                Double.compare(getHeight(), rectangle.getHeight()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getX(), getY(), getWidth(), getHeight());
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s[x=%f,y=%f,w=%f,h=%f]",
                getClass().getSimpleName(), getX(), getY(), getWidth(), getHeight());
    }

    public static class Float extends Rectangle2D {
        public float x;
        public float y;
        public float width;
        public float height;

        public Float() {
            this(0f, 0f, 0f, 0f);
        }

        public Float(float x, float y, float width, float height) {
            setRect(x, y, width, height);
        }

        @Override
        public double getX() {
            return x;
        }

        @Override
        public double getY() {
            return y;
        }

        @Override
        public double getWidth() {
            return width;
        }

        @Override
        public double getHeight() {
            return height;
        }

        @Override
        public void setRect(double x, double y, double width, double height) {
            this.x = (float) x;
            this.y = (float) y;
            this.width = (float) width;
            this.height = (float) height;
        }
    }
}
