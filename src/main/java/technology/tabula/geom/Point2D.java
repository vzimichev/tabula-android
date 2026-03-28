package technology.tabula.geom;

import java.util.Locale;
import java.util.Objects;

public abstract class Point2D implements Cloneable {

    public abstract double getX();

    public abstract double getY();

    public abstract void setLocation(double x, double y);

    public void setLocation(Point2D point) {
        setLocation(point.getX(), point.getY());
    }

    public double distance(Point2D point) {
        return Math.sqrt(distanceSq(point));
    }

    public double distanceSq(Point2D point) {
        double dx = getX() - point.getX();
        double dy = getY() - point.getY();
        return dx * dx + dy * dy;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Point2D)) {
            return false;
        }
        Point2D point = (Point2D) other;
        return java.lang.Double.compare(getX(), point.getX()) == 0 &&
                java.lang.Double.compare(getY(), point.getY()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getX(), getY());
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s[x=%f,y=%f]", getClass().getSimpleName(), getX(), getY());
    }

    public static class Float extends Point2D {
        public float x;
        public float y;

        public Float() {
            this(0f, 0f);
        }

        public Float(float x, float y) {
            this.x = x;
            this.y = y;
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
        public void setLocation(double x, double y) {
            this.x = (float) x;
            this.y = (float) y;
        }
    }

    public static class Double extends Point2D {
        public double x;
        public double y;

        public Double() {
            this(0d, 0d);
        }

        public Double(double x, double y) {
            this.x = x;
            this.y = y;
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
        public void setLocation(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
