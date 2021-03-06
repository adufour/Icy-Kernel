package plugins.kernel.canvas;

import icy.canvas.Canvas3D;
import icy.canvas.CanvasLayerEvent;
import icy.canvas.CanvasLayerEvent.LayersEventType;
import icy.canvas.IcyCanvasEvent;
import icy.canvas.IcyCanvasEvent.IcyCanvasEventType;
import icy.canvas.Layer;
import icy.gui.component.button.IcyToggleButton;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.lut.LUT;
import icy.image.lut.LUT.LUTChannel;
import icy.painter.Overlay;
import icy.painter.VtkPainter;
import icy.preferences.CanvasPreferences;
import icy.preferences.XMLPreferences;
import icy.resource.ResourceUtil;
import icy.resource.icon.IcyIcon;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent.SequenceEventType;
import icy.system.thread.ThreadUtil;
import icy.type.TypeUtil;
import icy.type.collection.array.Array1DUtil;
import icy.util.ColorUtil;
import icy.util.StringUtil;
import icy.vtk.IcyVtkPanel;
import icy.vtk.VtkImageVolume;
import icy.vtk.VtkImageVolume.VtkVolumeBlendType;
import icy.vtk.VtkImageVolume.VtkVolumeMapperType;
import icy.vtk.VtkUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JToolBar;

import vtk.vtkActor;
import vtk.vtkAxesActor;
import vtk.vtkCamera;
import vtk.vtkColorTransferFunction;
import vtk.vtkCubeAxesActor;
import vtk.vtkImageData;
import vtk.vtkLight;
import vtk.vtkOrientationMarkerWidget;
import vtk.vtkPanel;
import vtk.vtkPiecewiseFunction;
import vtk.vtkProp;
import vtk.vtkPropPicker;
import vtk.vtkRenderWindow;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;
import vtk.vtkUnsignedCharArray;

/**
 * VTK 3D canvas class.
 * 
 * @author Stephane
 */
@SuppressWarnings("deprecation")
public class VtkCanvas extends Canvas3D implements PropertyChangeListener, Runnable, ActionListener
{
    /**
     * 
     */
    private static final long serialVersionUID = -1274251057822161271L;

    /**
     * icons
     */
    protected static final Image ICON_AXES3D = ResourceUtil.getAlphaIconAsImage("axes3d.png");
    protected static final Image ICON_BOUNDINGBOX = ResourceUtil.getAlphaIconAsImage("bbox.png");
    protected static final Image ICON_GRID = ResourceUtil.getAlphaIconAsImage("3x3_grid.png");
    protected static final Image ICON_RULER = ResourceUtil.getAlphaIconAsImage("ruler.png");
    protected static final Image ICON_RULERLABEL = ResourceUtil.getAlphaIconAsImage("ruler_label.png");
    protected static final Image ICON_SHADING = ResourceUtil.getColorIconAsImage("shading.png");

    /**
     * properties
     */
    public static final String PROPERTY_AXES = "axis";
    public static final String PROPERTY_BOUNDINGBOX = "boundingBox";
    public static final String PROPERTY_BOUNDINGBOX_GRID = "boundingBoxGrid";
    public static final String PROPERTY_BOUNDINGBOX_RULES = "boundingBoxRules";
    public static final String PROPERTY_BOUNDINGBOX_LABELS = "boundingBoxLabels";
    public static final String PROPERTY_SHADING = "shading";
    public static final String PROPERTY_LUT = "lut";
    public static final String PROPERTY_DATA = "data";
    public static final String PROPERTY_SCALE = "scale";
    public static final String PROPERTY_BOUNDS = "bounds";

    /**
     * preferences id
     */
    protected static final String PREF_ID = "vtkCanvas";

    /**
     * id
     */
    protected static final String ID_BOUNDINGBOX = PROPERTY_BOUNDINGBOX;
    protected static final String ID_BOUNDINGBOX_GRID = PROPERTY_BOUNDINGBOX_GRID;
    protected static final String ID_BOUNDINGBOX_RULES = PROPERTY_BOUNDINGBOX_RULES;
    protected static final String ID_BOUNDINGBOX_LABELS = PROPERTY_BOUNDINGBOX_LABELS;
    protected static final String ID_AXES = PROPERTY_AXES;
    protected static final String ID_SHADING = PROPERTY_SHADING;
    protected static final String ID_BGCOLOR = VtkSettingPanel.PROPERTY_BG_COLOR;
    protected static final String ID_MAPPER = VtkSettingPanel.PROPERTY_MAPPER;
    protected static final String ID_SAMPLE = VtkSettingPanel.PROPERTY_SAMPLE;
    protected static final String ID_BLENDING = VtkSettingPanel.PROPERTY_BLENDING;
    protected static final String ID_INTERPOLATION = VtkSettingPanel.PROPERTY_INTERPOLATION;
    protected static final String ID_AMBIENT = VtkSettingPanel.PROPERTY_AMBIENT;
    protected static final String ID_DIFFUSE = VtkSettingPanel.PROPERTY_DIFFUSE;
    protected static final String ID_SPECULAR = VtkSettingPanel.PROPERTY_SPECULAR;

    /**
     * Property to update
     */
    protected static class Property
    {
        String name;
        Object value;

        public Property(String name, Object value)
        {
            super();

            this.name = name;
            this.value = value;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof Property)
                return name.equals(((Property) obj).name);

            return super.equals(obj);
        }

