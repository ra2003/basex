package org.basex.gui.layout;

import static org.basex.gui.GUIConstants.*;
import static org.basex.gui.layout.BaseXKeys.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * DoubleSlider implementation.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
public final class BaseXDSlider extends BaseXPanel {
  /** Slider width. */
  private static final int ARROW = 17;
  /** Label space. */
  public static final int LABELW = 300;

  /** Minimum slider value. */
  public final double totMin;
  /** Maximum slider value. */
  public final double totMax;

  /** Current slider value. */
  public double min;
  /** Current slider value. */
  public double max;
  /** Integer flag. */
  public boolean itr;

  /** Listener. */
  private final ActionListener listener;
  /** Cached slider value. */
  private double oldMin;
  /** Cached slider value. */
  private double oldMax;
  /** Mouse position for dragging operations. */
  private int mouX;
  /** Left button flag. */
  private boolean left;
  /** Right button flag. */
  private boolean right;
  /** Right button flag. */
  private boolean center;
  /** Logarithmic scale. */
  private final boolean log;

  /**
   * Constructor.
   * @param main reference to the main window
   * @param mn min value
   * @param mx max value
   * @param list listener
   */
  public BaseXDSlider(final Window main, final double mn, final double mx,
      final ActionListener list) {
    super(main);
    listener = list;
    totMin = mn;
    totMax = mx;
    min = mn;
    max = mx;
    // choose logarithmic scaling for larger ranges
    log = Math.log(totMax) - Math.log(totMin) > 5 && totMax - totMin > 100;
    mode(Fill.NONE).setFocusable(true);

    BaseXLayout.setHeight(this, getFont().getSize() + 9);
    BaseXLayout.setWidth(this, 200 + LABELW);

    addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(final FocusEvent e) {
        repaint();
      }
      @Override
      public void focusLost(final FocusEvent e) {
        repaint();
      }
    });

    addKeyListener(this);
    addMouseListener(this);
    addMouseMotionListener(this);
    setToolTip();
  }

  @Override
  public void mouseMoved(final MouseEvent e) {
    mouX = e.getX();
    final Range r = new Range(this);
    left = mouX >= r.xs && mouX <= r.xs + ARROW;
    right = mouX >= r.xe && mouX <= r.xe + ARROW;
    center = mouX + ARROW > r.xs && mouX < r.xe;
    oldMin = encode(min);
    oldMax = encode(max);
  }

  @Override
  public void mousePressed(final MouseEvent e) {
    mouseMoved(e);
  }

  @Override
  public void mouseDragged(final MouseEvent e) {
    if(!left && !right && !center) return;

    final Range r = new Range(this);
    final double prop = r.dist * (mouX - e.getX()) / r.w;

    if(left) {
      min = limit(totMin, max, decode(oldMin - prop) - 1);
    } else if(right) {
      max = limit(min, totMax, decode(oldMax - prop) - 1);
    } else {
      min = limit(totMin, totMax, decode(oldMin - prop) - 1);
      max = limit(totMin, totMax, decode(oldMax - prop) - 1);
    }
    if(itr) {
      min = (long) min;
      max = (long) max;
    }
    listener.actionPerformed(null);
    setToolTip();
    repaint();
  }

  /**
   * Sets a new tooltip.
   */
  private void setToolTip() {
    final double mn = (long) (min * 100) / 100.0;
    final double mx = (long) (max * 100) / 100.0;
    setToolTipText(BaseXLayout.value(mn) + " - " + BaseXLayout.value(mx));
  }

  @Override
  public void mouseReleased(final MouseEvent e) {
    left = false;
    right = false;
    center = false;
  }

  @Override
  public void keyPressed(final KeyEvent e) {
    oldMin = min;
    oldMax = min;
    double diffMin = 0;
    double diffMax = 0;
    if(PREV.is(e)) {
      diffMin = -1;
      diffMax = -1;
    } else if(NEXT.is(e)) {
      diffMin = 1;
      diffMax = 1;
    } else if(PREVLINE.is(e)) {
      diffMin = -1;
      diffMax = 1;
    } else if(NEXTLINE.is(e)) {
      diffMin = 1;
      diffMax = -1;
    } else if(LINESTART.is(e)) {
      min = totMin;
    } else if(LINEEND.is(e)) {
      max = totMax;
    }
    if(e.isShiftDown()) {
      diffMin /= 10;
      diffMax /= 10;
    }

    final double dist = encode(totMax) - encode(totMin);
    diffMin = dist / 20 * diffMin;
    diffMax = dist / 20 * diffMax;

    if(diffMin != 0) {
      min = limit(totMin, max, decode(Math.max(0, encode(min) + diffMin)));
    }
    if(diffMax != 0) {
      max = limit(min, totMax, decode(Math.max(0, encode(max) + diffMax)));
    }
    if(min != oldMin || max != oldMax) {
      if(itr) {
        if(min != oldMin) min = min > oldMin ? Math.max(oldMin + 1,
            (long) min) : Math.min(oldMin - 1, (long) min);
        if(max != oldMax) max = max > oldMax ? Math.max(oldMax + 1,
            (long) max) : Math.min(oldMax - 1, (long) max);
      }
      listener.actionPerformed(null);
      repaint();
    }
  }

  @Override
  public void paintComponent(final Graphics g) {
    super.paintComponent(g);

    final int w = getWidth() - LABELW;
    final int h = getHeight();
    final int hh = h / 2;

    final boolean focus = hasFocus();
    g.setColor(focus ? Color.white : color1);
    g.fillRect(0, hh - 4, w, 8);
    g.setColor(Color.black);
    g.drawLine(0, hh - 4, w - 1, hh - 4);
    g.drawLine(0, hh - 4, 0, hh + 4);
    g.setColor(color3);
    g.drawLine(w - 1, hh - 4, w - 1, hh + 4);
    g.drawLine(0, hh + 4, w, hh + 4);

    final Range r = new Range(this);
    BaseXLayout.drawCell(g, r.xs, r.xe + ARROW, 2, h - 2, false);

    if(r.xs + ARROW < r.xe) {
      g.setColor(color5);
      g.drawLine(r.xs + ARROW, 3, r.xs + ARROW, h - 4);
      g.drawLine(r.xe - 1, 3, r.xe - 1, h - 4);
      g.setColor(Color.white);
      if(r.xs + ARROW + 2 < r.xe) {
        g.drawLine(r.xs + ARROW + 1, 4, r.xs + ARROW + 1, h - 5);
        g.drawLine(r.xe, 4, r.xe, h - 5);
      }
      g.drawLine(r.xs + ARROW - 1, 4, r.xs + ARROW - 1, h - 5);
      g.drawLine(r.xe - 2, 4, r.xe - 2, h - 5);
    }

    // draw arrows
    final Polygon pol = new Polygon(
        new int[] { r.xs + 11, r.xs + 5, r.xs + 5, r.xs + 11 },
        new int[] { hh - 5, hh - 1, hh, hh + 5 }, 4);
    g.setColor(focus ? color5 : GRAY);
    g.fillPolygon(pol);
    pol.xpoints = new int[] { r.xe + 5, r.xe + 12, r.xe + 12, r.xe + 5 };
    g.fillPolygon(pol);

    g.setColor(focus ? Color.black : DGRAY);
    g.drawLine(r.xs + 11, hh - 5, r.xs + 11, hh + 4);
    g.drawLine(r.xs + 11, hh - 5, r.xs + 6, hh - 1);
    g.drawLine(r.xe + 5, hh - 5, r.xe + 5, hh + 4);
    g.drawLine(r.xe + 5, hh - 5, r.xe + 11, hh - 1);

    g.setColor(Color.white);
    g.drawLine(r.xs + 10, hh + 4, r.xs + 6, hh + 1);
    g.drawLine(r.xe + 6, hh + 4, r.xe + 11, hh + 1);

    // draw range info
    g.setColor(Color.black);
    final double mn = (long) (min * 100) / 100.0;
    final double mx = (long) (max * 100) / 100.0;

    g.drawString(BaseXLayout.value(mn) + " - " + BaseXLayout.value(mx),
        w + 15, h - (h - getFont().getSize()) / 2);
  }

  /**
   * Encodes the specified value.
   * @param v value to be normalized
   * @return new value
   */
  double encode(final double v) {
    return log ? Math.log(v + 1) : v;
  }

  /**
   * Decodes the specified value.
   * @param v value to be normalized
   * @return new value
   */
  private double decode(final double v) {
    return log ? Math.exp(v) - 1 : v;
  }

  /**
   * Returns a double in the specified minimum and maximum range.
   * @param mn minimum value
   * @param mx maximum value
   * @param val value
   * @return new value
   */
  private double limit(final double mn, final double mx, final double val) {
    return Math.max(mn, Math.min(mx, val));
  }

  /** Range class. */
  private static class Range {
    /** Range distance. */ double dist;
    /** Start position. */ int xs;
    /** End position.   */ int xe;
    /** Slider width.   */ int w;

    /**
     * Constructor.
     * @param s slider reference
     */
    Range(final BaseXDSlider s) {
      w = s.getWidth() - LABELW - ARROW * 2;
      dist = s.encode(s.totMax - s.totMin);
      xs = (int) (s.encode(s.min - s.totMin) * w / dist);
      xe = (s.totMin == s.totMax ? w :
        (int) (s.encode(s.max - s.totMin) * w / dist)) + ARROW;
    }
  }
}
