package com.clockmods.ui;

import android.graphics.Path;

/**
 * Parses SVG / Android VectorDrawable path data (the {@code pathData} attribute)
 * into an {@link android.graphics.Path}. Supports the subset of commands emitted
 * by Google Material Symbols exports: M/m, L/l, H/h, V/v, C/c, S/s, Q/q, T/t,
 * A/a and Z/z.
 *
 * <p>Parsing manually keeps the Material Symbols glyphs rendering identically on
 * API 14+ without depending on runtime VectorDrawable inflation.</p>
 */
final class SvgPath {

    private SvgPath() {
    }

    static Path parse(String data) {
        Path path = new Path();
        if (data == null || data.length() == 0) {
            return path;
        }

        int length = data.length();
        int index = 0;
        char command = 0;
        float currentX = 0f;
        float currentY = 0f;
        float startX = 0f;
        float startY = 0f;
        float lastControlX = 0f;
        float lastControlY = 0f;
        char lastCommand = 0;

        float[] args = new float[8];

        while (index < length) {
            char c = data.charAt(index);
            if (isWhitespaceOrComma(c)) {
                index++;
                continue;
            }
            if (isCommand(c)) {
                command = c;
                index++;
            } else if (command == 0) {
                index++;
                continue;
            }

            int argCount = argCountFor(command);
            int parsed = 0;
            while (parsed < argCount) {
                index = skipSeparators(data, index, length);
                if (index >= length) {
                    break;
                }
                int[] end = new int[1];
                args[parsed] = readNumber(data, index, length, end);
                index = end[0];
                parsed++;
            }
            if (parsed < argCount) {
                break;
            }

            boolean relative = Character.isLowerCase(command);
            char upper = Character.toUpperCase(command);
            switch (upper) {
                case 'M': {
                    currentX = relative ? currentX + args[0] : args[0];
                    currentY = relative ? currentY + args[1] : args[1];
                    path.moveTo(currentX, currentY);
                    startX = currentX;
                    startY = currentY;
                    // Subsequent implicit pairs are treated as line-to.
                    command = relative ? 'l' : 'L';
                    break;
                }
                case 'L': {
                    currentX = relative ? currentX + args[0] : args[0];
                    currentY = relative ? currentY + args[1] : args[1];
                    path.lineTo(currentX, currentY);
                    break;
                }
                case 'H': {
                    currentX = relative ? currentX + args[0] : args[0];
                    path.lineTo(currentX, currentY);
                    break;
                }
                case 'V': {
                    currentY = relative ? currentY + args[0] : args[0];
                    path.lineTo(currentX, currentY);
                    break;
                }
                case 'C': {
                    float x1 = relative ? currentX + args[0] : args[0];
                    float y1 = relative ? currentY + args[1] : args[1];
                    float x2 = relative ? currentX + args[2] : args[2];
                    float y2 = relative ? currentY + args[3] : args[3];
                    float x = relative ? currentX + args[4] : args[4];
                    float y = relative ? currentY + args[5] : args[5];
                    path.cubicTo(x1, y1, x2, y2, x, y);
                    lastControlX = x2;
                    lastControlY = y2;
                    currentX = x;
                    currentY = y;
                    break;
                }
                case 'S': {
                    float x1;
                    float y1;
                    if (lastCommand == 'C' || lastCommand == 'S') {
                        x1 = 2 * currentX - lastControlX;
                        y1 = 2 * currentY - lastControlY;
                    } else {
                        x1 = currentX;
                        y1 = currentY;
                    }
                    float x2 = relative ? currentX + args[0] : args[0];
                    float y2 = relative ? currentY + args[1] : args[1];
                    float x = relative ? currentX + args[2] : args[2];
                    float y = relative ? currentY + args[3] : args[3];
                    path.cubicTo(x1, y1, x2, y2, x, y);
                    lastControlX = x2;
                    lastControlY = y2;
                    currentX = x;
                    currentY = y;
                    break;
                }
                case 'Q': {
                    float x1 = relative ? currentX + args[0] : args[0];
                    float y1 = relative ? currentY + args[1] : args[1];
                    float x = relative ? currentX + args[2] : args[2];
                    float y = relative ? currentY + args[3] : args[3];
                    path.quadTo(x1, y1, x, y);
                    lastControlX = x1;
                    lastControlY = y1;
                    currentX = x;
                    currentY = y;
                    break;
                }
                case 'T': {
                    float x1;
                    float y1;
                    if (lastCommand == 'Q' || lastCommand == 'T') {
                        x1 = 2 * currentX - lastControlX;
                        y1 = 2 * currentY - lastControlY;
                    } else {
                        x1 = currentX;
                        y1 = currentY;
                    }
                    float x = relative ? currentX + args[0] : args[0];
                    float y = relative ? currentY + args[1] : args[1];
                    path.quadTo(x1, y1, x, y);
                    lastControlX = x1;
                    lastControlY = y1;
                    currentX = x;
                    currentY = y;
                    break;
                }
                case 'A': {
                    float rx = args[0];
                    float ry = args[1];
                    float rotation = args[2];
                    boolean largeArc = args[3] != 0f;
                    boolean sweep = args[4] != 0f;
                    float x = relative ? currentX + args[5] : args[5];
                    float y = relative ? currentY + args[6] : args[6];
                    arcTo(path, currentX, currentY, rx, ry, rotation, largeArc, sweep, x, y);
                    currentX = x;
                    currentY = y;
                    break;
                }
                case 'Z': {
                    path.close();
                    currentX = startX;
                    currentY = startY;
                    break;
                }
                default:
                    break;
            }
            lastCommand = upper;
        }
        return path;
    }

