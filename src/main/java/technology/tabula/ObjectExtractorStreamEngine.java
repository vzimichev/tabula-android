package technology.tabula;

import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Region;

import com.tom_roush.pdfbox.contentstream.PDFGraphicsStreamEngine;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import technology.tabula.geom.Line2D;
import technology.tabula.geom.Point2D;
import technology.tabula.geom.Rectangle2D;

class ObjectExtractorStreamEngine extends PDFGraphicsStreamEngine {

    private enum PathOpType { MOVE_TO, LINE_TO, CLOSE, CURVE_TO }

    private static final class PathOp {
        final PathOpType type;
        final float x;
        final float y;

        PathOp(PathOpType type, float x, float y) {
            this.type = type;
            this.x = x;
            this.y = y;
        }
    }

    protected List<Ruling> rulings;
    private final Matrix pageTransform;
    private final Logger logger;
    private boolean extractRulingLines = true;
    private Path.FillType clipWindingRule = null;
    private final Path currentPath = new Path();
    private final List<PathOp> pathOps = new ArrayList<>();
    private PointF currentPoint = new PointF();

    private static final float RULING_MINIMUM_LENGTH = 0.01f;

    protected ObjectExtractorStreamEngine(PDPage page) {
        super(page);
        logger = LoggerFactory.getLogger(ObjectExtractorStreamEngine.class);
        rulings = new ArrayList<>();

        pageTransform = new Matrix();
        PDRectangle pageCropBox = getPage().getCropBox();
        int rotationAngleInDegrees = getPage().getRotation();

        if (Math.abs(rotationAngleInDegrees) == 90 || Math.abs(rotationAngleInDegrees) == 270) {
            pageTransform.setRotate(rotationAngleInDegrees);
        } else {
            pageTransform.setTranslate(0, pageCropBox.getHeight());
        }

        pageTransform.postScale(1f, -1f);
        pageTransform.postTranslate(-pageCropBox.getLowerLeftX(), -pageCropBox.getLowerLeftY());
    }

    @Override
    public void appendRectangle(PointF p0, PointF p1, PointF p2, PointF p3) {
        moveToInternal(p0.x, p0.y);
        lineToInternal(p1.x, p1.y);
        lineToInternal(p2.x, p2.y);
        lineToInternal(p3.x, p3.y);
        closePathInternal();
    }

    @Override
    public void clip(Path.FillType windingRule) {
        clipWindingRule = windingRule;
    }

    @Override
    public void closePath() {
        closePathInternal();
    }

