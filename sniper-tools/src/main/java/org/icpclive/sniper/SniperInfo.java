package org.icpclive.sniper;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Scanner;

public class SniperInfo {
    public final String hostName;
    public final File coordinatesFile;
    public final int cameraID;
    public LocatorPoint[] coordinates;

    public SniperInfo(String hostName, File coordinatesFile, int cameraID) throws FileNotFoundException {
        this.hostName = hostName;
        this.coordinatesFile = coordinatesFile;
        this.cameraID = cameraID;
        update();
    }

    public void update() throws FileNotFoundException {
        Scanner in = new Scanner(coordinatesFile);
        in.useLocale(Locale.US);
        int n = in.nextInt();
        coordinates = new LocatorPoint[n];
        for (int i = 0; i < n; i++) {
            coordinates[i] = new LocatorPoint(in.nextInt(), in.nextDouble(), in.nextDouble(), in.nextDouble());
        }
    }

    @Override
    public String toString() {
        return "Sniper " + (cameraID + 1);
    }
}
