import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.*;

/**
 * @author: pashka
 */
public class SniperCalibrator implements MJpegViewer, MouseListener, KeyListener {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final double ANGLE = 1.28;
    private static final double COMPENSATION_TILT = 0;
    private static final double COMPENSATION_PAN = 0;
    private static final double COMPENSATION_X = 1;
    private static final double COMPENSATION_Y = 1;
    private Image image;
    private Image bg;
    private String url;

    double pan = 0, tilt = 0, angle = ANGLE;
    private java.util.List<Point> points = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Input sniper URL:");
        Scanner in = new Scanner(System.in);
        String cameraUrl = in.next();
        new SniperCalibrator(cameraUrl).run();
    }

    public SniperCalibrator(String url) {
        this.url = url;
    }

    JFrame frame;
    JLabel label;

    private void run() {
        try {
//            Scanner in = new Scanner(new File("output.txt"));
//            int n = in.nextInt();
//            for (int i = 0; i < n; i++) {
//                points.add(new Point(in.nextDouble(), in.nextDouble(), in.nextDouble()));
//            }
//            in.close();

            image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
//            updateState();
            new Thread() {
                @Override
                public void run() {
                    MjpegRunner runner = null;
                    try {
                        runner = new MjpegRunner(SniperCalibrator.this, new URL(url + "/mjpg/video.mjpg"));
                        runner.run();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            }.start();
            frame = new JFrame();
            label = new JLabel();
//            bg = ImageIO.read(new File("bg.png"));

//            GraphicsEnvironment ge = GraphicsEnvironment.
//                    getLocalGraphicsEnvironment();
//            GraphicsDevice[] gs =
//                    ge.getScreenDevices();
//            image = gs[0].getConfigurations()[0].createCompatibleVolatileImage(WIDTH, HEIGHT);
            draw((Graphics2D) image.getGraphics());

            label.setIcon(new ImageIcon(image));
            frame.add(label);
            frame.pack();
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            label.addMouseListener(this);
            frame.addKeyListener(this);
            frame.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private synchronized void updateState() throws Exception {
        parse(PtzTest.sendGet(url + "/axis-cgi/com/ptz.cgi?query=position,limits&camera=1&html=no&timestamp=" + PtzTest.getUTCTime()));
    }

    void parse(String s) {
        s = s.trim();
        int l = 0;
        int r = 0;
        while (r < s.length()) {
            l = r;
            r = l + 1;
            while (r < s.length() && Character.isAlphabetic(s.charAt(r))) {
                r++;
            }
            String key = s.substring(l, r);
            l = r + 1;
            r = l + 1;
            while (r < s.length() && !Character.isAlphabetic(s.charAt(r))) {
                r++;
            }
            try {
                double value = Double.parseDouble(s.substring(l, r));
                switch (key) {
                    case "pan":
                        pan = value * Math.PI / 180 + COMPENSATION_PAN;
                        break;
                    case "tilt":
                        tilt = value * Math.PI / 180 + COMPENSATION_TILT;
                        break;
                    case "zoom":
                        double maxmag = 35;
                        double mag = 1 + (maxmag - 1) * value / 9999;
                        angle = ANGLE / mag;
                        break;
                }
            } catch (Exception e) {
            }
        }
    }

    @Override
    public synchronized void setBufferedImage(BufferedImage image) {
        Graphics2D g = (Graphics2D) this.image.getGraphics();
        g.drawImage(image, 0, 0, WIDTH, HEIGHT, null);
        try {
            updateState();
        } catch (Exception e) {
            e.printStackTrace();
        }
        draw(g);
    }

    private synchronized void draw(Graphics2D g) {
//        g.setColor(Color.BLACK);
//        g.fillRect(0, 0, WIDTH, HEIGHT);
//        g.drawImage(bg, 0, 0, WIDTH, HEIGHT, null);
        for (Point p : points) {
            p = p.rotateY(pan);
            p = p.rotateX(-tilt);
            int R = (int) (20 / Math.abs(p.z) * ANGLE / angle) + 5;
            p = p.multiply(1 / p.z);
            p = p.multiply(WIDTH / angle);
            p.x *= COMPENSATION_X;
            p.y *= COMPENSATION_Y;
            p = p.move(new Point(WIDTH / 2, HEIGHT / 2, 0));
            g.setColor(new Color(255, 0, 0, 150));
            g.fillOval(
                    (int) (p.x - R),
                    (int) (p.y - R),
                    (int) (2 * R),
                    (int) (2 * R));
        }
    }

    @Override
    public synchronized void repaint() {
        frame.repaint();
    }

    @Override
    public void setFailedString(String s) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyChar() == ' ')
            click(WIDTH / 2, HEIGHT / 2);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        click(x, y);
    }

    public synchronized void click(int x, int y) {
        Point p = new Point(x, y, WIDTH / angle);
        p = p.move(new Point(-WIDTH / 2, -HEIGHT / 2, 0));
        p = p.multiply(angle / WIDTH);
        p = p.rotateX(tilt);
        p = p.rotateY(-pan);

        p = p.multiply(1 / Math.abs(p.z));

        points.add(p);
        System.out.println(p.x + " " + p.y + " " + p.z);

        Scanner in = null;
        try {
            in = new Scanner(new File("input.txt"));
        } catch (FileNotFoundException e1) {
        }

        int n = in.nextInt();
        int[] first = new int[4];
        for (int i = 0; i < 4; i++) {
            first[i] = in.nextInt() - 1;
        }
        int[] xx = new int[n];
        int[] yy = new int[n];
        for (int i = 0; i < n; i++) {
            in.nextInt();
            xx[i] = in.nextInt();
            yy[i] = in.nextInt();
        }

        if (points.size() == 4) {
            double[][] a = new double[9][10];
            for (int i = 0; i < 4; i++) {
                a[i * 2][0] = xx[first[i]] * points.get(i).z;
                a[i * 2][1] = yy[first[i]] * points.get(i).z;
                a[i * 2][2] = points.get(i).z;
                a[i * 2][6] = -xx[first[i]] * points.get(i).x;
                a[i * 2][7] = -yy[first[i]] * points.get(i).x;
                a[i * 2][8] = -points.get(i).x;

                a[i * 2 + 1][3] = xx[first[i]] * points.get(i).z;
                a[i * 2 + 1][4] = yy[first[i]] * points.get(i).z;
                a[i * 2 + 1][5] = points.get(i).z;
                a[i * 2 + 1][6] = -xx[first[i]] * points.get(i).y;
                a[i * 2 + 1][7] = -yy[first[i]] * points.get(i).y;
                a[i * 2 + 1][8] = -points.get(i).y;
            }
            a[8][8] = a[8][9] = 1;
            for (int i = 0; i < a.length; i++) {
                int ii = i;
                for (int t = i; t < a.length; t++) {
                    if (Math.abs(a[t][i]) > Math.abs(a[ii][i])) {
                        ii = t;
                    }
                }
                double[] tt = a[i];
                a[i] = a[ii];
                a[ii] = tt;
                if (Math.abs(a[i][i]) < 1e-9) throw new RuntimeException();
                double k = 1.0 / a[i][i];
                for (int j = 0; j < a[i].length; j++) {
                    a[i][j] *= k;
                }
                for (int t = 0; t < a.length; t++) {
                    if (t == i) continue;
                    k = -a[t][i];
                    for (int j = 0; j < a[t].length; j++) {
                        a[t][j] += k * a[i][j];
                    }
                }
//                for (int q = 0; q < a.length; q++) {
//                    System.out.println(Arrays.toString(a[q]));
//                }
//                System.out.println();
            }
            double[] r = new double[9];
            for (int i = 0; i < 9; i++) r[i] = a[i][9] / a[i][i];

            double dx = r[0] * (xx[first[1]] - xx[first[0]])
                    + r[1] * (yy[first[1]] - yy[first[0]]) + r[2];
            double dy = r[3] * (xx[first[1]] - xx[first[0]])
                    + r[4] * (yy[first[1]] - yy[first[0]]) + r[5];
            double dz = r[6] * (xx[first[1]] - xx[first[0]])
                    + r[7] * (yy[first[1]] - yy[first[0]]) + r[8];

            double dx1 = (xx[first[1]] - xx[first[0]]);
            double dy1 = (yy[first[1]] - yy[first[0]]);

            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double d2 = Math.sqrt(dx1 * dx1 + dy1 * dy1);

            for (int i = 0; i < 9; i++) {
                r[i] *= d2 / d;
            }

            if (Math.signum(r[2]) != Math.signum(points.get(0).x)) {
                for (int i = 0; i < 9; i++) {
                    r[i] = -r[i];
                }
            }

            points.clear();

            PrintWriter out = null;
            try {
                out = new PrintWriter("output.txt");
            } catch (FileNotFoundException e1) {
            }
            out.println(n);
            for (int i = 0; i < n; i++) {
                double xc = xx[i];
                double yc = yy[i];
                double xt = r[0] * xc + r[1] * yc + r[2];
                double yt = r[3] * xc + r[4] * yc + r[5];
                double zt = r[6] * xc + r[7] * yc + r[8];
                out.println(xt + " " + yt + " " + zt);
                points.add(new Point(xt, yt, zt));
            }
            out.close();
        }

        Graphics2D g = (Graphics2D) this.image.getGraphics();
        draw(g);
        frame.repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    static class Point {
        double x, y, z;

        public Point(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        Point move(Point d) {
            return new Point(x + d.x, y + d.y, z + d.z);
        }

        Point multiply(double d) {
            return new Point(x * d, y * d, z * d);
        }

        Point rotateZ(double a) {
            return new Point(x * Math.cos(a) - y * Math.sin(a),
                    x * Math.sin(a) + y * Math.cos(a),
                    z);
        }

        Point rotateY(double a) {
            return new Point(x * Math.cos(a) - z * Math.sin(a),
                    y,
                    x * Math.sin(a) + z * Math.cos(a));
        }

        Point rotateX(double a) {
            return new Point(x,
                    y * Math.cos(a) - z * Math.sin(a),
                    y * Math.sin(a) + z * Math.cos(a));
        }
    }
}
