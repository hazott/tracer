package org.graalvm.vm.trcview.ui.data.editor;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.graalvm.vm.trcview.ui.event.ChangeListener;

@SuppressWarnings("serial")
public class JEditor extends JComponent implements Scrollable, ChangeListener {
    public static final Font TEXT_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    public static final Font KEYWORD_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    public static final Color CURSOR_COLOR = Color.GRAY;

    private static final int MAX_CACHE_SIZE = 10_000;

    private int maxUnitIncrement = 1;

    private int currentLine;
    private int currentColumn;

    private EditorModel model;

    private FontMetrics fontMetrics;
    private int charWidth;
    private int charHeight;

    private int offsetX = 10;
    private int offsetY = 10;

    private Rectangle clip = null;

    private Map<Integer, Line> lineCache = new HashMap<>();

    private Color highlightColor = null;

    public JEditor() {
        this(new DefaultEditorModel());
    }

    public JEditor(EditorModel model) {
        fontMetrics = getFontMetrics(TEXT_FONT);
        charHeight = fontMetrics.getHeight();
        Rectangle2D charsize = TEXT_FONT.getMaxCharBounds(fontMetrics.getFontRenderContext());
        charWidth = charsize.getBounds().width;

        setBackground(Color.WHITE);
        setModel(model);

        setFocusable(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                setCursorToPoint(e.getX(), e.getY());
                requestFocusInWindow();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                setCursorToPoint(e.getX(), e.getY());
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        setCursor(currentLine, currentColumn - 1);
                        scrollToCursor();
                        break;
                    case KeyEvent.VK_RIGHT:
                        setCursor(currentLine, currentColumn + 1);
                        scrollToCursor();
                        break;
                    case KeyEvent.VK_UP:
                        setCursor(currentLine - 1, currentColumn);
                        scrollToCursor();
                        break;
                    case KeyEvent.VK_DOWN:
                        setCursor(currentLine + 1, currentColumn);
                        scrollToCursor();
                        break;
                }
            }
        });

        // swallow arrow keys
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() == JEditor.this) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_LEFT:
                        case KeyEvent.VK_RIGHT:
                        case KeyEvent.VK_UP:
                        case KeyEvent.VK_DOWN:
                            for (KeyListener l : getKeyListeners()) {
                                try {
                                    switch (e.getID()) {
                                        case KeyEvent.KEY_PRESSED:
                                            l.keyPressed(e);
                                            break;
                                        case KeyEvent.KEY_RELEASED:
                                            l.keyReleased(e);
                                            break;
                                        case KeyEvent.KEY_TYPED:
                                            l.keyTyped(e);
                                            break;
                                    }
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                }
                            }
                            e.consume();
                            return true;
                        default:
                            return false;
                    }
                } else {
                    return false;
                }
            }
        });
    }

    public void setModel(EditorModel model) {
        if (this.model != null) {
            this.model.removeChangeListener(this);
        }
        this.model = model;
        Dimension size = new Dimension(charWidth * model.getMaximumLineLength() + 2 * offsetX, charHeight * model.getLineCount() + 2 * offsetY);
        setMinimumSize(size);
        setPreferredSize(size);
        currentLine = 0;
        currentColumn = 0;
        model.addChangeListener(this);
    }

    @Override
    public void valueChanged() {
        lineCache.clear();
        repaint();
    }

    public void setCursorToPoint(int x, int y) {
        if (clip == null) {
            return;
        }

        int posX = (x - offsetX + (charWidth / 2)) / charWidth;
        int posY = (y - offsetY) / charHeight;

        setCursor(posY, posX);
    }

    public void setCursorLine(int line) {
        if (line < 0) {
            currentLine = 0;
        } else if (line >= model.getLineCount()) {
            currentLine = model.getLineCount() - 1;
            if (currentLine < 0) {
                currentLine = 0;
            }
        } else {
            currentLine = line;
        }

        setCursorColumn(currentColumn);
    }

    public void setCursorColumn(int column) {
        Line l = getLine(currentLine);
        if (l == null) {
            currentColumn = 0;
        } else if (column < 0) {
            currentColumn = 0;
        } else if (column > l.getLength()) {
            currentColumn = l.getLength();
            if (currentColumn < 0) {
                currentColumn = 0;
            }
        } else {
            currentColumn = column;
        }

        repaint();
    }

    public void setCursor(int line, int column) {
        setCursorLine(line);
        setCursorColumn(column);
    }

    public void scrollToCursor() {
        int x = getX(currentColumn);
        int y = getY(currentLine);
        scrollRectToVisible(new Rectangle(x, y, charWidth, charHeight));
    }

    private Line getLine(int y) {
        Line line = lineCache.get(y);
        if (line == null) {
            line = model.getLine(y);
            if (line != null) {
                if (lineCache.size() > MAX_CACHE_SIZE) {
                    lineCache.clear();
                }
                lineCache.put(y, line);
            }
        }
        return line;
    }

    public int getCursorLine() {
        return currentLine;
    }

    public int getCursorColumn() {
        return currentColumn;
    }

    private int getX(int column) {
        return column * charWidth + offsetX;
    }

    private int getY(int line) {
        return line * charHeight + fontMetrics.getAscent() + offsetY;
    }

    public void setHighlightColor(Color color) {
        highlightColor = color;
        repaint();
    }

    public Color getHighlightColor() {
        return highlightColor;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.WHITE);
        clip = g.getClipBounds();
        g.fillRect(clip.x, clip.y, clip.width, clip.height);

        int posY = (clip.y - offsetY) / charHeight;

        int startY = posY - 1;
        if (startY < 0) {
            startY = 0;
        }

        int endX = clip.x + clip.width;
        int endY = startY + clip.height / charHeight + 2;

        if (endY > model.getLineCount()) {
            endY = model.getLineCount();
        }

        for (int y = startY; y < endY; y++) {
            Line line = getLine(y);

            if (y == currentLine && highlightColor != null) {
                g.setColor(highlightColor);
                g.fillRect(0, offsetY + y * charHeight, getWidth(), charHeight);
            }

            int x = offsetX;
            if (line != null) {
                List<Element> elements = line.getElements();
                for (Element element : elements) {
                    x = element.draw(g, x, offsetY + y * charHeight + fontMetrics.getAscent());
                    if (x > endX) {
                        break;
                    }
                }
            }
        }

        // draw cursor crosshair
        int cursorY = getY(currentLine) + 1;
        int cursorX = getX(currentColumn) - 1;
        g.setColor(CURSOR_COLOR);
        g.drawLine(0, cursorY, getWidth(), cursorY);
        g.drawLine(cursorX, 0, cursorX, getHeight());
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(300, 300);
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        // stretch to viewport size if viewport is bigger than this component
        Container parent = SwingUtilities.getUnwrappedParent(this);
        if (parent instanceof JViewport) {
            return parent.getHeight() > getPreferredSize().height;
        }
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        // stretch to viewport size if viewport is bigger than this component
        Container parent = SwingUtilities.getUnwrappedParent(this);
        if (parent instanceof JViewport) {
            return parent.getWidth() > getPreferredSize().width;
        }
        return false;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        // get the current position
        int currentPosition = 0;
        if (orientation == SwingConstants.HORIZONTAL) {
            currentPosition = visibleRect.x;
        } else {
            currentPosition = visibleRect.y;
        }

        // return the number of pixels between currentPosition and the nearest tick mark in the
        // indicated direction
        if (direction < 0) {
            int newPosition = currentPosition - (currentPosition / maxUnitIncrement) * maxUnitIncrement;
            return newPosition == 0 ? maxUnitIncrement : newPosition;
        } else {
            return ((currentPosition / maxUnitIncrement) + 1) * maxUnitIncrement - currentPosition;
        }
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.HORIZONTAL) {
            return visibleRect.width - maxUnitIncrement;
        } else {
            return visibleRect.height - maxUnitIncrement;
        }
    }
}