    @Override
    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        currentPath.cubicTo(x1, y1, x2, y2, x3, y3);
        pathOps.add(new PathOp(PathOpType.CURVE_TO, x3, y3));
        currentPoint = new PointF(x3, y3);
    }

    @Override
    public void drawImage(PDImage image) {
    }

    @Override
    public void endPath() throws IOException {
        if (clipWindingRule != null) {
            currentPath.setFillType(clipWindingRule);
            getGraphicsState().intersectClippingPath(currentPath);
            clipWindingRule = null;
        }
        resetPath();
    }

    @Override
    public void fillAndStrokePath(Path.FillType fillRule) {
        strokeOrFillPath();
    }

    @Override
    public void fillPath(Path.FillType fillRule) {
        strokeOrFillPath();
    }

    @Override
    public PointF getCurrentPoint() {
        return new PointF(currentPoint.x, currentPoint.y);
    }

    @Override
    public void lineTo(float x, float y) {
        lineToInternal(x, y);
    }

    @Override
    public void moveTo(float x, float y) {
        moveToInternal(x, y);
    }

    @Override
    public void shadingFill(COSName shadingName) {
    }

    @Override
    public void strokePath() {
        strokeOrFillPath();
    }

    private void moveToInternal(float x, float y) {
        currentPath.moveTo(x, y);
        pathOps.add(new PathOp(PathOpType.MOVE_TO, x, y));
        currentPoint = new PointF(x, y);
    }

    private void lineToInternal(float x, float y) {
        currentPath.lineTo(x, y);
        pathOps.add(new PathOp(PathOpType.LINE_TO, x, y));
        currentPoint = new PointF(x, y);
    }

    private void closePathInternal() {
        currentPath.close();
        pathOps.add(new PathOp(PathOpType.CLOSE, currentPoint.x, currentPoint.y));
    }

    private void strokeOrFillPath() {
        if (!extractRulingLines) {
            resetPath();
            return;
        }

        if (filterPathBySegmentType()) {
            resetPath();
            return;
        }

        Point2D.Float startPoint = null;
        Point2D.Float lastMove = null;
        Point2D.Float endPoint;
        Line2D.Float line;
        PointComparator pointComparator = new PointComparator();

        for (PathOp op : pathOps) {
            switch (op.type) {
                case MOVE_TO:
                    endPoint = transformPoint(op.x, op.y);
                    if (startPoint == null) {
                        startPoint = endPoint;
                    }
                    lastMove = endPoint;
                    startPoint = endPoint;
                    break;
                case LINE_TO:
                    endPoint = transformPoint(op.x, op.y);
                    if (startPoint != null) {
                        line = getLineBetween(startPoint, endPoint, pointComparator);
                        verifyLineIntersectsClipping(line);
                    }
                    startPoint = endPoint;
                    break;
                case CLOSE:
                    if (startPoint != null && lastMove != null) {
                        line = getLineBetween(startPoint, lastMove, pointComparator);
                        verifyLineIntersectsClipping(line);
                        startPoint = lastMove;
                    }
                    break;
                case CURVE_TO:
                    logger.debug("Skipping curve segment in ruling extraction");
                    break;
            }
        }

        resetPath();
    }

    private boolean filterPathBySegmentType() {
        if (pathOps.isEmpty() || pathOps.get(0).type != PathOpType.MOVE_TO) {
            return true;
        }

        for (PathOp op : pathOps) {
            if (op.type != PathOpType.MOVE_TO && op.type != PathOpType.LINE_TO && op.type != PathOpType.CLOSE) {
                return true;
            }
        }
        return false;
    }

    private Point2D.Float transformPoint(float x, float y) {
        float[] points = new float[]{x, y};
        pageTransform.mapPoints(points);
        return new Point2D.Float(Utils.round(points[0], 2), Utils.round(points[1], 2));
    }

    private Line2D.Float getLineBetween(Point2D.Float pointA, Point2D.Float pointB, PointComparator pointComparator) {
        if (pointComparator.compare(pointA, pointB) <= 0) {
            return new Line2D.Float(pointA, pointB);
        }
        return new Line2D.Float(pointB, pointA);
    }

    private void verifyLineIntersectsClipping(Line2D.Float line) {
        Rectangle2D currentClippingPath = currentClippingPath();
        if (line.intersects(currentClippingPath)) {
            Ruling ruling = new Ruling(line.getP1(), line.getP2()).intersect(currentClippingPath);
            if (ruling.length() > RULING_MINIMUM_LENGTH) {
                rulings.add(ruling);
            }
        }
    }

    public Matrix getPageTransform() {
        return pageTransform;
    }

    public Rectangle2D currentClippingPath() {
        Region currentClippingPath = getGraphicsState().getCurrentClippingPath();
        Rect bounds = currentClippingPath.getBounds();
        float[] points = new float[]{bounds.left, bounds.top, bounds.right, bounds.bottom};
        pageTransform.mapPoints(points);
        float left = Math.min(points[0], points[2]);
        float top = Math.min(points[1], points[3]);
        float right = Math.max(points[0], points[2]);
        float bottom = Math.max(points[1], points[3]);
        return new Rectangle2D.Float(left, top, right - left, bottom - top);
    }

    private void resetPath() {
        currentPath.reset();
        pathOps.clear();
    }

    class PointComparator implements Comparator<Point2D> {
        @Override
        public int compare(Point2D p1, Point2D p2) {
            float p1X = Utils.round(p1.getX(), 2);
            float p1Y = Utils.round(p1.getY(), 2);
            float p2X = Utils.round(p2.getX(), 2);
            float p2Y = Utils.round(p2.getY(), 2);

            if (p1Y > p2Y) {
                return 1;
            }
            if (p1Y < p2Y) {
                return -1;
            }
            if (p1X > p2X) {
                return 1;
            }
            if (p1X < p2X) {
                return -1;
            }
            return 0;
        }
    }
}
