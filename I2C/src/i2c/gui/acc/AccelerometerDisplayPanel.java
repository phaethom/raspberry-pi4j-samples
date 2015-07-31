package i2c.gui.acc;


import i2c.gui.utils.Point3D;

import i2c.sensor.LSM303;
import i2c.sensor.listener.L3GD20Listener;
import i2c.sensor.listener.LSM303Listener;
import i2c.sensor.listener.SensorL3GD20Context;

import i2c.sensor.listener.SensorLSM303Context;
import i2c.sensor.main.SampleL3GD20RealReader;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;

import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

// This class listens to the  LSM303 (acc + mag)
public class AccelerometerDisplayPanel
  extends JPanel
{
  @SuppressWarnings("compatibility:5286281276243161150")
  public final static long serialVersionUID = 1L;
  
  protected transient Stroke thick = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
  protected transient Stroke dotted = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] {2f}, 0f);
  protected transient Stroke origStroke = null;
  
  private transient LSM303 sensor = null;
  
  private List<Integer>  accXList = new ArrayList<Integer>();
  private List<Integer>  accYList = new ArrayList<Integer>();
  private List<Integer>  accZList = new ArrayList<Integer>();
  private List<Integer>  magXList = new ArrayList<Integer>();
  private List<Integer>  magYList = new ArrayList<Integer>();
  private List<Integer>  magZList = new ArrayList<Integer>();
  private List<Float> headingList = new ArrayList<Float>();
  
  private int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
  private int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
  private int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
  
  private final double DELTA_T = 0.05;
  
  public AccelerometerDisplayPanel()
  {
    try
    {
      jbInit();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  private void jbInit()
    throws Exception
  {
    this.setLayout(null);
    this.setOpaque(false);
    this.setBackground(new Color(0, 0, 0, 0));
    
    Thread sensorListener = new Thread()
    {
      public void run()
      {
        try
        {
          sensor = new LSM303();
          System.out.println("...Adding listener");
          LSM303Listener dataListener = new LSM303Listener()
            {
              public void dataDetected(int accX, int accY, int accZ, int magX, int magY, int magZ, float heading) 
              {
                maxX = Math.max(maxX, accX);
                minX = Math.min(minX, accX);
                maxY = Math.max(maxY, accX);
                minY = Math.min(minY, accX);
                maxZ = Math.max(maxZ, accX);
                minZ = Math.min(minZ, accX);
                synchronized (accXList) { accXList.add(accX); while (accXList.size() > 1000) { accXList.remove(0); } }
                synchronized (accYList) { accYList.add(accY); while (accYList.size() > 1000) { accYList.remove(0); } }
                synchronized (accZList) { accZList.add(accZ); while (accZList.size() > 1000) { accZList.remove(0); } }
                synchronized (magXList) { magXList.add(magX); while (magXList.size() > 1000) { magXList.remove(0); } }
                synchronized (magYList) { magYList.add(magY); while (magYList.size() > 1000) { magYList.remove(0); } }
                synchronized (magZList) { magZList.add(magZ); while (magZList.size() > 1000) { magZList.remove(0); } }
                synchronized (headingList) { headingList.add(heading); while (headingList.size() > 1000) { headingList.remove(0); } }
                repaint();
              }
              
              public void close()
              {
                sensor.setKeepReading(false);
              }
            };
          SensorLSM303Context.getInstance().addReaderListener(dataListener);
          sensor.setDataListener(dataListener);
          sensor.setWait(250L);
          System.out.println("Starting listening...");
          sensor.startReading();          
        }
        catch (Exception ex)
        {
          ex.printStackTrace();
        }
      }
    };
    sensorListener.start();
  }
  
  @Override
  protected void paintComponent(Graphics gr)
  {
    super.paintComponent(gr);

    Graphics2D g2d = (Graphics2D)gr;
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);      
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);      
//  origStroke = g2d.getStroke();
//  g2d.setStroke(origStroke);
//  System.out.println("X data:" + accXList.size() + " point(s) min:" + minX + ", max:" + maxX);
    gr.setColor(Color.white);
    gr.fillRect(0, 0, this.getWidth(), this.getHeight());
    gr.setColor(Color.green);
    synchronized (accXList) { drawData(0, gr, accXList, minX, maxX); }
    gr.setColor(Color.red);
    synchronized (accYList) { drawData(1, gr, accYList, minY, maxY); }
    gr.setColor(Color.blue);
    synchronized (accZList) { drawData(2, gr, accZList, minZ, maxZ); }
  }
  
  private void drawData(int idx, Graphics gr, List<Integer> data, int min, int max)
  {
    double xRatio = (double)this.getWidth() / (double)data.size();
    double yRatio = (double)(this.getHeight() / 3) / ((double)(max - min));
    int _x = 0;
    Point previous = null;
    for (Integer x : data)
    {
      int xPt = (int)(_x * xRatio);
      int yPt = (idx * (this.getHeight() / 3)) + (int)((x.intValue() - min) * yRatio);
      _x++;
      Point pt = new Point(xPt, this.getHeight() - yPt);
      if (previous != null)
        gr.drawLine(previous.x, previous.y, pt.x, pt.y);
      previous = pt;
    }
  }
}
