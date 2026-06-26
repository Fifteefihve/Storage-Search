import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.geom.RoundRectangle2D;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainWindow extends JFrame
{

    private static final Color BG_DARK = new Color (80, 80, 80);
    private static final Color BG_PANEL = new Color (70, 70, 70);
    private static final Color BG_BOTTOM = new Color (55, 55, 55);
    private static final Color COL_LABEL = new Color (180, 130, 200);
    private static final Color COL_VALUE = new Color (80, 210, 180);
    private static final Color COL_ADD = new Color (80, 210, 80);
    private static final Color COL_RELOAD = new Color (210, 200, 80);
    private static final Color COL_STATS = new Color (200, 200, 200);
    private static final Color COL_TREE = new Color (220, 220, 220);
    private static final String DEFAULT_EXCEL = "components.xlsx";

    private final Map<String, java.util.List<Map<String, String>>> data = new LinkedHashMap<> ();
    private final String excelPath;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private JTree tree;
    private JPanel fieldsPanel;
    private JLabel titleLabel;
    private JLabel statsLabel;
    private JPanel panel1;

    public MainWindow (String excelPath)
    {
        this.excelPath = excelPath;
        loadFromExcel ();
        buildUI ();
    }

    private void loadFromExcel ()
    {
        data.clear ();
        File file = new File (excelPath);

        if (! file.exists ())
        {
            JOptionPane.showMessageDialog (null,
                    "Excel file not found:\n" + file.getAbsolutePath ()
                            + "\n\nStarting empty. Use 'add +' to add entries manually,\n"
                            + "or place your components.xlsx at the above path and click reload.",
                    "File Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (FileInputStream fis = new FileInputStream (file);
             Workbook workbook = new XSSFWorkbook (fis))
        {

            DataFormatter formatter = new DataFormatter ();

            for (int si = 0; si < workbook.getNumberOfSheets (); si++)
            {
                Sheet sheet = workbook.getSheetAt (si);
                String category = sheet.getSheetName ().toLowerCase ().trim ();

                // Find header row (row 0)
                Row headerRow = sheet.getRow (0);
                if (headerRow == null) continue;

                java.util.List<String> headers = new ArrayList<> ();
                for (Cell cell : headerRow)
                {
                    String h = formatter.formatCellValue (cell).trim ();
                    headers.add (h.isEmpty () ? "Col" + (cell.getColumnIndex () + 1) : h);
                }
                if (headers.isEmpty ()) continue;

                java.util.List<Map<String, String>> parts = new ArrayList<> ();
                for (int r = 1; r <= sheet.getLastRowNum (); r++)
                {
                    Row row = sheet.getRow (r);
                    if (row == null) continue;

                    Map<String, String> part = new LinkedHashMap<> ();
                    boolean hasContent = false;
                    for (int c = 0; c < headers.size (); c++)
                    {
                        Cell cell = row.getCell (c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        String val = cell != null ? formatter.formatCellValue (cell).trim () : "";
                        if (! val.isEmpty ()) hasContent = true;
                        part.put (headers.get (c), val);
                    }
                    if (hasContent) parts.add (part);
                }

                data.put (category, parts);
            }

        } catch (Exception ex)
        {
            JOptionPane.showMessageDialog (null,
                    "Could not read Excel file:\n" + ex.getMessage (),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buildUI ()
    {
        setTitle ("Inventory Search");
        setDefaultCloseOperation (EXIT_ON_CLOSE);
        setSize (1064, 1069);
        setLocationRelativeTo (null);

        JPanel root = new JPanel (new BorderLayout ());
        root.setBackground (BG_DARK);

        JSplitPane split = new JSplitPane (JSplitPane.HORIZONTAL_SPLIT,
                buildTreePanel (), buildDetailPanel ());
        split.setDividerLocation (520);
        split.setDividerSize (4);
        split.setBorder (null);

        root.add (split, BorderLayout.CENTER);
        root.add (buildStatsPanel (), BorderLayout.SOUTH);
        setContentPane (root);
        updateStats ();
    }

    private JPanel buildTreePanel ()
    {
        JPanel panel = new JPanel (new BorderLayout ());
        panel.setBackground (BG_PANEL);

        JPanel bar = new JPanel (new FlowLayout (FlowLayout.RIGHT, 10, 6));
        bar.setBackground (BG_PANEL);

        JButton reloadBtn = styledBtn ("↺ reload", COL_RELOAD, 18);
        reloadBtn.addActionListener (e -> onReload ());
        bar.add (reloadBtn);

        JButton addBtn = styledBtn ("add  +", COL_ADD, 18);
        addBtn.addActionListener (e -> onAdd ());
        bar.add (addBtn);

        panel.add (bar, BorderLayout.NORTH);

        rootNode = new DefaultMutableTreeNode ("Components");
        treeModel = new DefaultTreeModel (rootNode);
        rebuildNodes ();

        tree = new JTree (treeModel);
        tree.setBackground (BG_PANEL);
        tree.setFont (new Font ("SansSerif", Font.PLAIN, 16));
        tree.setBorder (new EmptyBorder (4, 8, 4, 4));
        tree.setRowHeight (28);
        tree.setRootVisible (true);
        tree.setShowsRootHandles (true);

        tree.setCellRenderer (new DefaultTreeCellRenderer ()
        {{
            setBackgroundNonSelectionColor (BG_PANEL);
            setBackgroundSelectionColor (new Color (60, 60, 90));
            setTextNonSelectionColor (COL_TREE);
            setTextSelectionColor (Color.WHITE);
            setBorderSelectionColor (new Color (60, 60, 90));
        }});

        tree.addTreeSelectionListener (this::onSelect);

        JScrollPane scroll = new JScrollPane (tree);
        scroll.setBorder (null);
        scroll.getViewport ().setBackground (BG_PANEL);
        panel.add (scroll, BorderLayout.CENTER);

        for (int i = 0; i < tree.getRowCount (); i++) tree.expandRow (i);

        return panel;
    }

    private JPanel buildDetailPanel ()
    {
        JPanel panel = new JPanel (new BorderLayout ());
        panel.setBackground (BG_DARK);
        panel.setBorder (new EmptyBorder (30, 40, 30, 40));

        titleLabel = new JLabel ("Select a component", SwingConstants.CENTER);
        titleLabel.setFont (new Font ("SansSerif", Font.PLAIN, 26));
        titleLabel.setForeground (COL_TREE);
        panel.add (titleLabel, BorderLayout.NORTH);

        fieldsPanel = new JPanel ();
        fieldsPanel.setBackground (BG_DARK);
        fieldsPanel.setLayout (new BoxLayout (fieldsPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane (fieldsPanel);
        scroll.setBorder (null);
        scroll.getViewport ().setBackground (BG_DARK);
        panel.add (scroll, BorderLayout.CENTER);

        //imagePanel = new ImagePanel ();
        //imagePanel.setBackground (BG_DARK);
        //imagePanel.setPreferredSize (new Dimension (100, 280));
        //panel.add (imagePanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildStatsPanel ()
    {
        JPanel panel = new JPanel ();
        panel.setBackground (BG_BOTTOM);
        panel.setLayout (new BoxLayout (panel, BoxLayout.Y_AXIS));
        panel.setBorder (new EmptyBorder (10, 14, 10, 14));

        JLabel h = new JLabel ("stats");
        h.setFont (new Font ("SansSerif", Font.PLAIN, 18));
        h.setForeground (COL_STATS);
        panel.add (h);

        statsLabel = new JLabel ();
        statsLabel.setFont (new Font ("SansSerif", Font.PLAIN, 16));
        statsLabel.setForeground (COL_STATS);
        panel.add (statsLabel);
        return panel;
    }

    private void rebuildNodes ()
    {
        rootNode.removeAllChildren ();
        for (Map.Entry<String, java.util.List<Map<String, String>>> e : data.entrySet ())
        {
            DefaultMutableTreeNode cat = new DefaultMutableTreeNode (e.getKey ());
            for (Map<String, String> part : e.getValue ())
                cat.add (new DefaultMutableTreeNode (label (part)));
            rootNode.add (cat);
        }
        if (treeModel != null && tree != null)
        {
            treeModel.reload ();
            for (int i = 0; i < tree.getRowCount (); i++) tree.expandRow (i);
        }
    }

    private DefaultMutableTreeNode findCatNode (String category)
    {
        for (int i = 0; i < rootNode.getChildCount (); i++)
        {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) rootNode.getChildAt (i);
            if (n.toString ().equals (category)) return n;
        }
        return null;
    }

    private void onSelect (TreeSelectionEvent e)
    {
        TreePath path = e.getPath ();
        if (path == null) return;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent ();

        if (node.isRoot ())
        {
            clearDetail ("Components");
            return;
        }

        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent ();
        if (parent == null || parent.isRoot ())
        {
            clearDetail (node.toString ());
            return;
        }

        String category = parent.toString ();
        int idx = parent.getIndex (node);
        java.util.List<Map<String, String>> parts = data.get (category);
        if (parts != null && idx < parts.size ()) showDetail (category, parts.get (idx));
    }

    private void onReload ()
    {
        loadFromExcel ();
        rebuildNodes ();
        clearDetail ("Select a component");
        updateStats ();
    }

    private void onAdd ()
    {
        if (data.isEmpty ())
        {
            JOptionPane.showMessageDialog (this,
                    "No categories found. Check if your Excel file has at least one sheet with data.");
            return;
        }
        String[] cats = data.keySet ().toArray (new String[0]);
        String cat = (String) JOptionPane.showInputDialog (this,
                "Select category:", "Add Component",
                JOptionPane.PLAIN_MESSAGE, null, cats, cats[0]);
        if (cat == null) return;

        JTextField brand = new JTextField (15), model = new JTextField (15);
        JPanel form = new JPanel (new GridLayout (2, 2, 6, 6));
        form.add (new JLabel ("Brand:"));
        form.add (brand);
        form.add (new JLabel ("Model:"));
        form.add (model);
        if (JOptionPane.showConfirmDialog (this, form, "New " + cat,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        String b = brand.getText ().trim (), m = model.getText ().trim ();
        if (b.isEmpty () && m.isEmpty ()) return;

        Map<String, String> part = new LinkedHashMap<> ();
        part.put ("Brand", b);
        part.put ("Model", m);
        data.get (cat).add (part);

        DefaultMutableTreeNode catNode = findCatNode (cat);
        if (catNode != null)
        {
            DefaultMutableTreeNode pn = new DefaultMutableTreeNode (label (part));
            treeModel.insertNodeInto (pn, catNode, catNode.getChildCount ());
            tree.scrollPathToVisible (new TreePath (pn.getPath ()));
        }
        updateStats ();
    }

    private void clearDetail (String title)
    {
        titleLabel.setText (title);
        fieldsPanel.removeAll ();
        fieldsPanel.revalidate ();
        fieldsPanel.repaint ();
        //imagePanel.setImage (null);
    }

    private void showDetail (String category, Map<String, String> part)
    {
        titleLabel.setText (label (part));
        fieldsPanel.removeAll ();
        fieldsPanel.add (Box.createVerticalStrut (20));

        for (Map.Entry<String, String> f : part.entrySet ())
        {
            if (f.getValue ().isEmpty ()) continue;
            JPanel row = new JPanel (new FlowLayout (FlowLayout.CENTER, 8, 6));
            row.setBackground (BG_DARK);
            row.setMaximumSize (new Dimension (Integer.MAX_VALUE, 50));

            JLabel k = new JLabel (f.getKey () + ":");
            k.setFont (new Font ("SansSerif", Font.PLAIN, 22));
            k.setForeground (COL_LABEL);

            JLabel v = new JLabel (f.getValue ());
            v.setFont (new Font ("SansSerif", Font.PLAIN, 22));
            v.setForeground (COL_VALUE);

            row.add (k);
            row.add (v);
            fieldsPanel.add (row);
        }
        fieldsPanel.add (Box.createVerticalGlue ());
        fieldsPanel.revalidate ();
        fieldsPanel.repaint ();

        //imagePanel.setImage (loadImageFor (category, part));
    }

    private String label (Map<String, String> part)
    {
        String s = (part.getOrDefault ("Brand", "") + " " + part.getOrDefault ("Model", "")).trim ();
        return s.isEmpty () ? "(unnamed)" : s;
    }

    private void updateStats ()
    {
        int total = data.values ().stream ().mapToInt (java.util.List::size).sum ();
        StringBuilder sb = new StringBuilder ("total components in storage: " + total);
        statsLabel.setText (sb.toString ());
    }

    private JButton styledBtn (String text, Color fg, int size)
    {
        JButton b = new JButton (text);
        b.setForeground (fg);
        b.setBackground (BG_PANEL);
        b.setBorderPainted (false);
        b.setFocusPainted (false);
        b.setFont (new Font ("SansSerif", Font.BOLD, size));
        b.setCursor (Cursor.getPredefinedCursor (Cursor.HAND_CURSOR));
        return b;
    }

    private final Map<String, Image> imageCache = new java.util.HashMap<> ();
    // private ImagePanel imagePanel;
    private static final String IMAGES_DIR = "images";
    private static final String FALLBACK_IMAGE = System.getProperty("user.home") + File.separator + "Documents" +
            File.separator + "images" + File.separator + "untitled.png";

    /*
region ImagePanel
    private static class ImagePanel extends JPanel
    {
        private Image image;

        void setImage (Image img)
        {
            this.image = img;
            repaint ();
        }

        @Override
        protected void paintComponent (Graphics g)
        {
            super.paintComponent (g);
            Graphics2D g2 = (Graphics2D) g.create ();
            g2.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth (),h = getHeight ();

            if (image != null)
            {
                int iw = image.getWidth (this), ih = image.getHeight (this);
                if (iw > 0 && ih > 0)
                {
                    double scale = Math.min ((double) (w - 20) / iw, (double) (h - 20) / ih);
                    scale = Math.min (scale, 1.0);
                    int dw = (int) (iw * scale), dh = (int) (ih * scale);
                    g2.drawImage (image, (w - dw) / 2, (h - dh) / 2, dw, dh, this);
                } else
                {
                    drawPlaceholder (g2, w, h);
                }
            }
            g2.dispose ();
        }

        private void drawPlaceholder (Graphics2D g2, int w, int h)
        {
            int boxW = Math.min (w - 40, 280);
            int boxH = (int) (boxW * 0.55);
            int x = (w - boxW) / 2, y = (h - boxH) / 2 - 10;

            g2.setColor (new Color (160, 160, 200));
            g2.setStroke (new BasicStroke (2.5f));
            g2.draw (new RoundRectangle2D.Float (x, y, boxW, boxH, 12, 12));

            int fanD = boxH - 30;
            drawFan (g2, x + 20 + fanD / 2, y + 15 + fanD / 2, fanD / 2);
            drawFan (g2, x + boxW - fanD / 2 - 20, y + 15 + fanD / 2, fanD / 2);

            g2.setFont (new Font ("SansSerif", Font.PLAIN, 22));
            FontMetrics fm = g2.getFontMetrics ();
            String msg = "No image available";
            g2.drawString (msg, x + (boxW - fm.stringWidth (msg)) / 2, y + boxH + 26);
        }

        private void drawFan (Graphics2D g2, int cx, int cy, int r)
        {
            g2.drawOval (cx - r, cy - r, r * 2, r * 2);
            for (int i = 0; i < 6; i++)
            {
                double a = Math.toRadians (i * 60);
                g2.drawLine (cx, cy, cx + (int) (r * Math.cos (a)), cy + (int) (r * Math.sin (a)));
            }
        }
    }

    private Image loadImageFor (String category, Map<String, String> part)
    {
        java.util.List<String> candidates = new ArrayList<> ();

        String explicit = firstNonEmpty (part.get ("Image"), part.get ("ImagePath"), part.get ("Photo"));
        if (explicit != null)
        {
            candidates.add (explicit);
            candidates.add (IMAGES_DIR + File.separator + category + File.separator + explicit);
        }

        String base = sanitize (part.getOrDefault ("Brand", "") + "_" + part.getOrDefault ("Model", ""));
        if (! base.isEmpty () && ! base.equals ("_"))
        {
            for (String ext : new String[] {"png", "jpg", "jpeg", "gif", "bmp", "tif", "tiff"})
            {
                candidates.add (IMAGES_DIR + File.separator + category + File.separator + base + "." + ext);
                candidates.add (IMAGES_DIR + File.separator + base + "." + ext);
            }
        }

        for (String path : candidates)
        {
            Image img = tryLoad (path);
            if (img != null)
                return img;
        }
        return tryLoad (FALLBACK_IMAGE);
    }

    private Image tryLoad (String path)
    {
        if (path == null || path.isBlank ())
            return null;
        if (imageCache.containsKey (path))
        return imageCache.get (path);
        Image img = null;
        File f = new File (path);
        if (f.exists () && f.isFile ())
        {
            try
            {
                img = ImageIO.read (f);
            } catch (Exception ignored) {}
        }
        imageCache.put (path, img);
        return img;
    }

    private String sanitize (String s)
    {
        return s.trim ().toLowerCase ().replaceAll ("[^a-z0-9]+", "_").replaceAll ("^_+|_+$", "");
    }

    private String firstNonEmpty (String... vals)
    {
        for (String v : vals)
            if (v != null && ! v.isBlank ())
                return v;
        return null;
    }
endregion
*/
}