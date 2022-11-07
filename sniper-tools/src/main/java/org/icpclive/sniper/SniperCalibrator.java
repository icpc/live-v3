package org.icpclive.sniper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;

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
    private static Scanner in = new Scanner(System.in);

    public static void main(String[] args) throws FileNotFoundException {
        String[] urls;
        {
            Scanner in = new Scanner(new File("snipers.txt"));
            int m = in.nextInt();
            urls = new String[m];
            for (int i = 0; i < m; i++) {
                urls[i] = in.next();
            }
            in.close();
        }
        System.out.println("Select sniper (1-" + urls.length + ")");
        int sniper = in.nextInt();
        new SniperCalibrator(urls[sniper - 1]).run();
    }

    public SniperCalibrator(String url) {
        this.url = url;
    }

    JFrame frame;
    JLabel label;

    int currentTeam = -1;
    Object currentTeamMonitor = new Object();

    private void run() {
        try {
            readInput();
            startPlayer();
            synchronized (currentTeamMonitor) {
                while (true) {
                    System.out.println("Input team id:");
                    currentTeam = in.nextInt();
                    if (currentTeam == -1) {
                        points.clear();
                        locations.clear();
                        continue;
                    }
                    System.out.println("Now locate team " + currentTeam);
                    while (currentTeam != -1) {
                        currentTeamMonitor.wait();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void startPlayer() {
        image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        new Thread() {
            @Override
            public void run() {
                MjpegRunner runner;
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
        draw((Graphics2D) image.getGraphics());

        label.setIcon(new ImageIcon(image));
        frame.add(label);
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        label.addMouseListener(this);
        frame.addKeyListener(this);
        frame.setVisible(true);
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

    class Position {
        int id;
        Point p;

        public Position(int id, int x, int y) {
            this.id = id;
            p = new Point(x, y, 0);
        }

        public Position(int id, double x, double y, double z) {
            this.id = id;
            p = new Point(x, y, z);
        }

        public Position(int id, Point p) {
            this.id = id;
            this.p = p;
        }
    }

    List<Position> input = new ArrayList<>();
    List<Position> locations = new ArrayList<>();

    void readInput() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("input.txt"));
        for (int x = 0; ; x++) {
            String s = reader.readLine();
            if (s == null) break;
            String[] ss = s.split("\\s+");
            for (int y = 0; y < ss.length; y++) {
                try {
                    int id = Integer.parseInt(ss[y]);
                    input.add(new Position(id, x, y));
                } catch (NumberFormatException e) {
                }
            }
        }
    }

    void recalculate() {
        if (locations.size() < 4) return;

        List<Position> from = new ArrayList<>();
        List<Position> to = new ArrayList<>();

        Map<Integer, Position> mp = new HashMap<>();
        for (Position position : input) {
            mp.put(position.id, position);
        }

        for (int i = 0; i < 4; i++) {
            Position e = locations.get(locations.size() - 1 - i);
            to.add(e);
            Position e1 = mp.get(e.id);
            if (e1 == null) {
                System.out.println("no team " + e.id);
                return;
            }
            from.add(e1);
        }

        double[][] a = new double[9][10];
        for (int i = 0; i < 4; i++) {
            Point A = from.get(i).p;
            Point B = to.get(i).p;
            a[i * 2][0] = A.x * B.z;
            a[i * 2][1] = A.y * B.z;
            a[i * 2][2] = B.z;
            a[i * 2][6] = -A.x * B.x;
            a[i * 2][7] = -A.y * B.x;
            a[i * 2][8] = -B.x;

            a[i * 2 + 1][3] = A.x * B.z;
            a[i * 2 + 1][4] = A.y * B.z;
            a[i * 2 + 1][5] = B.z;
            a[i * 2 + 1][6] = -A.x * B.y;
            a[i * 2 + 1][7] = -A.y * B.y;
            a[i * 2 + 1][8] = -B.y;
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
        }
        double[] r = new double[9];
        for (int i = 0; i < 9; i++) r[i] = a[i][9] / a[i][i];

        double ddx = from.get(1).p.x - from.get(0).p.x;
        double ddy = from.get(1).p.y - from.get(0).p.y;
        double dx = r[0] * ddx + r[1] * ddy + r[2];
        double dy = r[3] * ddx + r[4] * ddy + r[5];
        double dz = r[6] * ddx + r[7] * ddy + r[8];

        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double d2 = Math.sqrt(ddx * ddx + ddy * ddy);

        for (int i = 0; i < 9; i++) {
            r[i] *= d2 / d;
        }

        if (Math.signum(r[2]) != Math.signum(points.get(0).x)) {
            for (int i = 0; i < 9; i++) {
                r[i] = -r[i];
            }
        }

        points.clear();

        try {
            PrintWriter out = new PrintWriter("output.txt");
            out.println(input.size());
            for (int i = 0; i < input.size(); i++) {
                double xc = input.get(i).p.x;
                double yc = input.get(i).p.y;
                double xt = r[0] * xc + r[1] * yc + r[2];
                double yt = r[3] * xc + r[4] * yc + r[5];
                double zt = r[6] * xc + r[7] * yc + r[8];
                out.println(input.get(i).id + " " + xt + " " + yt + " " + zt);
                points.add(new Point(xt, yt, zt));
            }
            out.close();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
    }

    public void click(int x, int y) {
        synchronized (currentTeamMonitor) {
            if (currentTeam == -1) return;
            Point p = new Point(x, y, WIDTH / angle);
            p = p.move(new Point(-WIDTH / 2, -HEIGHT / 2, 0));
            p = p.multiply(angle / WIDTH);
            p = p.rotateX(tilt);
            p = p.rotateY(-pan);
            p = p.multiply(1 / Math.abs(p.z));
            points.add(p);
            locations.add(new Position(currentTeam, p));
            currentTeam = -1;
            recalculate();
            Graphics2D g = (Graphics2D) this.image.getGraphics();
            draw(g);
            frame.repaint();
            currentTeamMonitor.notifyAll();
        }
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