        @Override
        public int hashCode()
        {
            return name.hashCode();
        }
    };

    /**
     * basic vtk objects
     */
    protected vtkRenderer renderer;
    protected vtkRenderWindow renderWindow;
    protected vtkCamera camera;
    protected vtkAxesActor axes;
    protected vtkCubeAxesActor boundingBox;
    protected vtkCubeAxesActor rulerBox;
    protected vtkOrientationMarkerWidget widget;

    /**
     * volume data
     */
    protected VtkImageVolume imageVolume;

    /**
     * GUI
     */
    protected VtkSettingPanel settingPanel;
    protected CustomVtkPanel panel3D;
    protected IcyToggleButton axesButton;
    protected IcyToggleButton boundingBoxButton;
    protected IcyToggleButton gridButton;
    protected IcyToggleButton rulerButton;
    protected IcyToggleButton rulerLabelButton;
    protected IcyToggleButton shadingButton;

    /**
     * internals
     */
    protected final Thread propertiesUpdater;
    protected XMLPreferences preferences;
    protected final LUT lutSave;
    protected final LinkedBlockingQueue<Property> propertiesToUpdate;
    protected final EDTTask<Object> edtTask;
    protected boolean initialized;

    public VtkCanvas(Viewer viewer)
    {
        super(viewer);

        initialized = false;

        // create the processor
        propertiesUpdater = new Thread(this, "VTK canvas properties updater");
        propertiesToUpdate = new LinkedBlockingQueue<VtkCanvas.Property>(256);

        // all channel visible at once by default
        posC = -1;

        final Sequence seq = getSequence();

        preferences = CanvasPreferences.getPreferences().node(PREF_ID);

        settingPanel = new VtkSettingPanel();
        panel = settingPanel;

        // default background color set to white
        settingPanel.setBackgroundColor(Color.WHITE);
        settingPanel.addPropertyChangeListener(this);

        // initialize VTK components & main GUI
        panel3D = new CustomVtkPanel();
        panel3D.addKeyListener(this);
        // set 3D view in center
        add(panel3D, BorderLayout.CENTER);

        // update nav bar & mouse infos
        mouseInfPanel.setVisible(false);
        updateZNav();
        updateTNav();

        // create toolbar buttons
        axesButton = new IcyToggleButton(new IcyIcon(ICON_AXES3D));
        axesButton.setFocusable(false);
        axesButton.setToolTipText("Display 3D axis");
        axesButton.addActionListener(this);
        boundingBoxButton = new IcyToggleButton(new IcyIcon(ICON_BOUNDINGBOX));
        boundingBoxButton.setFocusable(false);
        boundingBoxButton.setToolTipText("Display bounding box");
        boundingBoxButton.addActionListener(this);
        gridButton = new IcyToggleButton(new IcyIcon(ICON_GRID));
        gridButton.setFocusable(false);
        gridButton.setToolTipText("Display grid");
        gridButton.addActionListener(this);
        rulerButton = new IcyToggleButton(new IcyIcon(ICON_RULER));
        rulerButton.setFocusable(false);
        rulerButton.setToolTipText("Display rules");
        rulerButton.addActionListener(this);
        rulerLabelButton = new IcyToggleButton(new IcyIcon(ICON_RULERLABEL));
        rulerLabelButton.setFocusable(false);
        rulerLabelButton.setToolTipText("Display rules label");
        rulerLabelButton.addActionListener(this);
        shadingButton = new IcyToggleButton(new IcyIcon(ICON_SHADING, false));
        shadingButton.setFocusable(false);
        shadingButton.setToolTipText("Enable volume shadow");
        shadingButton.addActionListener(this);

        renderer = panel3D.GetRenderer();
        renderWindow = panel3D.GetRenderWindow();
        // set renderer properties
        renderer.SetBackground(Array1DUtil.floatArrayToDoubleArray(getBackgroundColor().getColorComponents(null)));
        renderer.SetLightFollowCamera(1);
        // set interactor
        final vtkRenderWindowInteractor interactor = new vtkRenderWindowInteractor();
        interactor.SetRenderWindow(renderWindow);

        camera = renderer.GetActiveCamera();
        // set camera properties
        // camera.Azimuth(20.0);
        // camera.Dolly(1.60);

        // initialize internals
        // processor = new InstanceProcessor();
        // processor.setDefaultThreadName("Canvas3D renderer");
        // // we want the processor to stay alive for sometime
        // processor.setKeepAliveTime(3, TimeUnit.SECONDS);

        // save lut and prepare for 3D visualization
        lutSave = seq.getDefaultLUT();
        final LUT lut = getLut();

        // save colormap
        saveColormap(lut);
        // adjust LUT alpha level for 3D view (this make lutChanged() to be called)
        setDefaultOpacity(lut);

        // initialize volume data
        imageVolume = new VtkImageVolume();
        // rebuild volume image
        updateImageData(getImageData());
        // setup volume scaling
        imageVolume.setScale(seq.getPixelSizeX(), seq.getPixelSizeY(), seq.getPixelSizeZ());
        // setup volume LUT
        imageVolume.setLUT(getLut());

        // initialize axe
        axes = new vtkAxesActor();
        widget = new vtkOrientationMarkerWidget();
        widget.SetOrientationMarker(axes);
        widget.SetInteractor(interactor);
        widget.SetViewport(0, 0, 0.3, 0.3);
        widget.SetEnabled(1);
        // not visible by default
        axes.SetVisibility(0);

        // initialize bounding box
        boundingBox = new vtkCubeAxesActor();
        boundingBox.SetBounds(imageVolume.getVolume().GetBounds());
        boundingBox.SetCamera(camera);
        // set bounding box labels properties
        boundingBox.SetFlyModeToStaticEdges();
        boundingBox.SetUseBounds(true);
        boundingBox.XAxisLabelVisibilityOff();
        boundingBox.XAxisMinorTickVisibilityOff();
        boundingBox.XAxisTickVisibilityOff();
        boundingBox.YAxisLabelVisibilityOff();
        boundingBox.YAxisMinorTickVisibilityOff();
        boundingBox.YAxisTickVisibilityOff();
        boundingBox.ZAxisLabelVisibilityOff();
        boundingBox.ZAxisMinorTickVisibilityOff();
        boundingBox.ZAxisTickVisibilityOff();
        // not visible by default
        boundingBox.SetVisibility(0);

        // initialize rules and box axis
        rulerBox = new vtkCubeAxesActor();
        rulerBox.SetBounds(imageVolume.getVolume().GetBounds());
        rulerBox.SetCamera(camera);
        // set bounding box labels properties
        rulerBox.GetTitleTextProperty(0).SetColor(1.0, 0.0, 0.0);
        rulerBox.GetLabelTextProperty(0).SetColor(1.0, 0.0, 0.0);
        rulerBox.GetXAxesGridlinesProperty().SetColor(1.0, 0.0, 0.0);
        rulerBox.GetXAxesGridpolysProperty().SetColor(1.0, 0.0, 0.0);
        rulerBox.GetXAxesInnerGridlinesProperty().SetColor(1.0, 0.0, 0.0);

        rulerBox.GetTitleTextProperty(1).SetColor(0.0, 1.0, 0.0);
        rulerBox.GetLabelTextProperty(1).SetColor(0.0, 1.0, 0.0);
        rulerBox.GetYAxesGridlinesProperty().SetColor(0.0, 1.0, 0.0);
        rulerBox.GetYAxesGridpolysProperty().SetColor(0.0, 1.0, 0.0);
        rulerBox.GetYAxesInnerGridlinesProperty().SetColor(0.0, 1.0, 0.0);

        rulerBox.GetTitleTextProperty(2).SetColor(0.0, 0.0, 1.0);
        rulerBox.GetLabelTextProperty(2).SetColor(0.0, 0.0, 1.0);
        rulerBox.GetZAxesGridlinesProperty().SetColor(0.0, 0.0, 1.0);
        rulerBox.GetZAxesGridpolysProperty().SetColor(0.0, 0.0, 1.0);
        rulerBox.GetZAxesInnerGridlinesProperty().SetColor(0.0, 0.0, 1.0);

        rulerBox.XAxisVisibilityOff();
        rulerBox.YAxisVisibilityOff();
        rulerBox.ZAxisVisibilityOff();

        rulerBox.SetGridLineLocation(VtkUtil.VTK_GRID_LINES_FURTHEST);
        rulerBox.SetFlyModeToOuterEdges();
        rulerBox.SetUseBounds(true);

        // not visible by default
        rulerBox.SetDrawXGridlines(0);
        rulerBox.SetDrawYGridlines(0);
        rulerBox.SetDrawZGridlines(0);
        rulerBox.SetXAxisTickVisibility(0);
        rulerBox.SetXAxisMinorTickVisibility(0);
        rulerBox.SetYAxisTickVisibility(0);
        rulerBox.SetYAxisMinorTickVisibility(0);
        rulerBox.SetZAxisTickVisibility(0);
        rulerBox.SetZAxisMinorTickVisibility(0);
        rulerBox.SetXAxisLabelVisibility(0);
        rulerBox.SetYAxisLabelVisibility(0);
        rulerBox.SetZAxisLabelVisibility(0);

        // add volume to renderer
        // TODO : add option to remove volume rendering
        renderer.AddVolume(imageVolume.getVolume());
        // add bounding box & ruler
        renderer.AddViewProp(boundingBox);
        renderer.AddViewProp(rulerBox);
        // then vtkPainter actors to the renderer
        for (Layer l : getLayers(false))
            addLayerActors(l);

        // reset camera
        resetCamera();

        // restore settings
        setBackgroundColor(new Color(preferences.getInt(ID_BGCOLOR, 0xFFFFFF)));
        setVolumeMapperType(VtkVolumeMapperType.values()[preferences.getInt(ID_MAPPER,
                VtkVolumeMapperType.RAYCAST_CPU_FIXEDPOINT.ordinal())]);
        setVolumeInterpolation(preferences.getInt(ID_INTERPOLATION, VtkUtil.VTK_LINEAR_INTERPOLATION));
        setVolumeBlendingMode(VtkVolumeBlendType.values()[preferences.getInt(ID_BLENDING,
                VtkVolumeBlendType.COMPOSITE.ordinal())]);
        setVolumeAmbient(preferences.getDouble(ID_AMBIENT, 0.5d));
        setVolumeDiffuse(preferences.getDouble(ID_DIFFUSE, 0.4d));
        setVolumeSpecular(preferences.getDouble(ID_SPECULAR, 0.4d));

        setAxisVisible(preferences.getBoolean(ID_AXES, true));
        setBoundingBoxVisible(preferences.getBoolean(ID_BOUNDINGBOX, true));
        setBoundingBoxGridVisible(preferences.getBoolean(ID_BOUNDINGBOX_GRID, true));
        setBoundingBoxRulerVisible(preferences.getBoolean(ID_BOUNDINGBOX_RULES, false));
        setBoundingBoxRulerLabelsVisible(preferences.getBoolean(ID_BOUNDINGBOX_LABELS, false));
        setVolumeShadingEnable(preferences.getBoolean(ID_SHADING, false));

        // create EDTTask object
        edtTask = new EDTTask<Object>();

        // start the properties updater thread
        propertiesUpdater.start();

        initialized = true;
    }

    @Override
    public void shutDown()
    {
        final long st = System.currentTimeMillis();
        // wait for initialization to complete before shutdown (max 5s)
        while (((System.currentTimeMillis() - st) < 5000L) && !initialized)
            ThreadUtil.sleep(10);

        super.shutDown();

        propertiesUpdater.interrupt();
        propertiesToUpdate.clear();

        // save settings
        preferences.putInt(ID_BGCOLOR, getBackgroundColor().getRGB());
        preferences.putBoolean(ID_BOUNDINGBOX, boundingBoxButton.isSelected());
        preferences.putBoolean(ID_BOUNDINGBOX_GRID, isBoundingBoxGridVisible());
        preferences.putBoolean(ID_BOUNDINGBOX_RULES, rulerButton.isSelected());
        preferences.putBoolean(ID_BOUNDINGBOX_LABELS, rulerLabelButton.isSelected());
        preferences.putBoolean(ID_AXES, axesButton.isSelected());
        preferences.putInt(ID_MAPPER, getVolumeMapperType().ordinal());
        preferences.putInt(ID_BLENDING, getVolumeBlendingMode().ordinal());
        preferences.putInt(ID_INTERPOLATION, getVolumeInterpolation());
        preferences.putBoolean(ID_SHADING, isVolumeShadingEnable());
        preferences.putDouble(ID_AMBIENT, getVolumeAmbient());
        preferences.putDouble(ID_DIFFUSE, getVolumeDiffuse());
        preferences.putDouble(ID_SPECULAR, getVolumeSpecular());

        // restore colormap
        restoreOpacity(lutSave, getLut());
        // restoreColormap(getLut());

        // AWTMultiCaster of vtkPanel keep reference of this frame so
        // we have to release as most stuff we can
        removeAll();
        panel.removeAll();

        // VTK need it
        invokeOnEDTSilent(new Runnable()
        {
            @Override
            public void run()
            {
                renderer.RemoveAllViewProps();
                renderer.Delete();
                renderWindow.Delete();
                imageVolume.release();
                widget.Delete();
                axes.Delete();
                boundingBox.Delete();
                camera.Delete();
            }
        });

        renderer = null;
        renderWindow = null;
        imageVolume = null;
        widget = null;
        axes = null;
        boundingBox = null;
        camera = null;

        panel3D = null;
        panel = null;
    }

    @Override
    public void customizeToolbar(JToolBar toolBar)
    {
        toolBar.addSeparator();
        toolBar.add(axesButton);
        toolBar.addSeparator();
        toolBar.add(boundingBoxButton);
        toolBar.add(gridButton);
        toolBar.add(rulerButton);
        toolBar.add(rulerLabelButton);
        toolBar.addSeparator();
        toolBar.add(shadingButton);
    }

    /**
     * Request exclusive access to VTK rendering.
     */
    public void lock()
    {
        if (panel3D != null)
            panel3D.lock();
    }

    /**
     * Release exclusive access from VTK rendering.
     */
    public void unlock()
    {
        if (panel3D != null)
            panel3D.unlock();
    }

    /**
     * @deprecated Use {@link #getCamera()} instead
     */
    @Deprecated
    public vtkCamera getActiveCam()
    {
        return getCamera();
    }

    /**
     * @return the VTK scene camera object
     */
    public vtkCamera getCamera()
    {
        return camera;
    }

    /**
     * @return the VTK default scene light object.<br>
     *         Can be <code>null</code> if render window is not yet initialized.
     */
    public vtkLight getLight()
    {
        return renderer.GetLights().GetNextItem();
    }

    /**
     * @return the VTK axes object
     */
    public vtkAxesActor getAxes()
    {
        return axes;
    }

    /**
     * @return the VTK bounding box object
     */
    public vtkCubeAxesActor getBoundingBox()
    {
        return boundingBox;
    }

    /**
     * @return the VTK ruler box object
     */
    public vtkCubeAxesActor getRulerBox()
    {
        return rulerBox;
    }

    /**
     * @return the VTK widget object used to display axes
     */
    public vtkOrientationMarkerWidget getWidget()
    {
        return widget;
    }

    /**
     * @return the VTK image volume object
     */
    public VtkImageVolume getImageVolume()
    {
        return imageVolume;
    }

    /**
     * Returns rendering background color
     */
    public Color getBackgroundColor()
    {
        return settingPanel.getBackgroundColor();
    }

    /**
     * Sets rendering background color
     */
    public void setBackgroundColor(Color value)
    {
        settingPanel.setBackgroundColor(value);
    }

    /**
     * Returns <code>true</code> if the volume bounding box is visible.
     */
    public boolean isBoundingBoxVisible()
    {
        return boundingBoxButton.isSelected();
    }

    /**
     * Enable / disable volume bounding box display.
     */
    public void setBoundingBoxVisible(boolean value)
    {
        if (boundingBoxButton.isSelected() != value)
            boundingBoxButton.doClick();
    }

    /**
     * Returns <code>true</code> if the volume bounding box grid is visible.
     */
    public boolean isBoundingBoxGridVisible()
    {
        return gridButton.isSelected();
    }

    /**
     * Enable / disable volume bounding box grid display.
     */
    public void setBoundingBoxGridVisible(boolean value)
    {
        if (gridButton.isSelected() != value)
            gridButton.doClick();
    }

    /**
     * Returns <code>true</code> if the volume bounding box ruler are visible.
     */
    public boolean isBoundingBoxRulerVisible()
    {
        return rulerButton.isSelected();
    }

    /**
     * Enable / disable volume bounding box ruler display.
     */
    public void setBoundingBoxRulerVisible(boolean value)
    {
        if (rulerButton.isSelected() != value)
            rulerButton.doClick();
    }

    /**
     * Returns <code>true</code> if the volume bounding box ruler labels are visible.
     */
    public boolean isBoundingBoxRulerLabelsVisible()
    {
        return rulerLabelButton.isSelected();
    }

    /**
     * Enable / disable volume bounding box ruler labels display.
     */
    public void setBoundingBoxRulerLabelsVisible(boolean value)
    {
        if (rulerLabelButton.isSelected() != value)
            rulerLabelButton.doClick();
    }

    /**
     * Enable / disable volume bounding box ruler display.
     */
    public void setBoundingBoxColor(Color color)
    {
        final float[] comp = color.getRGBColorComponents(null);

        final float r = comp[0];
        final float g = comp[0];
        final float b = comp[0];

        boundingBox.GetXAxesLinesProperty().SetColor(r, g, b);
        boundingBox.GetYAxesLinesProperty().SetColor(r, g, b);
        boundingBox.GetZAxesLinesProperty().SetColor(r, g, b);

        rulerBox.GetXAxesGridlinesProperty().SetColor(r, g, b);
        rulerBox.GetXAxesGridpolysProperty().SetColor(r, g, b);
        rulerBox.GetXAxesInnerGridlinesProperty().SetColor(r, g, b);
        rulerBox.GetXAxesLinesProperty().SetColor(r, g, b);

        rulerBox.GetYAxesGridlinesProperty().SetColor(r, g, b);
        rulerBox.GetYAxesGridpolysProperty().SetColor(r, g, b);
        rulerBox.GetYAxesInnerGridlinesProperty().SetColor(r, g, b);
        rulerBox.GetYAxesLinesProperty().SetColor(r, g, b);

        rulerBox.GetZAxesGridlinesProperty().SetColor(r, g, b);
        rulerBox.GetZAxesGridpolysProperty().SetColor(r, g, b);
        rulerBox.GetZAxesInnerGridlinesProperty().SetColor(r, g, b);
        rulerBox.GetZAxesLinesProperty().SetColor(r, g, b);
    }

    /**
     * Returns <code>true</code> if the 3D axis are visible.
     */
    public boolean isAxisVisible()
    {
        return axesButton.isSelected();
    }

    /**
     * Enable / disable 3D axis display.
     */
    public void setAxisVisible(boolean value)
    {
        if (axesButton.isSelected() != value)
            axesButton.doClick();
    }

    /**
     * @see VtkImageVolume#getBlendingMode()
     */
    public VtkVolumeBlendType getVolumeBlendingMode()
    {
        return settingPanel.getVolumeBlendingMode();
    }

    /**
     * @see VtkImageVolume#setBlendingMode(VtkVolumeBlendType)
     */
    public void setVolumeBlendingMode(VtkVolumeBlendType value)
    {
        settingPanel.setVolumeBlendingMode(value);
    }

    /**
     * @see VtkImageVolume#getSampleResolution()
     */
    public int getVolumeSample()
    {
        return settingPanel.getVolumeSample();
    }

    /**
     * @see VtkImageVolume#setSampleResolution(double)
     */
    public void setVolumeSample(int value)
    {
        settingPanel.setVolumeSample(value);
    }

    /**
     * @see VtkImageVolume#getShade()
     */
    public boolean isVolumeShadingEnable()
    {
        return shadingButton.isSelected();
    }

    /**
     * @see VtkImageVolume#setShade(boolean)
     */
    public void setVolumeShadingEnable(boolean value)
    {
        if (shadingButton.isSelected() != value)
            shadingButton.doClick();
    }

    /**
     * @see VtkImageVolume#getAmbient()
     */
    public double getVolumeAmbient()
    {
        return settingPanel.getVolumeAmbient();
    }

    /**
     * @see VtkImageVolume#setAmbient(double)
     */
    public void setVolumeAmbient(double value)
    {
        settingPanel.setVolumeAmbient(value);
    }

    /**
     * @see VtkImageVolume#getDiffuse()
     */
    public double getVolumeDiffuse()
    {
        return settingPanel.getVolumeDiffuse();
    }

    /**
     * @see VtkImageVolume#setDiffuse(double)
     */
    public void setVolumeDiffuse(double value)
    {
        settingPanel.setVolumeDiffuse(value);
    }

    /**
     * @see VtkImageVolume#getSpecular()
     */
    public double getVolumeSpecular()
    {
        return settingPanel.getVolumeSpecular();
    }

    /**
     * @see VtkImageVolume#setSpecular(double)
     */
    public void setVolumeSpecular(double value)
    {
        settingPanel.setVolumeSpecular(value);
    }

    /**
     * @see VtkImageVolume#getInterpolationMode()
     */
    public int getVolumeInterpolation()
    {
        return settingPanel.getVolumeInterpolation();
    }

    /**
     * @see VtkImageVolume#setInterpolationMode(int)
     */
    public void setVolumeInterpolation(int value)
    {
        settingPanel.setVolumeInterpolation(value);
    }

    /**
     * @see VtkImageVolume#getVolumeMapperType()
     */
    public VtkVolumeMapperType getVolumeMapperType()
    {
        return settingPanel.getVolumeMapperType();
    }

    /**
     * @see VtkImageVolume#setVolumeMapperType(VtkVolumeMapperType)
     */
    public void setVolumeMapperType(VtkVolumeMapperType value)
    {
        settingPanel.setVolumeMapperType(value);
    }

    /**
     * @return visible state of the image volume object
     * @see VtkImageVolume#isVisible()
     */
    public boolean isVolumeVisible()
    {
        return imageVolume.isVisible();
    }

    /**
     * Sets the visible state of the image volume object
     * 
     * @see VtkImageVolume#setVisible(boolean)
     */
    public void setVolumeVisible(boolean value)
    {
        imageVolume.setVisible(value);
    }

    /**
     * Force render refresh
     */
    @Override
    public void refresh()
    {
        if (!initialized)
            return;

        // refresh rendering
        if (panel3D != null)
            panel3D.repaint();
    }

    // private void test()
    // {
    // // exemple de clipping a utiliser par la suite.
    // if ( false )
    // {
    // vtkPlane plane = new vtkPlane();
    // plane.SetOrigin(1000, 1000, 1000);
    // plane.SetNormal( 1, 1, 0);
    // volumeMapper.AddClippingPlane( plane );
    // }
    //
    // vtkOrientationMarkerWidget ow = new vtkOrientationMarkerWidget();
    // }

    protected void resetCamera()
    {
        camera.SetViewUp(0, -1, 0);
        renderer.ResetCamera();
        camera.Elevation(180);
        renderer.ResetCameraClippingRange();
    }

    /**
     * @deprecated Use {@link #setVolumeSample(int)} instead
     */
    @Deprecated
    @Override
    public void setVolumeDistanceSample(int value)
    {
        setVolumeSample(value);
    }

    /**
     * Returns channel position based on enabled channel in LUT
     * 
     * @return
     */
    protected int getChannelPos()
    {
        final LUT lut = getLut();
        int result = -1;

        for (int c = 0; c < lut.getNumChannel(); c++)
        {
            final LUTChannel lutChannel = lut.getLutChannel(c);

            if (lutChannel.isEnabled())
            {
                if (result == -1)
                    result = c;
                else
                    return -1;
            }
        }

        return result;
    }

    /**
     * @see icy.vtk.IcyVtkPanel#getPicker()
     */
    public vtkPropPicker getPicker()
    {
        return panel3D.getPicker();
    }

    /**
     * @see icy.vtk.IcyVtkPanel#pick(int, int)
     */
    public vtkActor pick(int x, int y)
    {
        return panel3D.pick(x, y);
    }

    protected void setDefaultOpacity(LUT lut)
    {
        lut.beginUpdate();
        try
        {
            for (LUTChannel lutChannel : lut.getLutChannels())
                lutChannel.getColorMap().setDefaultAlphaFor3D();
        }
        finally
        {
            lut.endUpdate();
        }
    }

    protected void restoreOpacity(LUT srcLut, LUT dstLut)
    {
        final int numComp = Math.min(srcLut.getNumChannel(), dstLut.getNumChannel());

        for (int c = 0; c < numComp; c++)
            retoreOpacity(srcLut.getLutChannel(c), dstLut.getLutChannel(c));
    }

    protected void retoreOpacity(LUTChannel srcLutBand, LUTChannel dstLutBand)
    {
        dstLutBand.getColorMap().alpha.copyFrom(srcLutBand.getColorMap().alpha);
    }

    protected void restoreColormap(LUT lut)
    {
        lut.setScalers(lutSave);
        lut.setColorMaps(lutSave, true);
    }

    protected void saveColormap(LUT lut)
    {
        lutSave.setScalers(lut);
        lutSave.setColorMaps(lut, true);
    }

    protected vtkProp[] getLayerActors(Layer layer)
    {
        if (layer != null)
        {
            // add painter actor from the vtk render
            final Overlay overlay = layer.getOverlay();

            if (overlay instanceof VtkPainter)
                return ((VtkPainter) overlay).getProps();
        }

        return new vtkProp[0];
    }

    protected void addLayerActors(Layer layer)
    {
        final vtkProp[] props = getLayerActors(layer);

        invokeOnEDTSilent(new Runnable()
        {
            @Override
            public void run()
            {
                for (vtkProp actor : props)
                    VtkUtil.addProp(renderer, actor);
            }
        });
    }

    protected void removeLayerActors(Layer layer)
    {
        final vtkProp[] props = getLayerActors(layer);

        invokeOnEDTSilent(new Runnable()
        {
            @Override
            public void run()
            {
                for (vtkProp actor : props)
                    VtkUtil.removeProp(renderer, actor);
            }
        });
    }

    protected void updateBoundingBoxSize()
    {
        boundingBox.SetBounds(imageVolume.getVolume().GetBounds());
        rulerBox.SetBounds(imageVolume.getVolume().GetBounds());
    }

    /**
     * Build and get image data
     */
    protected vtkImageData getImageData()
    {
        final Sequence sequence = getSequence();
        if ((sequence == null) || sequence.isEmpty())
            return null;

        final int posT = getPositionT();
        final int posC = getPositionC();

        final Object data;

        if (posC == -1)
        {
            data = sequence.getDataCopyCXYZ(posT);
            return VtkUtil.getImageData(data, sequence.getDataType_(), sequence.getSizeX(), sequence.getSizeY(),
                    sequence.getSizeZ(), sequence.getSizeC());
        }

        data = sequence.getDataCopyXYZ(posT, posC);
        return VtkUtil.getImageData(data, sequence.getDataType_(), sequence.getSizeX(), sequence.getSizeY(),
                sequence.getSizeZ(), 1);
    }

    /**
     * update image data
     */
    protected void updateImageData(vtkImageData data)
    {
        if (data != null)
        {
            imageVolume.setVolumeData(data);
            imageVolume.getVolume().SetVisibility(1);
        }
        else
            // no data --> hide volume
            imageVolume.getVolume().SetVisibility(0);
    }

    protected void updateLut()
    {
        final LUT lut = getLut();
        final int posC = getPositionC();

        // multi channel volume rendering mapper ?
        if (imageVolume.isMultiChannelVolumeMapper())
        {
            // update the whole LUT
            for (int c = 0; c < lut.getNumChannel(); c++)
                updateLut(lut.getLutChannel(c), c);
        }
        // single channel mapper, always set channel 0
        else if (posC != -1)
            updateLut(lut.getLutChannel(posC), 0);
    }

    protected void updateLut(LUTChannel lutChannel, int channel)
    {
        final Sequence sequence = getSequence();
        if ((sequence == null) || sequence.isEmpty())
            return;

        final int ch = channel;
        final vtkColorTransferFunction colorMap = VtkUtil.getColorMap(lutChannel);
        final vtkPiecewiseFunction opacityMap = VtkUtil.getOpacityMap(lutChannel);

        imageVolume.setColorMap(colorMap, ch);
        imageVolume.setOpacityMap(opacityMap, ch);
    }

    @Override
    public Component getViewComponent()
    {
        return panel3D;
    }

    @Override
    public vtkPanel getPanel3D()
    {
        return panel3D;
    }

    @Override
    public vtkRenderer getRenderer()
    {
        return renderer;
    }

    public vtkRenderWindow getRenderWindow()
    {
        return renderWindow;
    }

    /**
     * Get scaling for image volume rendering
     */
    @Override
    public double[] getVolumeScale()
    {
        return imageVolume.getScale();
    }

    /**
     * Set scaling for image volume rendering
     */
    @Override
    public void setVolumeScale(double x, double y, double z)
    {
        propertyChange(PROPERTY_SCALE, new double[] {x, y, z});
    }

    @Override
    public double getMouseImagePosX()
    {
        // not supported
        return 0d;
    }

    @Override
    public double getMouseImagePosY()
    {
        // not supported
        return 0d;
    }

    @Override
    public double getMouseImagePosZ()
    {
        // not supported
        return 0d;
    }

    @Override
    public double getMouseImagePosT()
    {
        // not supported
        return 0d;
    }

    @Override
    public double getMouseImagePosC()
    {
        // not supported
        return 0d;
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        // send to overlays
        super.keyPressed(e);

        // forward to view
        panel3D.keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        // send to overlays
        super.keyReleased(e);

        // forward to view
        panel3D.keyReleased(e);
    }

    @Override
    protected void setPositionZInternal(int z)
    {
        // not supported, Z should stay at -1
    }

    @Override
    public BufferedImage getRenderedImage(int t, int c)
    {
        // save position
        final int prevT = getPositionT();
        final int prevC = getPositionC();

        // set wanted position (needed for correct overlay drawing)
        // we have to fire events else some stuff can miss the change
        setPositionT(t);
        setPositionC(c);
        try
        {
            final vtkRenderWindow renderWindow = renderer.GetRenderWindow();
            final int[] size = renderWindow.GetSize();
            final int w = size[0];
            final int h = size[1];
            final vtkUnsignedCharArray array = new vtkUnsignedCharArray();
            final vtkImageData imageData = getImageData();

            // VTK need this to be called in the EDT
            invokeOnEDTSilent(new Runnable()
            {
                @Override
                public void run()
                {
                    // set image data
                    updateImageData(imageData);

                    // render now !
                    panel3D.paint(panel3D.getGraphics());

                    // NOTE: in vtk the [0,0] pixel is bottom left, so a vertical flip is required
                    // NOTE: GetRGBACharPixelData gives problematic results depending on the
                    // platform
                    // (see comment about alpha and platform-dependence in the doc for
                    // vtkWindowToImageFilter)
                    // Since the canvas is opaque, simply use GetPixelData.
                    renderWindow.GetPixelData(0, 0, w - 1, h - 1, 1, array);
                }
            });

            // convert the vtk array into a IcyBufferedImage
            final byte[] inData = array.GetJavaArray();
            final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            final int[] outData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

            int inOffset = 0;
            for (int y = h - 1; y >= 0; y--)
            {
                int outOffset = y * w;

                for (int x = 0; x < w; x++)
                {
                    final int r = TypeUtil.unsign(inData[inOffset++]);
                    final int g = TypeUtil.unsign(inData[inOffset++]);
                    final int b = TypeUtil.unsign(inData[inOffset++]);

                    outData[outOffset++] = (r << 16) | (g << 8) | (b << 0);
                }
            }

            return image;
        }
        finally
        {
            // restore position
            setPositionT(prevT);
            setPositionC(prevC);
        }
    }

    @Override
    public BufferedImage getRenderedImage(int t, int z, int c, boolean canvasView)
    {
        if (z != -1)
            throw new UnsupportedOperationException(
                    "Error: getRenderedImage(..) with z != -1 not supported on Canvas3D.");
        if (!canvasView)
            System.out.println("Warning: getRenderedImage(..) with canvasView = false not supported on Canvas3D.");

        return getRenderedImage(t, c);
    }

    @Override
    public void run()
    {
        while (!propertiesUpdater.isInterrupted())
        {
            Property prop;

            try
            {
                prop = propertiesToUpdate.take();

                final String name = prop.name;
                final Object value = prop.value;

                if (StringUtil.equals(name, VtkSettingPanel.PROPERTY_AMBIENT))
                {
                    final double d = ((Double) value).doubleValue();

                    invokeOnEDT(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            imageVolume.setAmbient(d);
                        }
                    });

                    preferences.putDouble(ID_AMBIENT, d);
                }
                else if (StringUtil.equals(name, VtkSettingPanel.PROPERTY_DIFFUSE))
                {
                    final double d = ((Double) value).doubleValue();

                    invokeOnEDT(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            imageVolume.setDiffuse(d);
                        }
                    });

                    preferences.putDouble(ID_DIFFUSE, d);
                }
                else if (StringUtil.equals(name, VtkSettingPanel.PROPERTY_SPECULAR))
                {
                    final double d = ((Double) value).doubleValue();

                    invokeOnEDT(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            imageVolume.setSpecular(d);
                        }
                    });

                    preferences.putDouble(ID_SPECULAR, d);
                }
                else if (StringUtil.equals(name, VtkSettingPanel.PROPERTY_BG_COLOR))
                {
                    final Color color = (Color) value;
                    final double[] colorArray = Array1DUtil.floatArrayToDoubleArray(color.getColorComponents(null));

                    invokeOnEDT(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            renderer.SetBackground(colorArray);

                            // adjust bounding box color
                            if (ColorUtil.getLuminance(color) > 128)
                                setBoundingBoxColor(Color.black);
                            else
                                setBoundingBoxColor(Color.white);
                        }
                    });

                    preferences.putInt(ID_BGCOLOR, color.getRGB());
                }
                else if (StringUtil.equals(name, VtkSettingPanel.PROPERTY_INTERPOLATION))
                {
                    final int i = ((Integer) value).intValue();

                    invokeOnEDT(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            imageVolume.setInterpolationMode(i);
                        }
                    });

                    preferences.putDouble(ID_INTERPOLATION, i);
                }
                else if (StringUtil.equals(name, VtkSettingPanel.PROPERTY_MAPPER))
                {
                    final VtkVolumeMapperType type = (VtkVolumeMapperType) value;
                    final boolean prevMC = imageVolume.isMultiChannelVolumeMapper();

                    invokeOnEDT(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            imageVolume.setVolumeMapperType(type);

                            // FIXME: this line actually make VTK to crash
                            // mapper not supported ? --> switch back to default one
                            // if (!sequenceVolume.isMapperSupported(renderer))
                            // sequenceVolume.setVolumeMapperType(VtkVolumeMapperType.RAYCAST_CPU_FIXEDPOINT);

                            // blending mode can change when mapper changed
                            setVolumeBlendingMode(imageVolume.getBlendingMode());

                            final boolean newMC = imageVolume.isMultiChannelVolumeMapper();
                            if (prevMC != newMC)
                            {
                                // changed to multi channel mapper --> display all channel
                                if (newMC)
                                    setPositionC(-1);
                                // changed to single channel mapper ?
                                else
                                {
                                    // find channel pos from enabled channel
                                    final int c = getChannelPos();

                                    if (c == -1)
                                        setPositionC(0);
                                    else
                                    {
                                        // this won't do any LUT change event so do it manually
                                        setPositionC(c);
                                        updateLut();
                                    }
                                }
                            }
                        }
                    });

                    preferences.putInt(ID_MAPPER, type.ordinal());
                }
                else if (StringUtil.equals(name, VtkSettingPanel.PROPERTY_BLENDING))
                {
                    final VtkVolumeBlendType type = (VtkVolumeBlendType) value;

                    invokeOnEDT(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            imageVolume.setBlendingMode(type);
                        }
                    });

                    preferences.putInt(ID_BLENDING, getVolumeBlendingMode().ordinal());
                }
                else if (StringUtil.equals(name, VtkSettingPanel.PROPERTY_SAMPLE))
                {
                    final int i = ((Integer) value).intValue();

                    invokeOnEDT(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            imageVolume.setSampleResolution(i);
                        }
                    });

                    preferences.putDouble(ID_SAMPLE, i);
                }
                else if (StringUtil.equals(name, PROPERTY_AXES))
                {
                    final boolean b = ((Boolean) value).booleanValue();

                    invokeOnEDT(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            axes.SetVisibility(b ? 1 : 0);
                        }
                    });

                    preferences.putBoolean(ID_AXES, b);
                }
                else if (StringUtil.equals(name, PROPERTY_BOUNDINGBOX))
                {
                    final boolean b = ((Boolean) value).booleanValue();

                    invokeOnEDT(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            boundingBox.SetVisibility(b ? 1 : 0);
                        }
                    });

                    preferences.putBoolean(ID_BOUNDINGBOX, b);
                }
                else if (StringUtil.equals(name, PROPERTY_BOUNDINGBOX_GRID))
                {
                    final boolean b = ((Boolean) value).booleanValue();

                    invokeOnEDT(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            rulerBox.SetDrawXGridlines(b ? 1 : 0);
                            rulerBox.SetDrawYGridlines(b ? 1 : 0);
                            rulerBox.SetDrawZGridlines(b ? 1 : 0);
                        }
                    });

                    preferences.putBoolean(ID_BOUNDINGBOX_GRID, b);
                }
                else if (StringUtil.equals(name, PROPERTY_BOUNDINGBOX_RULES))
                {
                    final boolean b = ((Boolean) value).booleanValue();

                    invokeOnEDT(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            rulerBox.SetXAxisTickVisibility(b ? 1 : 0);
                            rulerBox.SetXAxisMinorTickVisibility(b ? 1 : 0);
                            rulerBox.SetYAxisTickVisibility(b ? 1 : 0);
                            rulerBox.SetYAxisMinorTickVisibility(b ? 1 : 0);
                            rulerBox.SetZAxisTickVisibility(b ? 1 : 0);
                            rulerBox.SetZAxisMinorTickVisibility(b ? 1 : 0);
                        }
                    });

                    preferences.putBoolean(ID_BOUNDINGBOX_RULES, b);
                }
                else if (StringUtil.equals(name, PROPERTY_BOUNDINGBOX_LABELS))
                {
                    final boolean b = ((Boolean) value).booleanValue();

                    invokeOnEDT(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            rulerBox.SetXAxisLabelVisibility(b ? 1 : 0);
                            rulerBox.SetYAxisLabelVisibility(b ? 1 : 0);
                            rulerBox.SetZAxisLabelVisibility(b ? 1 : 0);
                        }
                    });

                    preferences.putBoolean(ID_BOUNDINGBOX_LABELS, b);
                }
                else if (StringUtil.equals(name, PROPERTY_SHADING))
                {
                    final boolean b = ((Boolean) value).booleanValue();

                    invokeOnEDT(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            imageVolume.setShade(b);
                        }
                    });

                    preferences.putBoolean(ID_SHADING, b);
                }
                else if (StringUtil.equals(name, PROPERTY_LUT))
                {
                    updateLut();
                }
                else if (StringUtil.equals(name, PROPERTY_SCALE))
                {
                    final double[] oldScale = getVolumeScale();
                    final double[] newScale = (double[]) value;

                    if (!Arrays.equals(oldScale, newScale))
                    {
                        invokeOnEDT(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                imageVolume.setScale(newScale);
                                // need to update bounding box as well
                                updateBoundingBoxSize();
                            }
                        });
                    }
                }
                else if (StringUtil.equals(name, PROPERTY_DATA))
                {
                    final vtkImageData data = getImageData();

                    invokeOnEDT(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            // set image data
                            updateImageData(data);
                        }
                    });
                }
                else if (StringUtil.equals(name, PROPERTY_BOUNDS))
                {
                    invokeOnEDT(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            updateBoundingBoxSize();
                        }
                    });
                }
            }
            catch (InterruptedException e)
            {
                // just end process
                return;
            }

            // need to refresh rendering
            if (propertiesToUpdate.isEmpty())
                refresh();
        }
    }

    protected void invokeOnEDT(Runnable task) throws InterruptedException
    {
        // in initialization --> just execute
        if (edtTask == null)
        {
            task.run();
            return;
        }

        edtTask.setTask(task);

        try
        {
            ThreadUtil.invokeNow(edtTask);
        }
        catch (InterruptedException e)
        {
            throw e;
        }
        catch (Exception t)
        {
            // just ignore as this is async process
        }
    }

    protected void invokeOnEDTSilent(Runnable task)
    {
        try
        {
            invokeOnEDT(task);
        }
        catch (InterruptedException e)
        {
            // just ignore
        }
    }

    @Override
    public void changed(IcyCanvasEvent event)
    {
        super.changed(event);

        // avoid useless process during canvas initialization
        if (!initialized)
            return;

        if (event.getType() == IcyCanvasEventType.POSITION_CHANGED)
        {
            switch (event.getDim())
            {
                case C:
                    propertyChange(PROPERTY_DATA, null);
                    break;

                case T:
                    propertyChange(PROPERTY_DATA, null);
                    break;

                case Z:
                    // shouldn't happen
                    break;
            }
        }
    }

    @Override
    protected void lutChanged(int channel)
    {
        super.lutChanged(channel);

        // avoid useless process during canvas initialization
        if (!initialized)
            return;

        propertyChange(PROPERTY_LUT, Integer.valueOf(channel));
    }

    @Override
    protected void sequenceOverlayChanged(Overlay overlay, SequenceEventType type)
    {
        super.sequenceOverlayChanged(overlay, type);

        if (!initialized)
            return;

        // refresh
        refresh();
    }

    @Override
    protected void sequenceDataChanged(IcyBufferedImage image, SequenceEventType type)
    {
        super.sequenceDataChanged(image, type);

        // rebuild image data and bounds
        propertyChange(PROPERTY_DATA, null);
        propertyChange(PROPERTY_BOUNDS, null);
    }

    @Override
    protected void sequenceMetaChanged(String metadataName)
    {
        super.sequenceMetaChanged(metadataName);

        final Sequence sequence = getSequence();
        if ((sequence == null) || sequence.isEmpty())
            return;

        // need to set scale ?
        if (StringUtil.isEmpty(metadataName)
                || (StringUtil.equals(metadataName, Sequence.ID_PIXEL_SIZE_X)
                        || StringUtil.equals(metadataName, Sequence.ID_PIXEL_SIZE_Y) || StringUtil.equals(metadataName,
                        Sequence.ID_PIXEL_SIZE_Z)))
        {
            setVolumeScale(sequence.getPixelSizeX(), sequence.getPixelSizeY(), sequence.getPixelSizeZ());
        }
    }

    @Override
    protected void layerChanged(CanvasLayerEvent event)
    {
        super.layerChanged(event);

        if (!initialized)
            return;

        // layer visibility property modified ?
        if ((event.getType() == LayersEventType.CHANGED) && Layer.isPaintProperty(event.getProperty()))
        {
            // TODO: refresh actor properties from layers properties (alpha and visible)
            // refresh();
        }
    }

    @Override
    protected void layerAdded(Layer layer)
    {
        super.layerAdded(layer);

        addLayerActors(layer);
    }

    @Override
    protected void layerRemoved(Layer layer)
    {
        super.layerRemoved(layer);

        removeLayerActors(layer);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        final Object source = e.getSource();

        // translate button action to property change event
        if (source == axesButton)
            propertyChange(PROPERTY_AXES, Boolean.valueOf(axesButton.isSelected()));
        else if (source == boundingBoxButton)
            propertyChange(PROPERTY_BOUNDINGBOX, Boolean.valueOf(boundingBoxButton.isSelected()));
        else if (source == gridButton)
            propertyChange(PROPERTY_BOUNDINGBOX_GRID, Boolean.valueOf(gridButton.isSelected()));
        else if (source == rulerButton)
            propertyChange(PROPERTY_BOUNDINGBOX_RULES, Boolean.valueOf(rulerButton.isSelected()));
        else if (source == rulerLabelButton)
            propertyChange(PROPERTY_BOUNDINGBOX_LABELS, Boolean.valueOf(rulerLabelButton.isSelected()));
        else if (source == shadingButton)
            propertyChange(PROPERTY_SHADING, Boolean.valueOf(shadingButton.isSelected()));
    }

    protected void propertyChange(String name, Object value)
    {
        final Property prop = new Property(name, value);

        // add the property to update list if not already present
        if (!propertiesToUpdate.contains(prop))
            propertiesToUpdate.add(prop);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        propertyChange(evt.getPropertyName(), evt.getNewValue());
    }

    protected class EDTTask<T> implements Callable<T>
    {
        protected Runnable task;

        public void setTask(Runnable task)
        {
            this.task = task;
        }

        @Override
        public T call() throws Exception
        {
            task.run();

            return null;
        }
    }

    protected class CustomVtkPanel extends IcyVtkPanel
    {
        /**
         * 
         */
        private static final long serialVersionUID = -7399887230624608711L;

        public CustomVtkPanel()
        {
            super();

            // key events should be forwarded from the viewer
            removeKeyListener(this);
        }

        @Override
        public void paint(Graphics g)
        {
            // call paint on overlays first
            if (isLayersVisible())
            {
                final List<Layer> layers = getLayers(true);
                final Layer imageLayer = getImageLayer();
                final Sequence seq = getSequence();

                // call paint in inverse order to have first overlay "at top"
                for (int i = layers.size() - 1; i >= 0; i--)
                {
                    final Layer layer = layers.get(i);

                    // don't call paint on the image layer
                    if (layer != imageLayer)
                        paintLayer(seq, layer);
                }
            }

            // then do 3D rendering
            super.paint(g);
        }

        /**
         * Draw specified layer
         */
        protected void paintLayer(Sequence seq, Layer layer)
        {
            if (layer.isVisible())
                layer.getOverlay().paint(null, seq, VtkCanvas.this);
        }

        @Override
        public void mouseEntered(MouseEvent e)
        {
            // send mouse event to overlays
            VtkCanvas.this.mouseEntered(e, null);

            super.mouseEntered(e);
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
            // send mouse event to overlays
            VtkCanvas.this.mouseExited(e, null);

            super.mouseExited(e);
        }

        @Override
        public void mouseClicked(MouseEvent e)
        {
            // send mouse event to overlays
            VtkCanvas.this.mouseClick(e, null);

            super.mouseClicked(e);
        }

        @Override
        public void mouseMoved(MouseEvent e)
        {
            // send mouse event to overlays
            VtkCanvas.this.mouseMove(e, null);

            super.mouseMoved(e);
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            // send mouse event to overlays
            VtkCanvas.this.mouseDrag(e, null);

            super.mouseDragged(e);
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
            // send mouse event to overlays
            VtkCanvas.this.mousePressed(e, null);

            super.mousePressed(e);
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            // send mouse event to overlays
            VtkCanvas.this.mouseReleased(e, null);

            super.mouseReleased(e);
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e)
        {
            // send mouse event to overlays
            VtkCanvas.this.mouseWheelMoved(e, null);

            super.mouseWheelMoved(e);
        }
    }
}
