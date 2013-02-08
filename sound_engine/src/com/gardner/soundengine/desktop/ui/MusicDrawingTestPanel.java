package com.gardner.soundengine.desktop.ui;

import java.util.ArrayList;
import java.util.List;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.*;

import com.gardner.soundengine.common.*;
import com.gardner.soundengine.desktop.staves.*;
import com.gardner.soundengine.staves.*;

public class MusicDrawingTestPanel extends JPanel {
    private DesktopStaffCanvas canvas;

    public MusicDrawingTestPanel() {
        setPreferredSize(new Dimension(700, 200));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        canvas = new DesktopStaffCanvas(700, 200);
        canvas.setColor(Color.WHITE.getRGB());
        canvas.drawRect(0, 0, 700, 200);
        add(canvas);

        StaffImageUtil util = new DesktopStaffImageUtil();
        StaffDrawer drawer = new StaffDrawer();
        drawer.drawTrebleClefStaff(canvas, StaffSize.LARGE, util);
    }
}