    private static int argCountFor(char command) {
        switch (Character.toUpperCase(command)) {
            case 'M':
            case 'L':
            case 'T':
                return 2;
            case 'H':
            case 'V':
                return 1;
            case 'C':
                return 6;
            case 'S':
            case 'Q':
                return 4;
            case 'A':
                return 7;
            case 'Z':
                return 0;
            default:
                return 0;
        }
    }

    private static boolean isCommand(char c) {
        switch (Character.toUpperCase(c)) {
            case 'M':
            case 'L':
            case 'H':
            case 'V':
            case 'C':
            case 'S':
            case 'Q':
            case 'T':
            case 'A':
            case 'Z':
                return true;
            default:
                return false;
        }
    }

    private static boolean isWhitespaceOrComma(char c) {
        return c == ' ' || c == ',' || c == '\t' || c == '\n' || c == '\r';
    }

    private static int skipSeparators(String data, int index, int length) {
        while (index < length && isWhitespaceOrComma(data.charAt(index))) {
            index++;
        }
        return index;
    }

    private static float readNumber(String data, int start, int length, int[] endOut) {
        int index = start;
        boolean seenDigit = false;
        boolean seenDot = false;
        boolean seenExp = false;
        if (index < length && (data.charAt(index) == '+' || data.charAt(index) == '-')) {
            index++;
        }
        while (index < length) {
            char c = data.charAt(index);
            if (c >= '0' && c <= '9') {
                seenDigit = true;
                index++;
            } else if (c == '.' && !seenDot && !seenExp) {
                seenDot = true;
                index++;
            } else if ((c == 'e' || c == 'E') && seenDigit && !seenExp) {
                seenExp = true;
                index++;
                if (index < length && (data.charAt(index) == '+' || data.charAt(index) == '-')) {
                    index++;
                }
            } else {
                break;
            }
        }
        endOut[0] = index;
        if (index == start) {
            return 0f;
        }
        try {
            return Float.parseFloat(data.substring(start, index));
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    /**
     * Approximates an SVG elliptical arc using cubic beziers appended to the path.
     */
    private static void arcTo(Path path, float x0, float y0, float rx, float ry,
                              float rotationDeg, boolean largeArc, boolean sweep,
                              float x, float y) {
        if (rx == 0f || ry == 0f) {
            path.lineTo(x, y);
            return;
        }
        rx = Math.abs(rx);
        ry = Math.abs(ry);
        double phi = Math.toRadians(rotationDeg % 360.0);
        double cosPhi = Math.cos(phi);
        double sinPhi = Math.sin(phi);

        double dx = (x0 - x) / 2.0;
        double dy = (y0 - y) / 2.0;
        double x1p = cosPhi * dx + sinPhi * dy;
        double y1p = -sinPhi * dx + cosPhi * dy;

        double rxSq = rx * rx;
        double rySq = ry * ry;
        double x1pSq = x1p * x1p;
        double y1pSq = y1p * y1p;

        double lambda = x1pSq / rxSq + y1pSq / rySq;
        if (lambda > 1.0) {
            double scale = Math.sqrt(lambda);
            rx *= scale;
            ry *= scale;
            rxSq = rx * rx;
            rySq = ry * ry;
        }

        double sign = (largeArc != sweep) ? 1.0 : -1.0;
        double num = rxSq * rySq - rxSq * y1pSq - rySq * x1pSq;
        double den = rxSq * y1pSq + rySq * x1pSq;
        double coef = sign * Math.sqrt(Math.max(0.0, num / den));
        double cxp = coef * (rx * y1p / ry);
        double cyp = coef * -(ry * x1p / rx);

        double cx = cosPhi * cxp - sinPhi * cyp + (x0 + x) / 2.0;
        double cy = sinPhi * cxp + cosPhi * cyp + (y0 + y) / 2.0;

        double startAngle = angle(1.0, 0.0, (x1p - cxp) / rx, (y1p - cyp) / ry);
        double deltaAngle = angle((x1p - cxp) / rx, (y1p - cyp) / ry,
                (-x1p - cxp) / rx, (-y1p - cyp) / ry);
        if (!sweep && deltaAngle > 0) {
            deltaAngle -= 2 * Math.PI;
        } else if (sweep && deltaAngle < 0) {
            deltaAngle += 2 * Math.PI;
        }

        int segments = (int) Math.ceil(Math.abs(deltaAngle) / (Math.PI / 2.0));
        double delta = deltaAngle / segments;
        double t = (4.0 / 3.0) * Math.tan(delta / 4.0);

        double angle1 = startAngle;
        for (int i = 0; i < segments; i++) {
            double angle2 = angle1 + delta;
            double cos1 = Math.cos(angle1);
            double sin1 = Math.sin(angle1);
            double cos2 = Math.cos(angle2);
            double sin2 = Math.sin(angle2);

            double e1x = cx + rx * cosPhi * cos1 - ry * sinPhi * sin1;
            double e1y = cy + rx * sinPhi * cos1 + ry * cosPhi * sin1;
            double e2x = cx + rx * cosPhi * cos2 - ry * sinPhi * sin2;
            double e2y = cy + rx * sinPhi * cos2 + ry * cosPhi * sin2;

            double d1x = -rx * cosPhi * sin1 - ry * sinPhi * cos1;
            double d1y = -rx * sinPhi * sin1 + ry * cosPhi * cos1;
            double d2x = -rx * cosPhi * sin2 - ry * sinPhi * cos2;
            double d2y = -rx * sinPhi * sin2 + ry * cosPhi * cos2;

            float c1x = (float) (e1x + t * d1x);
            float c1y = (float) (e1y + t * d1y);
            float c2x = (float) (e2x - t * d2x);
            float c2y = (float) (e2y - t * d2y);
            path.cubicTo(c1x, c1y, c2x, c2y, (float) e2x, (float) e2y);
            angle1 = angle2;
        }
    }

    private static double angle(double ux, double uy, double vx, double vy) {
        double dot = ux * vx + uy * vy;
        double len = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
        double value = dot / len;
        if (value < -1.0) {
            value = -1.0;
        } else if (value > 1.0) {
            value = 1.0;
        }
        double result = Math.acos(value);
        if (ux * vy - uy * vx < 0) {
            result = -result;
        }
        return result;
    }
}
